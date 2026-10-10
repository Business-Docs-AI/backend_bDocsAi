package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoEstruturadoMetadadosDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoRequestDTO;
import br.com.example.senac.businessDocsAi.document.entity.Confidencialidade;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoAreaParticipanteEntity;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoVersaoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusCicloVida;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import br.com.example.senac.businessDocsAi.document.entity.TipoDocumento;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoAreaParticipanteRepository;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoVersaoRepository;
import br.com.example.senac.businessDocsAi.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Etapa 13.3 — {@code criarComEstrutura}/{@code atualizarComEstrutura}: aplicam o bloco de
 * metadados nas colunas do documento (decisão B3) e substituem as áreas participantes,
 * independente de {@code DocumentoEstruturadoAplicadorService} (testado separadamente).
 */
@ExtendWith(MockitoExtension.class)
class DocumentoServiceComEstruturaTest {

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
    void criarComEstruturaAplicaMetadadosNoDocumentoEGravaConteudoEstruturadoNaVersao() {
        DocumentoRequestDTO dto = new DocumentoRequestDTO("Processo", "<h2>x</h2>", "comentario", 7L);
        DocumentoEstruturadoMetadadosDTO metadados = new DocumentoEstruturadoMetadadosDTO(
                TipoDocumento.PROCESSO, 5L, null, "Analista Financeiro", "Gestor", 12,
                Confidencialidade.INTERNO, List.of("reembolso"), List.of(2L, 3L)
        );

        documentoService.criarComEstrutura(dto, "{\"objetivo\":\"x\"}", "1.0", metadados);

        ArgumentCaptor<DocumentoEntity> documentoCaptor = ArgumentCaptor.forClass(DocumentoEntity.class);
        verify(documentoRepository).save(documentoCaptor.capture());
        DocumentoEntity salvo = documentoCaptor.getValue();
        assertThat(salvo.getTipoDocumento()).isEqualTo(TipoDocumento.PROCESSO);
        assertThat(salvo.getMacroprocessoId()).isEqualTo(5L);
        assertThat(salvo.getDonoProcesso()).isEqualTo("Analista Financeiro");
        assertThat(salvo.getAprovador()).isEqualTo("Gestor");
        assertThat(salvo.getPeriodicidadeRevisaoMeses()).isEqualTo(12);
        assertThat(salvo.getConfidencialidade()).isEqualTo(Confidencialidade.INTERNO);
        assertThat(salvo.getTags()).containsExactly("reembolso");
        assertThat(salvo.getStatusCicloVida()).isEqualTo(StatusCicloVida.VIGENTE);
        // O documento espelha conteudoEstruturado/versaoSchema da sua própria versão atual
        // (mesma regra de aplicarNovaVersao, usada por atualizarComEstrutura/restaurarVersao)
        // — achado faltando neste teste até o teste manual E2E da Etapa 13.6 revelar que só
        // documento_versao recebia o valor na CRIAÇÃO estruturada.
        assertThat(salvo.getConteudoEstruturado()).isEqualTo("{\"objetivo\":\"x\"}");
        assertThat(salvo.getVersaoSchema()).isEqualTo("1.0");

        ArgumentCaptor<DocumentoVersaoEntity> versaoCaptor = ArgumentCaptor.forClass(DocumentoVersaoEntity.class);
        verify(documentoVersaoRepository).save(versaoCaptor.capture());
        assertThat(versaoCaptor.getValue().getConteudoEstruturado()).isEqualTo("{\"objetivo\":\"x\"}");
        assertThat(versaoCaptor.getValue().getVersaoSchema()).isEqualTo("1.0");
        assertThat(versaoCaptor.getValue().getNumeroVersao()).isEqualTo(1);

        verify(documentoAreaParticipanteRepository).save(argThat(
                (DocumentoAreaParticipanteEntity a) -> a.getCategoriaId().equals(2L)
        ));
        verify(documentoAreaParticipanteRepository).save(argThat(
                (DocumentoAreaParticipanteEntity a) -> a.getCategoriaId().equals(3L)
        ));
    }

    @Test
    void criarComEstruturaSemMetadadosUsaNaoClassificado() {
        DocumentoRequestDTO dto = new DocumentoRequestDTO("Processo", "<h2>x</h2>", "comentario", 7L);

        documentoService.criarComEstrutura(dto, "{\"objetivo\":\"x\"}", "1.0", null);

        ArgumentCaptor<DocumentoEntity> documentoCaptor = ArgumentCaptor.forClass(DocumentoEntity.class);
        verify(documentoRepository).save(documentoCaptor.capture());
        assertThat(documentoCaptor.getValue().getTipoDocumento()).isEqualTo(TipoDocumento.NAO_CLASSIFICADO);
    }

    // atualizarComEstrutura nunca pula a versão por hash igual (diferente de atualizar() do
    // fluxo legado) — confirmar uma proposta estruturada é sempre uma ação deliberada.
    @Test
    void atualizarComEstruturaSempreCriaNovaVersao() {
        UUID documentoId = UUID.randomUUID();
        DocumentoEntity existente = documentoExistente(documentoId, "hash-antigo-irrelevante");
        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(existente));

        DocumentoRequestDTO dto = new DocumentoRequestDTO(
                existente.getTitulo(), existente.getConteudoHtml(), "comentario", existente.getCategoriaId()
        );
        DocumentoEstruturadoMetadadosDTO metadados = new DocumentoEstruturadoMetadadosDTO(
                TipoDocumento.PROCESSO, null, null, null, null, null, null, List.of(), List.of()
        );

        documentoService.atualizarComEstrutura(documentoId, dto, "{\"objetivo\":\"novo\"}", "1.0", metadados);

        assertThat(existente.getVersaoAtual()).isEqualTo(2);
        verify(documentoVersaoRepository).save(argThat(
                (DocumentoVersaoEntity v) -> "{\"objetivo\":\"novo\"}".equals(v.getConteudoEstruturado())
        ));
    }

    @Test
    void atualizarComEstruturaRejeitaAutorreferenciaDeHierarquia() {
        UUID documentoId = UUID.randomUUID();
        DocumentoEntity existente = documentoExistente(documentoId, "hash-qualquer");
        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(existente));

        DocumentoRequestDTO dto = new DocumentoRequestDTO(
                existente.getTitulo(), existente.getConteudoHtml(), "comentario", existente.getCategoriaId()
        );
        DocumentoEstruturadoMetadadosDTO metadados = new DocumentoEstruturadoMetadadosDTO(
                TipoDocumento.PROCESSO, null, documentoId, null, null, null, null, List.of(), List.of()
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> documentoService.atualizarComEstrutura(documentoId, dto, "{}", "1.0", metadados)
        ).isInstanceOf(br.com.example.senac.businessDocsAi.exception.BadRequestException.class);
    }

    private DocumentoEntity documentoExistente(UUID id, String hash) {
        DocumentoEntity documento = new DocumentoEntity();
        documento.setId(id);
        documento.setTitulo("Título Existente");
        documento.setConteudoHtml("<p>Existente</p>");
        documento.setHashConteudo(hash);
        documento.setVersaoAtual(1);
        documento.setStatusIndexacao(StatusIndexacao.INDEXADO);
        documento.setCriadoPor("Autor Original");
        documento.setCriadoEm(LocalDateTime.now());
        documento.setDeletado(false);
        documento.setCategoriaId(7L);
        return documento;
    }
}
