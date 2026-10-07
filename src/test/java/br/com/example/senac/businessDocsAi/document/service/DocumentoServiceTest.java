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
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoVersaoRepository;
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
import java.time.LocalDateTime;
import java.util.HexFormat;
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

    private DocumentoService documentoService;

    @BeforeEach
    void setUp() {
        documentoService = new DocumentoService(
                documentoRepository, documentoVersaoRepository, categoryRepository, htmlSanitizerService,
                currentUserProvider, categoriaAccessService, eventPublisher
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
