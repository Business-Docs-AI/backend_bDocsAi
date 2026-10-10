package br.com.example.senac.businessDocsAi.document.tool;

import br.com.example.senac.businessDocsAi.ai.retrieval.PesquisaService;
import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.categories.service.CategoryService;
import br.com.example.senac.businessDocsAi.chat.service.ConversaContextHolder;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;
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
}
