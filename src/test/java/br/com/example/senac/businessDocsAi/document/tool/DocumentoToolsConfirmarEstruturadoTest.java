package br.com.example.senac.businessDocsAi.document.tool;

import br.com.example.senac.businessDocsAi.ai.retrieval.PesquisaService;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.categories.service.CategoryService;
import br.com.example.senac.businessDocsAi.chat.service.ConversaContextHolder;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import br.com.example.senac.businessDocsAi.document.entity.StatusRascunho;
import br.com.example.senac.businessDocsAi.document.entity.TipoRascunho;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoAreaParticipanteRepository;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoVersaoRepository;
import br.com.example.senac.businessDocsAi.document.repository.IRascunhoDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.service.DocumentoEstruturadoAplicadorService;
import br.com.example.senac.businessDocsAi.document.service.DocumentoService;
import br.com.example.senac.businessDocsAi.document.service.HtmlSanitizerService;
import br.com.example.senac.businessDocsAi.security.CurrentUserProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Prova a regra C3 fim-a-fim pelo caminho do CHAT (não só pela chamada direta a
 * {@code DocumentoService.atualizar}, já testada em {@code DocumentoServiceTest}):
 * {@code DocumentoTools.confirmarRascunhoPendente} delega para um {@code DocumentoService}
 * REAL aqui (só os repositórios/dependências de baixo nível são mocks) — nada precisou
 * mudar em {@code DocumentoTools} para isso funcionar, porque {@code DocumentoRequestDTO}
 * nunca carrega conteúdo estruturado.
 */
@ExtendWith(MockitoExtension.class)
class DocumentoToolsConfirmarEstruturadoTest {

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
    private PesquisaService pesquisaService;

    @Mock
    private IRascunhoDocumentoRepository rascunhoRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private IDocumentoAreaParticipanteRepository documentoAreaParticipanteRepository;

    @Mock
    private DocumentoEstruturadoAplicadorService documentoEstruturadoAplicadorService;

    private DocumentoTools documentoTools;

    private final UUID conversaId = UUID.randomUUID();
    private final UUID turnoAtual = UUID.randomUUID();
    private final UUID turnoAnterior = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        DocumentoService documentoServiceReal = new DocumentoService(
                documentoRepository, documentoVersaoRepository, categoryRepository, htmlSanitizerService,
                currentUserProvider, categoriaAccessService, eventPublisher, documentoAreaParticipanteRepository
        );

        documentoTools = new DocumentoTools(
                documentoServiceReal, pesquisaService, rascunhoRepository, categoryService, categoriaAccessService,
                documentoEstruturadoAplicadorService
        );

        ConversaContextHolder.iniciar(conversaId, turnoAtual);

        lenient().when(htmlSanitizerService.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(currentUserProvider.getCurrentUserName()).thenReturn("Autor Teste");
        lenient().when(documentoVersaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        ConversaContextHolder.limpar();
    }

    @Test
    void confirmarRascunhoAtualizarLegadoInvalidaOConteudoEstruturadoExistente() {
        UUID documentoId = UUID.randomUUID();

        DocumentoEntity documentoExistente = new DocumentoEntity();
        documentoExistente.setId(documentoId);
        documentoExistente.setTitulo("Título Antigo");
        documentoExistente.setConteudoHtml("<p>Antigo</p>");
        documentoExistente.setHashConteudo("hash-bem-diferente-do-novo");
        documentoExistente.setVersaoAtual(1);
        documentoExistente.setStatusIndexacao(StatusIndexacao.INDEXADO);
        documentoExistente.setCriadoPor("Autor Original");
        documentoExistente.setCriadoEm(LocalDateTime.now());
        documentoExistente.setDeletado(false);
        documentoExistente.setCategoriaId(1L);
        // O documento JÁ tem conteúdo estruturado de uma confirmação anterior (via tool
        // estruturada, Etapa 13 — simulado aqui só com um valor fixo).
        documentoExistente.setConteudoEstruturado("{\"objetivo\":\"versão estruturada anterior\"}");
        documentoExistente.setVersaoSchema("v1");

        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documentoExistente));

        RascunhoDocumentoEntity rascunho = new RascunhoDocumentoEntity();
        rascunho.setConversaId(conversaId);
        rascunho.setTipo(TipoRascunho.ATUALIZAR);
        rascunho.setDocumentoIdAlvo(documentoId);
        rascunho.setCategoriaId(1L);
        rascunho.setTitulo("Título Novo (legado)");
        rascunho.setConteudoHtml("<p>Conteúdo novo, via fluxo legado de HTML</p>");
        rascunho.setStatus(StatusRascunho.PENDENTE);
        rascunho.setTurnoCriacao(turnoAnterior);
        rascunho.setCriadoEm(LocalDateTime.now());
        // rascunho.conteudoEstruturado fica null de propósito — é um rascunho LEGADO.

        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(rascunho));

        documentoTools.confirmarRascunhoPendente();

        assertThat(documentoExistente.getConteudoEstruturado()).isNull();
        assertThat(documentoExistente.getVersaoSchema()).isNull();

        ArgumentCaptor<br.com.example.senac.businessDocsAi.document.entity.DocumentoVersaoEntity> captor =
                ArgumentCaptor.forClass(br.com.example.senac.businessDocsAi.document.entity.DocumentoVersaoEntity.class);
        verify(documentoVersaoRepository).save(captor.capture());
        assertThat(captor.getValue().getNumeroVersao()).isEqualTo(2);
        assertThat(captor.getValue().getConteudoEstruturado()).isNull();
    }
}
