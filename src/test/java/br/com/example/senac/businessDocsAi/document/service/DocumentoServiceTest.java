package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoRequestDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoVersaoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusCicloVida;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import br.com.example.senac.businessDocsAi.document.entity.TipoDocumento;
import br.com.example.senac.businessDocsAi.document.event.DocumentoAlteradoEvent;
import br.com.example.senac.businessDocsAi.document.event.DocumentoExcluidoEvent;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoAreaParticipanteRepository;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoVersaoRepository;
import br.com.example.senac.businessDocsAi.exception.BadRequestException;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import br.com.example.senac.businessDocsAi.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock
    private IDocumentoRepository documentoRepository;

    @Mock
    private IDocumentoVersaoRepository documentoVersaoRepository;

    @Mock
    private ICategoryRepository categoryRepository;

    @Mock
    private HtmlSanitizerService htmlSanitizerService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private CategoriaAccessService categoriaAccessService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private IDocumentoAreaParticipanteRepository documentoAreaParticipanteRepository;

    private DocumentoService documentoService;

    @BeforeEach
    void setUp() {
        documentoService = new DocumentoService(
                documentoRepository, documentoVersaoRepository, categoryRepository, htmlSanitizerService,
                currentUserProvider, categoriaAccessService, eventPublisher, documentoAreaParticipanteRepository
        );

        lenient().when(htmlSanitizerService.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(currentUserProvider.getCurrentUserName()).thenReturn("Autor Teste");
        lenient().when(documentoRepository.save(any(DocumentoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(documentoVersaoRepository.save(any(DocumentoVersaoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void criarDeveDefinirTipoDocumentoNaoClassificadoEStatusCicloVidaVigentePorDefault() {
        DocumentoRequestDTO dto = new DocumentoRequestDTO("Título Novo", "<p>Conteúdo</p>", null, 1L);

        ArgumentCaptor<DocumentoEntity> captor = ArgumentCaptor.forClass(DocumentoEntity.class);

        documentoService.criar(dto);

        verify(documentoRepository, atLeastOnce()).save(captor.capture());
        DocumentoEntity salvo = captor.getAllValues().get(0);

        assertThat(salvo.getTipoDocumento()).isEqualTo(TipoDocumento.NAO_CLASSIFICADO);
        assertThat(salvo.getStatusCicloVida()).isEqualTo(StatusCicloVida.VIGENTE);
    }

    @Test
    void deveGerarNovaVersaoEReindexarQuandoConteudoMuda() {
        UUID id = UUID.randomUUID();
        DocumentoEntity existente = documentoExistente(id, 1, "hash-antigo-arbitrario");

        when(documentoRepository.findById(id)).thenReturn(Optional.of(existente));

        DocumentoRequestDTO dto = new DocumentoRequestDTO("Novo Título", "<p>Novo conteúdo</p>", "ajuste", 1L);

        DocumentoResponseDTO response = documentoService.atualizar(id, dto);

        assertThat(response.versaoAtual()).isEqualTo(2);
        assertThat(existente.getStatusIndexacao()).isEqualTo(StatusIndexacao.PENDENTE);

        verify(documentoVersaoRepository).save(argThat(v -> v.getNumeroVersao() == 2));

        ArgumentCaptor<DocumentoAlteradoEvent> captor = ArgumentCaptor.forClass(DocumentoAlteradoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().versao()).isEqualTo(2);
        assertThat(captor.getValue().documentoId()).isEqualTo(id);
    }

    @Test
    void naoDeveGerarNovaVersaoNemReindexarQuandoHashNaoMuda() {
        UUID id = UUID.randomUUID();
        String titulo = "Título Estável";
        String html = "<p>Conteúdo estável</p>";
        String hashAtual = sha256(titulo, html);

        DocumentoEntity existente = documentoExistente(id, 1, hashAtual);

        when(documentoRepository.findById(id)).thenReturn(Optional.of(existente));

        DocumentoRequestDTO dto = new DocumentoRequestDTO(titulo, html, null, 1L);

        DocumentoResponseDTO response = documentoService.atualizar(id, dto);

        assertThat(response.versaoAtual()).isEqualTo(1);
        verify(documentoVersaoRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void restaurarVersaoDeveGerarNovaVersaoEReindexar() {
        UUID id = UUID.randomUUID();
        DocumentoEntity existente = documentoExistente(id, 2, "hash-atual-diferente");

        DocumentoVersaoEntity versaoAntiga = new DocumentoVersaoEntity();
        versaoAntiga.setDocumentoId(id);
        versaoAntiga.setNumeroVersao(1);
        versaoAntiga.setTitulo("Título Versão 1");
        versaoAntiga.setConteudoHtml("<p>Conteúdo versão 1</p>");
        versaoAntiga.setAutor("Autor Original");
        versaoAntiga.setCriadoEm(LocalDateTime.now());

        when(documentoRepository.findById(id)).thenReturn(Optional.of(existente));
        when(documentoVersaoRepository.findByDocumentoIdAndNumeroVersao(id, 1))
                .thenReturn(Optional.of(versaoAntiga));

        DocumentoResponseDTO response = documentoService.restaurarVersao(id, 1);

        assertThat(response.versaoAtual()).isEqualTo(3);
        assertThat(response.titulo()).isEqualTo("Título Versão 1");

        verify(documentoVersaoRepository).save(argThat(v -> v.getNumeroVersao() == 3));
        verify(eventPublisher).publishEvent(any(DocumentoAlteradoEvent.class));
    }

    @Test
    void excluirDeveMarcarSoftDeleteEPublicarEventoDeRemocao() {
        UUID id = UUID.randomUUID();
        DocumentoEntity existente = documentoExistente(id, 1, "hash-qualquer");

        when(documentoRepository.findById(id)).thenReturn(Optional.of(existente));

        documentoService.excluir(id);

        assertThat(existente.isDeletado()).isTrue();
        assertThat(existente.getExcluidoEm()).isNotNull();

        verify(eventPublisher).publishEvent(new DocumentoExcluidoEvent(id));
    }

    @Test
    void buscarPorIdDeveLancarNotFoundQuandoDocumentoEstaExcluido() {
        UUID id = UUID.randomUUID();
        DocumentoEntity existente = documentoExistente(id, 1, "hash-qualquer");
        existente.setDeletado(true);

        when(documentoRepository.findById(id)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> documentoService.buscarPorId(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void validarHierarquiaProcessoAceitaNuloSemConsultarOBanco() {
        documentoService.validarHierarquiaProcesso(UUID.randomUUID(), null);

        verifyNoInteractions(documentoRepository);
    }

    @Test
    void validarHierarquiaProcessoRejeitaAutoReferencia() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> documentoService.validarHierarquiaProcesso(id, id))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validarHierarquiaProcessoRejeitaCicloDireto() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        DocumentoEntity docB = documentoExistente(b, 1, "hash-b");
        docB.setProcessoPaiId(a);

        when(documentoRepository.findById(b)).thenReturn(Optional.of(docB));

        // Tentando setar A como filho de B, quando B já é filho de A — formaria um ciclo.
        assertThatThrownBy(() -> documentoService.validarHierarquiaProcesso(a, b))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validarHierarquiaProcessoRejeitaCicloIndireto() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        DocumentoEntity docC = documentoExistente(c, 1, "hash-c");
        docC.setProcessoPaiId(b);
        DocumentoEntity docB = documentoExistente(b, 1, "hash-b");
        docB.setProcessoPaiId(a);

        when(documentoRepository.findById(c)).thenReturn(Optional.of(docC));
        when(documentoRepository.findById(b)).thenReturn(Optional.of(docB));

        // A → B → C já existe; setar C como pai de A formaria um ciclo A→B→C→A.
        assertThatThrownBy(() -> documentoService.validarHierarquiaProcesso(a, c))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validarHierarquiaProcessoAceitaHierarquiaValidaSemCiclo() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        DocumentoEntity docB = documentoExistente(b, 1, "hash-b");
        // docB não tem pai — cadeia termina aí, sem ciclo.

        when(documentoRepository.findById(b)).thenReturn(Optional.of(docB));

        documentoService.validarHierarquiaProcesso(a, b);
        // Não lança exceção.
    }

    @Test
    void buscarPorIdNaoExpoeProcessoPaiQuandoEleEstaSoftDeletado() {
        UUID id = UUID.randomUUID();
        UUID paiId = UUID.randomUUID();

        DocumentoEntity existente = documentoExistente(id, 1, "hash-filho");
        existente.setProcessoPaiId(paiId);

        DocumentoEntity pai = documentoExistente(paiId, 1, "hash-pai");
        pai.setDeletado(true);

        when(documentoRepository.findById(id)).thenReturn(Optional.of(existente));
        when(documentoRepository.findById(paiId)).thenReturn(Optional.of(pai));

        DocumentoResponseDTO response = documentoService.buscarPorId(id);

        assertThat(response.processoPaiId()).isNull();
    }

    @Test
    void listarNaoExpoeProcessoPaiQuandoEleEstaSoftDeletado() {
        UUID id = UUID.randomUUID();
        UUID paiId = UUID.randomUUID();

        DocumentoEntity existente = documentoExistente(id, 1, "hash-filho");
        existente.setProcessoPaiId(paiId);

        DocumentoEntity pai = documentoExistente(paiId, 1, "hash-pai");
        pai.setDeletado(true);

        when(documentoRepository.findByDeletadoFalseOrderByTituloAsc()).thenReturn(List.of(existente));
        when(documentoRepository.findAllById(List.of(paiId))).thenReturn(List.of(pai));
        when(categoriaAccessService.podeAcessarCategoria(any())).thenReturn(true);

        List<DocumentoResponseDTO> resposta = documentoService.listar(null);

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).processoPaiId()).isNull();
    }

    // --- Etapa 18 (decisão 9): filtro de listagem por revisão vencida ---

    @Test
    void listarComRevisaoVencidaSoDevolveDocumentosComProximaRevisaoNoPassado() {
        DocumentoEntity vencido = documentoExistente(UUID.randomUUID(), 1, "hash-vencido");
        vencido.setProximaRevisao(LocalDate.now().minusDays(1));

        DocumentoEntity emDia = documentoExistente(UUID.randomUUID(), 1, "hash-em-dia");
        emDia.setProximaRevisao(LocalDate.now().plusDays(1));

        DocumentoEntity semPeriodicidade = documentoExistente(UUID.randomUUID(), 1, "hash-sem-periodicidade");
        semPeriodicidade.setProximaRevisao(null);

        when(documentoRepository.findByDeletadoFalseOrderByTituloAsc())
                .thenReturn(List.of(vencido, emDia, semPeriodicidade));
        when(categoriaAccessService.podeAcessarCategoria(any())).thenReturn(true);

        List<DocumentoResponseDTO> resposta = documentoService.listar(null, true);

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).id()).isEqualTo(vencido.getId());
    }

    // A6: documento OBSOLETO nunca conta como "revisão vencida".
    @Test
    void listarComRevisaoVencidaNuncaDevolveDocumentoObsoleto() {
        DocumentoEntity obsoletoVencido = documentoExistente(UUID.randomUUID(), 1, "hash-obsoleto");
        obsoletoVencido.setProximaRevisao(LocalDate.now().minusDays(1));
        obsoletoVencido.setStatusCicloVida(StatusCicloVida.OBSOLETO);

        when(documentoRepository.findByDeletadoFalseOrderByTituloAsc()).thenReturn(List.of(obsoletoVencido));
        when(categoriaAccessService.podeAcessarCategoria(any())).thenReturn(true);

        List<DocumentoResponseDTO> resposta = documentoService.listar(null, true);

        assertThat(resposta).isEmpty();
    }

    @Test
    void listarSemOFiltroDevolveTodosIndependenteDaRevisao() {
        DocumentoEntity vencido = documentoExistente(UUID.randomUUID(), 1, "hash-vencido");
        vencido.setProximaRevisao(LocalDate.now().minusDays(1));

        when(documentoRepository.findByDeletadoFalseOrderByTituloAsc()).thenReturn(List.of(vencido));
        when(categoriaAccessService.podeAcessarCategoria(any())).thenReturn(true);

        List<DocumentoResponseDTO> resposta = documentoService.listar(null);

        assertThat(resposta).hasSize(1);
    }

    @Test
    void atualizarStatusCicloVidaTrocaOStatusEAtualizaMetadadosSemVersionar() {
        UUID id = UUID.randomUUID();
        DocumentoEntity existente = documentoExistente(id, 1, "hash-qualquer");
        existente.setStatusCicloVida(StatusCicloVida.VIGENTE);

        when(documentoRepository.findById(id)).thenReturn(Optional.of(existente));
        when(currentUserProvider.getCurrentUserName()).thenReturn("Admin Teste");

        DocumentoResponseDTO resposta = documentoService.atualizarStatusCicloVida(id, StatusCicloVida.OBSOLETO);

        assertThat(resposta.statusCicloVida()).isEqualTo(StatusCicloVida.OBSOLETO);
        assertThat(existente.getAtualizadoPor()).isEqualTo("Admin Teste");
        assertThat(existente.getAtualizadoEm()).isNotNull();
        assertThat(existente.getVersaoAtual()).isEqualTo(1);
        verify(documentoVersaoRepository, never()).save(any());
    }

    // Etapa 17 (B4): status_ciclo_vida é metadado do chunk (Etapa 14)/pré-filtro do RAG
    // (Etapa 16) — a troca não versiona o documento, mas precisa disparar a reindexação pra
    // sincronizar o metadado nos chunks já existentes (sempre ligado, não atrás de flag).
    @Test
    void atualizarStatusCicloVidaDisparaReindexacaoMesmoSemVersionar() {
        UUID id = UUID.randomUUID();
        DocumentoEntity existente = documentoExistente(id, 3, "hash-qualquer");
        existente.setStatusCicloVida(StatusCicloVida.VIGENTE);

        when(documentoRepository.findById(id)).thenReturn(Optional.of(existente));
        when(currentUserProvider.getCurrentUserName()).thenReturn("Admin Teste");

        documentoService.atualizarStatusCicloVida(id, StatusCicloVida.OBSOLETO);

        ArgumentCaptor<DocumentoAlteradoEvent> captor = ArgumentCaptor.forClass(DocumentoAlteradoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().documentoId()).isEqualTo(id);
        assertThat(captor.getValue().versao()).isEqualTo(3);
    }

    // (a) Fluxo legado sem estruturado grava documento e versão idênticos a antes.
    @Test
    void fluxoLegadoSemEstruturadoMantemConteudoEstruturadoSempreNuloNaCriacao() {
        DocumentoRequestDTO dto = new DocumentoRequestDTO("Título", "<p>Conteúdo</p>", null, 1L);

        ArgumentCaptor<DocumentoEntity> captorDocumento = ArgumentCaptor.forClass(DocumentoEntity.class);
        documentoService.criar(dto);

        verify(documentoRepository, atLeastOnce()).save(captorDocumento.capture());
        assertThat(captorDocumento.getAllValues().get(0).getConteudoEstruturado()).isNull();

        ArgumentCaptor<DocumentoVersaoEntity> captorVersao = ArgumentCaptor.forClass(DocumentoVersaoEntity.class);
        verify(documentoVersaoRepository).save(captorVersao.capture());
        assertThat(captorVersao.getValue().getConteudoEstruturado()).isNull();
        assertThat(captorVersao.getValue().getVersaoSchema()).isNull();
    }

    // (b) Documento com estruturado, atualizado via REST legado → nova versão com
    // conteudo_estruturado NULL; versão anterior nunca é tocada (só um INSERT novo).
    @Test
    void atualizarPeloFluxoLegadoInvalidaOConteudoEstruturadoExistente() {
        UUID id = UUID.randomUUID();
        DocumentoEntity existente = documentoExistente(id, 1, "hash-antigo-arbitrario");
        existente.setConteudoEstruturado("{\"objetivo\":\"antigo\"}");
        existente.setVersaoSchema("v1");

        when(documentoRepository.findById(id)).thenReturn(Optional.of(existente));

        DocumentoRequestDTO dto = new DocumentoRequestDTO("Novo Título", "<p>Novo conteúdo</p>", "ajuste", 1L);

        documentoService.atualizar(id, dto);

        assertThat(existente.getConteudoEstruturado()).isNull();
        assertThat(existente.getVersaoSchema()).isNull();

        ArgumentCaptor<DocumentoVersaoEntity> captor = ArgumentCaptor.forClass(DocumentoVersaoEntity.class);
        verify(documentoVersaoRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getNumeroVersao()).isEqualTo(2);
        assertThat(captor.getValue().getConteudoEstruturado()).isNull();
    }

    // Etapa 18 (achado na verificação V4): proximaRevisao precisa ser recalculada numa nova
    // versão de conteúdo, mesmo pelo fluxo LEGADO (sem bloco de metadados — só
    // aplicarNovaVersao roda, nunca aplicarMetadadosEstruturados). periodicidadeRevisaoMeses
    // já definido antes continua valendo (o legado não o altera), só a data é recalculada a
    // partir de hoje.
    @Test
    void atualizarPeloFluxoLegadoComMudancaDeConteudoRecalculaProximaRevisao() {
        UUID id = UUID.randomUUID();
        DocumentoEntity existente = documentoExistente(id, 1, "hash-antigo-arbitrario");
        existente.setPeriodicidadeRevisaoMeses(6);
        existente.setProximaRevisao(LocalDate.now().minusDays(100)); // valor antigo, já vencido

        when(documentoRepository.findById(id)).thenReturn(Optional.of(existente));

        DocumentoRequestDTO dto = new DocumentoRequestDTO("Novo Título", "<p>Novo conteúdo</p>", "ajuste", 1L);

        documentoService.atualizar(id, dto);

        assertThat(existente.getProximaRevisao()).isEqualTo(LocalDate.now().plusMonths(6));
    }

    // (P3) Atualizar pelo fluxo legado SEM mudança de conteúdo (mesmo hash — só categoria/
    // metadado) é um caminho totalmente diferente: nunca passa por aplicarNovaVersao (que é
    // quem nulifica o estruturado), então um conteúdo estruturado já existente precisa
    // continuar intacto, e nenhuma versão nova pode ser criada.
    //
    // Etapa 17/B4 (achado na verificação V4, 2026-10-10): categoria é metadado do chunk
    // (Etapa 14) — mudar só a categoria precisa disparar reindexação (categoria_id do chunk
    // fica dessincronizado senão), mesmo sem versionar. Esta asserção substitui a antiga
    // `verify(eventPublisher, never())...`, que documentava o gap.
    @Test
    void atualizarSoACategoriaSemMudarOConteudoPreservaOEstruturadoNaoVersionaEDisparaReindexacao() {
        UUID id = UUID.randomUUID();
        String titulo = "Título Estável";
        String html = "<p>Conteúdo estável</p>";
        String hashAtual = sha256(titulo, html);

        DocumentoEntity existente = documentoExistente(id, 1, hashAtual);
        existente.setConteudoEstruturado("{\"objetivo\":\"preservado\"}");
        existente.setVersaoSchema("v1");
        existente.setCategoriaId(1L);

        when(documentoRepository.findById(id)).thenReturn(Optional.of(existente));

        // Só a categoria muda (2L em vez de 1L) — título e HTML idênticos, mesmo hash.
        DocumentoRequestDTO dto = new DocumentoRequestDTO(titulo, html, null, 2L);

        documentoService.atualizar(id, dto);

        assertThat(existente.getConteudoEstruturado()).isEqualTo("{\"objetivo\":\"preservado\"}");
        assertThat(existente.getVersaoSchema()).isEqualTo("v1");
        assertThat(existente.getCategoriaId()).isEqualTo(2L);
        assertThat(existente.getVersaoAtual()).isEqualTo(1);
        verify(documentoVersaoRepository, never()).save(any());

        ArgumentCaptor<DocumentoAlteradoEvent> captor = ArgumentCaptor.forClass(DocumentoAlteradoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().documentoId()).isEqualTo(id);
        assertThat(captor.getValue().versao()).isEqualTo(1); // mesma versão — não houve nova versão de conteúdo
    }

    // Contraponto: nem categoria nem conteúdo mudaram — nada a reindexar, nenhum evento.
    @Test
    void atualizarSemMudarCategoriaNemConteudoNaoDisparaReindexacao() {
        UUID id = UUID.randomUUID();
        String titulo = "Título Estável";
        String html = "<p>Conteúdo estável</p>";
        String hashAtual = sha256(titulo, html);

        DocumentoEntity existente = documentoExistente(id, 1, hashAtual);
        existente.setCategoriaId(1L);

        when(documentoRepository.findById(id)).thenReturn(Optional.of(existente));

        DocumentoRequestDTO dto = new DocumentoRequestDTO(titulo, html, null, 1L);

        documentoService.atualizar(id, dto);

        verify(documentoVersaoRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // (d) restaurarVersao traz conteudo_estruturado e versao_schema de volta da versão restaurada.
    @Test
    void restaurarVersaoTrazDeVoltaOConteudoEstruturadoEVersaoSchemaDaquelaVersao() {
        UUID id = UUID.randomUUID();
        DocumentoEntity existente = documentoExistente(id, 2, "hash-atual-diferente");

        DocumentoVersaoEntity versaoAntiga = new DocumentoVersaoEntity();
        versaoAntiga.setDocumentoId(id);
        versaoAntiga.setNumeroVersao(1);
        versaoAntiga.setTitulo("Título Versão 1");
        versaoAntiga.setConteudoHtml("<p>Conteúdo versão 1</p>");
        versaoAntiga.setAutor("Autor Original");
        versaoAntiga.setCriadoEm(LocalDateTime.now());
        versaoAntiga.setConteudoEstruturado("{\"objetivo\":\"da versão 1\"}");
        versaoAntiga.setVersaoSchema("v1");

        when(documentoRepository.findById(id)).thenReturn(Optional.of(existente));
        when(documentoVersaoRepository.findByDocumentoIdAndNumeroVersao(id, 1))
                .thenReturn(Optional.of(versaoAntiga));

        documentoService.restaurarVersao(id, 1);

        assertThat(existente.getConteudoEstruturado()).isEqualTo("{\"objetivo\":\"da versão 1\"}");
        assertThat(existente.getVersaoSchema()).isEqualTo("v1");

        ArgumentCaptor<DocumentoVersaoEntity> captor = ArgumentCaptor.forClass(DocumentoVersaoEntity.class);
        verify(documentoVersaoRepository).save(captor.capture());
        assertThat(captor.getValue().getConteudoEstruturado()).isEqualTo("{\"objetivo\":\"da versão 1\"}");
    }

    // (e) conteudo_estruturado guarda só conteúdo, nunca metadado (B3) — independência
    // estrutural entre o campo e as colunas de metadado (nenhum setter deriva o outro).
    @Test
    void conteudoEstruturadoNaoInterfereComOsCamposDeMetadado() {
        UUID id = UUID.randomUUID();
        DocumentoEntity documento = documentoExistente(id, 1, "hash-qualquer");
        documento.setTipoDocumento(TipoDocumento.PROCESSO);
        documento.setStatusCicloVida(StatusCicloVida.VIGENTE);
        documento.setMacroprocessoId(5L);

        documento.setConteudoEstruturado("{\"objetivo\":\"x\",\"fluxo\":[]}");

        assertThat(documento.getTipoDocumento()).isEqualTo(TipoDocumento.PROCESSO);
        assertThat(documento.getStatusCicloVida()).isEqualTo(StatusCicloVida.VIGENTE);
        assertThat(documento.getMacroprocessoId()).isEqualTo(5L);
    }

    private DocumentoEntity documentoExistente(UUID id, int versaoAtual, String hash) {
        DocumentoEntity documento = new DocumentoEntity();
        documento.setId(id);
        documento.setTitulo("Título Atual");
        documento.setConteudoHtml("<p>Conteúdo atual</p>");
        documento.setHashConteudo(hash);
        documento.setVersaoAtual(versaoAtual);
        documento.setStatusIndexacao(StatusIndexacao.INDEXADO);
        documento.setCriadoPor("Autor Original");
        documento.setCriadoEm(LocalDateTime.now());
        documento.setDeletado(false);
        return documento;
    }

    private String sha256(String titulo, String html) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(titulo.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(html.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
