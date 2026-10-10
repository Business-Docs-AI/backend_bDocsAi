package br.com.example.senac.businessDocsAi.document.tool;

import br.com.example.senac.businessDocsAi.ai.retrieval.PesquisaService;
import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.categories.service.CategoryService;
import br.com.example.senac.businessDocsAi.chat.service.ConversaContextHolder;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;
import br.com.example.senac.businessDocsAi.document.dto.ResultadoBuscaDTO;
import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import br.com.example.senac.businessDocsAi.document.entity.StatusRascunho;
import br.com.example.senac.businessDocsAi.document.entity.TipoRascunho;
import br.com.example.senac.businessDocsAi.document.repository.IRascunhoDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.service.DocumentoEstruturadoAplicadorService;
import br.com.example.senac.businessDocsAi.document.service.DocumentoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentoToolsTest {

    @Mock
    private DocumentoService documentoService;

    @Mock
    private PesquisaService pesquisaService;

    @Mock
    private IRascunhoDocumentoRepository rascunhoRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CategoriaAccessService categoriaAccessService;

    @Mock
    private DocumentoEstruturadoAplicadorService documentoEstruturadoAplicadorService;

    private DocumentoTools documentoTools;

    private final UUID conversaId = UUID.randomUUID();
    private final UUID turnoAtual = UUID.randomUUID();
    private static final Long CATEGORIA_ID = 1L;

    @BeforeEach
    void setUp() {
        documentoTools = new DocumentoTools(
                documentoService, pesquisaService, rascunhoRepository, categoryService, categoriaAccessService,
                documentoEstruturadoAplicadorService
        );
        ConversaContextHolder.iniciar(conversaId, turnoAtual);
    }

    @AfterEach
    void tearDown() {
        ConversaContextHolder.limpar();
    }

    @Test
    void prepararCriacaoDocumentoApenasGravaRascunhoSemChamarDocumentoService() {
        when(rascunhoRepository.save(any(RascunhoDocumentoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String resposta = documentoTools.prepararCriacaoDocumento(
                "Política de Home Office", "<p>2 dias por semana</p>", CATEGORIA_ID
        );

        ArgumentCaptor<RascunhoDocumentoEntity> captor = ArgumentCaptor.forClass(RascunhoDocumentoEntity.class);
        verify(rascunhoRepository).save(captor.capture());

        RascunhoDocumentoEntity salvo = captor.getValue();
        assertThat(salvo.getConversaId()).isEqualTo(conversaId);
        assertThat(salvo.getTipo()).isEqualTo(TipoRascunho.CRIAR);
        assertThat(salvo.getStatus()).isEqualTo(StatusRascunho.PENDENTE);
        assertThat(salvo.getTurnoCriacao()).isEqualTo(turnoAtual);
        assertThat(salvo.getTitulo()).isEqualTo("Política de Home Office");

        assertThat(resposta).contains("Política de Home Office");
        verifyNoInteractions(documentoService);
    }

    @Test
    void prepararAtualizacaoDocumentoComIdInvalidoNaoGravaNada() {
        String resposta = documentoTools.prepararAtualizacaoDocumento("nao-e-um-uuid", "Título", "<p>x</p>");

        assertThat(resposta).contains("inválido");
        verifyNoInteractions(rascunhoRepository, documentoService);
    }

    @Test
    void confirmarRascunhoPendenteSemNenhumRascunhoDevolveMensagemSemChamarDocumentoService() {
        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.empty());

        String resposta = documentoTools.confirmarRascunhoPendente();

        assertThat(resposta).contains("Não há nenhuma proposta pendente");
        verifyNoInteractions(documentoService);
    }

    @Test
    void confirmarRascunhoPendenteNoMesmoTurnoDaPropostaEhRecusado() {
        RascunhoDocumentoEntity rascunho = rascunhoCriar(turnoAtual);

        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(rascunho));

        String resposta = documentoTools.confirmarRascunhoPendente();

        assertThat(resposta).containsIgnoringCase("ainda não é possível confirmar");
        assertThat(rascunho.getStatus()).isEqualTo(StatusRascunho.PENDENTE);
        verifyNoInteractions(documentoService);
        verify(rascunhoRepository, never()).save(any());
    }

    @Test
    void confirmarRascunhoPendenteDeTurnoAnteriorCriaODocumentoEMarcaConfirmado() {
        UUID turnoAnterior = UUID.randomUUID();
        RascunhoDocumentoEntity rascunho = rascunhoCriar(turnoAnterior);

        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(rascunho));

        DocumentoResponseDTO documentoCriado = new DocumentoResponseDTO(
                UUID.randomUUID(), rascunho.getTitulo(), rascunho.getConteudoHtml(), 1,
                StatusIndexacao.PENDENTE, "Autor", LocalDateTime.now(), null, null, null, null
        );
        when(documentoService.criar(any())).thenReturn(documentoCriado);

        String resposta = documentoTools.confirmarRascunhoPendente();

        verify(documentoService).criar(any());
        verify(documentoService, never()).atualizar(any(), any());
        assertThat(rascunho.getStatus()).isEqualTo(StatusRascunho.CONFIRMADO);
        assertThat(rascunho.getConfirmadoEm()).isNotNull();
        assertThat(rascunho.getDocumentoResultanteId()).isEqualTo(documentoCriado.id());
        assertThat(resposta).contains(documentoCriado.id().toString());
    }

    @Test
    void confirmarRascunhoDeAtualizacaoChamaAtualizarComODocumentoIdAlvo() {
        UUID turnoAnterior = UUID.randomUUID();
        UUID documentoIdAlvo = UUID.randomUUID();

        RascunhoDocumentoEntity rascunho = new RascunhoDocumentoEntity();
        rascunho.setConversaId(conversaId);
        rascunho.setTipo(TipoRascunho.ATUALIZAR);
        rascunho.setDocumentoIdAlvo(documentoIdAlvo);
        rascunho.setTitulo("Título Atualizado");
        rascunho.setConteudoHtml("<p>Novo conteúdo</p>");
        rascunho.setStatus(StatusRascunho.PENDENTE);
        rascunho.setTurnoCriacao(turnoAnterior);
        rascunho.setCriadoEm(LocalDateTime.now());

        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(rascunho));

        DocumentoResponseDTO documentoAtualizado = new DocumentoResponseDTO(
                documentoIdAlvo, rascunho.getTitulo(), rascunho.getConteudoHtml(), 2,
                StatusIndexacao.PENDENTE, "Autor", LocalDateTime.now(), "Autor", LocalDateTime.now(), null, null
        );
        when(documentoService.atualizar(eq(documentoIdAlvo), any())).thenReturn(documentoAtualizado);

        documentoTools.confirmarRascunhoPendente();

        verify(documentoService).atualizar(eq(documentoIdAlvo), any());
        verify(documentoService, never()).criar(any());
    }

    @Test
    void descartarRascunhoPendenteMarcaDescartadoSemChamarDocumentoService() {
        RascunhoDocumentoEntity rascunho = rascunhoCriar(UUID.randomUUID());

        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(rascunho));
        when(rascunhoRepository.descartar(rascunho.getId(), StatusRascunho.ativos())).thenReturn(1);

        String resposta = documentoTools.descartarRascunhoPendente();

        // UPDATE condicional (não save() da entidade inteira) — ver bug corrigido em
        // IRascunhoDocumentoRepositoryGeracaoAssincronaTest,
        // saveDeEntidadeLidaAntesDoFinalizarComSucessoApagaOConteudoRecemGravado.
        verify(rascunhoRepository).descartar(rascunho.getId(), StatusRascunho.ativos());
        verify(rascunhoRepository, never()).save(any());
        assertThat(resposta).containsIgnoringCase("descartada");
        verifyNoInteractions(documentoService);
    }

    @Test
    void descartarRascunhoPendenteQuandoWorkerJaTerminouAvisaQueNaoEstaMaisAtiva() {
        RascunhoDocumentoEntity rascunho = rascunhoCriar(UUID.randomUUID());

        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(rascunho));
        // affected=0: o worker terminou (ou outro descarte já rodou) entre o findFirst e o
        // descartar() — a linha não está mais em nenhum status ativo.
        when(rascunhoRepository.descartar(rascunho.getId(), StatusRascunho.ativos())).thenReturn(0);

        String resposta = documentoTools.descartarRascunhoPendente();

        assertThat(resposta).containsIgnoringCase("não está mais ativa");
    }

    private RascunhoDocumentoEntity rascunhoCriar(UUID turno) {
        RascunhoDocumentoEntity rascunho = new RascunhoDocumentoEntity();
        rascunho.setConversaId(conversaId);
        rascunho.setTipo(TipoRascunho.CRIAR);
        rascunho.setTitulo("Política de Home Office");
        rascunho.setConteudoHtml("<p>2 dias por semana</p>");
        rascunho.setStatus(StatusRascunho.PENDENTE);
        rascunho.setTurnoCriacao(turno);
        rascunho.setCriadoEm(LocalDateTime.now());
        return rascunho;
    }

    // --- Etapa 13.3 (R1): tools legadas agora respeitam GERANDO/ERRO_GERACAO ---

    @Test
    void prepararCriacaoDocumentoComOutroRascunhoGerandoNaConversaRecusa() {
        RascunhoDocumentoEntity gerando = new RascunhoDocumentoEntity();
        gerando.setStatus(StatusRascunho.GERANDO);
        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(gerando));

        String resposta = documentoTools.prepararCriacaoDocumento("Outro", "<p>x</p>", CATEGORIA_ID);

        assertThat(resposta).containsIgnoringCase("já existe um documento sendo gerado");
        verify(rascunhoRepository, never()).save(any());
    }

    // F2 (2026-10-10): confirma que prepararAtualizacaoDocumento também recusa com um
    // rascunho GERANDO na conversa (só prepararCriacaoDocumento tinha teste disso até aqui).
    @Test
    void prepararAtualizacaoDocumentoComOutroRascunhoGerandoNaConversaRecusa() {
        UUID documentoIdAlvo = UUID.randomUUID();
        DocumentoResponseDTO documentoAtual = new DocumentoResponseDTO(
                documentoIdAlvo, "Título Atual", "<p>x</p>", 1, StatusIndexacao.PENDENTE, "Autor",
                LocalDateTime.now(), null, null, CATEGORIA_ID, null
        );
        when(documentoService.buscarPorId(documentoIdAlvo)).thenReturn(documentoAtual);

        RascunhoDocumentoEntity gerando = new RascunhoDocumentoEntity();
        gerando.setStatus(StatusRascunho.GERANDO);
        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(gerando));

        String resposta = documentoTools.prepararAtualizacaoDocumento(documentoIdAlvo.toString(), "Outro", "<p>x</p>");

        assertThat(resposta).containsIgnoringCase("já existe um documento sendo gerado");
        verify(rascunhoRepository, never()).save(any());
    }

    // F2 (2026-10-10): as tools LEGADAS (preparar*) sempre recebem o HTML completo como
    // parâmetro (nunca leem conteudoHtml do rascunho antigo) — encontrar um rascunho em
    // ERRO_GERACAO é seguro: sobrescreve a linha com o conteúdo novo, nunca mescla nem lê o
    // conteudoHtml antigo (que podia estar nulo, já que o worker assíncrono nunca chegou a
    // escrever nada antes de falhar).
    @Test
    void prepararCriacaoDocumentoComRascunhoEmErroSobrescreveSemLerConteudoAntigo() {
        RascunhoDocumentoEntity emErro = new RascunhoDocumentoEntity();
        emErro.setId(UUID.randomUUID());
        emErro.setStatus(StatusRascunho.ERRO_GERACAO);
        emErro.setErroGeracao("Falha anterior do worker assíncrono");
        emErro.setConteudoHtml(null); // worker nunca chegou a escrever nada
        emErro.setTentativasGeracao(2);
        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(emErro));
        when(rascunhoRepository.save(any(RascunhoDocumentoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        documentoTools.prepararCriacaoDocumento("Novo Título", "<p>conteúdo novo completo</p>", CATEGORIA_ID);

        ArgumentCaptor<RascunhoDocumentoEntity> captor = ArgumentCaptor.forClass(RascunhoDocumentoEntity.class);
        verify(rascunhoRepository).save(captor.capture());

        RascunhoDocumentoEntity salvo = captor.getValue();
        assertThat(salvo.getStatus()).isEqualTo(StatusRascunho.PENDENTE);
        assertThat(salvo.getConteudoHtml()).isEqualTo("<p>conteúdo novo completo</p>");
        assertThat(salvo.getErroGeracao()).isNull();
        assertThat(salvo.getTentativasGeracao()).isZero();
    }

    @Test
    void prepararAtualizacaoDocumentoComRascunhoEmErroSobrescreveSemLerConteudoAntigo() {
        UUID documentoIdAlvo = UUID.randomUUID();
        DocumentoResponseDTO documentoAtual = new DocumentoResponseDTO(
                documentoIdAlvo, "Título Atual", "<p>conteúdo antigo</p>", 1, StatusIndexacao.PENDENTE, "Autor",
                LocalDateTime.now(), null, null, CATEGORIA_ID, null
        );
        when(documentoService.buscarPorId(documentoIdAlvo)).thenReturn(documentoAtual);

        RascunhoDocumentoEntity emErro = new RascunhoDocumentoEntity();
        emErro.setId(UUID.randomUUID());
        emErro.setStatus(StatusRascunho.ERRO_GERACAO);
        emErro.setErroGeracao("Falha anterior do worker assíncrono");
        emErro.setConteudoHtml(null);
        emErro.setTentativasGeracao(1);
        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(emErro));
        when(rascunhoRepository.save(any(RascunhoDocumentoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        documentoTools.prepararAtualizacaoDocumento(documentoIdAlvo.toString(), "Novo Título", "<p>conteúdo novo completo</p>");

        ArgumentCaptor<RascunhoDocumentoEntity> captor = ArgumentCaptor.forClass(RascunhoDocumentoEntity.class);
        verify(rascunhoRepository).save(captor.capture());

        RascunhoDocumentoEntity salvo = captor.getValue();
        assertThat(salvo.getStatus()).isEqualTo(StatusRascunho.PENDENTE);
        assertThat(salvo.getConteudoHtml()).isEqualTo("<p>conteúdo novo completo</p>");
        assertThat(salvo.getErroGeracao()).isNull();
        assertThat(salvo.getTentativasGeracao()).isZero();
    }

    @Test
    void confirmarRascunhoEmGerandoERecusadoComMensagemClara() {
        RascunhoDocumentoEntity gerando = new RascunhoDocumentoEntity();
        gerando.setStatus(StatusRascunho.GERANDO);
        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(gerando));

        String resposta = documentoTools.confirmarRascunhoPendente();

        assertThat(resposta).containsIgnoringCase("ainda está sendo gerado");
        verifyNoInteractions(documentoService);
    }

    @Test
    void confirmarRascunhoEmErroGeracaoERecusadoComAMensagemDeErro() {
        RascunhoDocumentoEntity emErro = new RascunhoDocumentoEntity();
        emErro.setStatus(StatusRascunho.ERRO_GERACAO);
        emErro.setErroGeracao("Esgotou as tentativas de validação");
        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(emErro));

        String resposta = documentoTools.confirmarRascunhoPendente();

        assertThat(resposta).contains("Esgotou as tentativas de validação");
        verifyNoInteractions(documentoService);
    }

    @Test
    void descartarRascunhoEmGerandoFunciona() {
        UUID id = UUID.randomUUID();
        RascunhoDocumentoEntity gerando = new RascunhoDocumentoEntity();
        gerando.setId(id);
        gerando.setStatus(StatusRascunho.GERANDO);
        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(gerando));
        when(rascunhoRepository.descartar(id, StatusRascunho.ativos())).thenReturn(1);

        String resposta = documentoTools.descartarRascunhoPendente();

        verify(rascunhoRepository).descartar(id, StatusRascunho.ativos());
        assertThat(resposta).containsIgnoringCase("descartada");
    }

    // Decisão B3 pelo caminho do chat: um rascunho com conteudoEstruturado preenchido
    // confirma pelo DocumentoEstruturadoAplicadorService, nunca por documentoService.criar
    // direto (esse é o aplicador real, testado em DocumentoEstruturadoAplicadorServiceTest).
    @Test
    void confirmarRascunhoComConteudoEstruturadoDelegaParaOAplicador() {
        UUID turnoAnterior = UUID.randomUUID();
        RascunhoDocumentoEntity rascunho = rascunhoCriar(turnoAnterior);
        rascunho.setConteudoEstruturado("{\"objetivo\":\"x\"}");
        rascunho.setVersaoSchema("1.0");

        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(rascunho));

        DocumentoResponseDTO documentoCriado = new DocumentoResponseDTO(
                UUID.randomUUID(), rascunho.getTitulo(), "<h2>x</h2>", 1, StatusIndexacao.PENDENTE, "Autor",
                LocalDateTime.now(), null, null, null, null
        );
        when(documentoEstruturadoAplicadorService.aplicar(rascunho)).thenReturn(documentoCriado);

        String resposta = documentoTools.confirmarRascunhoPendente();

        verify(documentoEstruturadoAplicadorService).aplicar(rascunho);
        verifyNoInteractions(documentoService);
        assertThat(resposta).contains(documentoCriado.id().toString());
    }

    // Decisão ADMIN/OBSOLETO (2026-10-10): buscarDocumentosIncluindoHistorico é o caminho
    // explícito de volta pro histórico completo — delega para pesquisaService.buscar com
    // incluirHistorico=true, nunca para o overload de 1 argumento (que esconde não-vigentes
    // com a flag ligada).
    @Test
    void buscarDocumentosIncluindoHistoricoDelegaParaPesquisaServiceComIncluirHistorico() {
        UUID documentoId = UUID.randomUUID();
        ResultadoBuscaDTO resultado = new ResultadoBuscaDTO(documentoId, "Documento Obsoleto", 0.87, List.of());
        when(pesquisaService.buscar("política antiga", true)).thenReturn(List.of(resultado));

        String resposta = documentoTools.buscarDocumentosIncluindoHistorico("política antiga");

        verify(pesquisaService).buscar("política antiga", true);
        verify(pesquisaService, never()).buscar("política antiga");
        assertThat(resposta).contains(documentoId.toString());
        assertThat(resposta).contains("Documento Obsoleto");
        assertThat(resposta).contains(String.format("%.2f", 0.87));
    }

    @Test
    void buscarDocumentosIncluindoHistoricoSemResultadosAvisaOQueFoiBuscado() {
        when(pesquisaService.buscar("nada disso existe", true)).thenReturn(List.of());

        String resposta = documentoTools.buscarDocumentosIncluindoHistorico("nada disso existe");

        assertThat(resposta).containsIgnoringCase("nenhum documento encontrado");
    }
}
