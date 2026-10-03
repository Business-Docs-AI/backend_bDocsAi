package br.com.example.senac.businessDocsAi.chat.service;

import br.com.example.senac.businessDocsAi.categories.repository.ICategoriaRascunhoRepository;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.chat.dto.MensagemRequestDTO;
import br.com.example.senac.businessDocsAi.chat.dto.MensagemResponseDTO;
import br.com.example.senac.businessDocsAi.chat.entity.ConversaEntity;
import br.com.example.senac.businessDocsAi.chat.entity.MensagemEntity;
import br.com.example.senac.businessDocsAi.chat.repository.IConversaRepository;
import br.com.example.senac.businessDocsAi.chat.repository.IMensagemRepository;
import br.com.example.senac.businessDocsAi.document.repository.IRascunhoDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.service.DocumentoService;
import br.com.example.senac.businessDocsAi.exception.BadRequestException;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import br.com.example.senac.businessDocsAi.security.CurrentUserProvider;
import br.com.example.senac.businessDocsAi.ai.generation.RagAssistant;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final Long USUARIO_A = 1L;
    private static final Long USUARIO_B = 2L;

    @Mock
    private IConversaRepository conversaRepository;

    @Mock
    private IMensagemRepository mensagemRepository;

    @Mock
    private IRascunhoDocumentoRepository rascunhoRepository;

    @Mock
    private ICategoriaRascunhoRepository categoriaRascunhoRepository;

    @Mock
    private RagAssistant ragAssistantSomenteLeitura;

    @Mock
    private RagAssistant ragAssistantComFerramentas;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AudioTranscricaoService audioTranscricaoService;

    @Mock
    private AnexoTextoExtractorService anexoTextoExtractorService;

    @Mock
    private ArmazenamentoAnexoService armazenamentoAnexoService;

    @Mock
    private PersistentChatMemoryStore persistentChatMemoryStore;

    @Mock
    private ICategoryRepository categoryRepository;

    @Mock
    private DocumentoService documentoService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                conversaRepository, mensagemRepository, rascunhoRepository, categoriaRascunhoRepository,
                ragAssistantSomenteLeitura, ragAssistantComFerramentas,
                currentUserProvider, new ObjectMapper(), new MarkdownConversorService(2000),
                audioTranscricaoService, anexoTextoExtractorService, armazenamentoAnexoService,
                persistentChatMemoryStore, categoryRepository, documentoService, eventPublisher
        );
    }

    @Test
    void usuarioNaoConsegueVerConversaDeOutroUsuario() {
        UUID conversaId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_B);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.buscarConversa(conversaId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void usuarioNaoConsegueEnviarMensagemEmConversaDeOutroUsuario() {
        UUID conversaId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_B);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.enviarMensagem(conversaId, new MensagemRequestDTO("Oi")))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(ragAssistantSomenteLeitura, ragAssistantComFerramentas);
        verify(mensagemRepository, never()).save(any());
    }

    @Test
    void usuarioNaoConsegueExcluirConversaDeOutroUsuario() {
        UUID conversaId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_B);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.excluirConversa(conversaId))
                .isInstanceOf(NotFoundException.class);

        verify(conversaRepository, never()).delete(any());
        verify(mensagemRepository, never()).deleteByConversaId(any());
    }

    @Test
    void excluirConversaTambemRemoveOsArquivosDaConversaDoDisco() {
        UUID conversaId = UUID.randomUUID();
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setUsuarioId(USUARIO_A);

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_A);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_A)).thenReturn(Optional.of(conversa));

        chatService.excluirConversa(conversaId);

        verify(conversaRepository).delete(conversa);
        verify(mensagemRepository).deleteByConversaId(conversaId);
        verify(rascunhoRepository).deleteByConversaId(conversaId);
        verify(categoriaRascunhoRepository).deleteByConversaId(conversaId);
        verify(armazenamentoAnexoService).excluirTudoDaConversa(conversaId);
    }

    @Test
    void enviarMensagemNaPropriaConversaDeveSalvarPerguntaERespostaComFontes() {
        UUID conversaId = UUID.randomUUID();
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setUsuarioId(USUARIO_A);

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_A);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_A)).thenReturn(Optional.of(conversa));
        when(currentUserProvider.isEditorOuAdmin()).thenReturn(false);

        UUID documentoId = UUID.randomUUID();
        Metadata metadata = new Metadata()
                .put("documento_id", documentoId)
                .put("versao", 1)
                .put("titulo", "Documento X")
                .put("secao", "intro");
        Content fonte = Content.from(TextSegment.from("Trecho relevante", metadata));

        Result<String> resultado = Result.<String>builder()
                .content("Resposta da IA")
                .sources(List.of(fonte))
                .build();

        when(ragAssistantSomenteLeitura.responder(eq(conversaId), eq("Qual a resposta?"))).thenReturn(resultado);

        MensagemResponseDTO resposta = chatService.enviarMensagem(conversaId, new MensagemRequestDTO("Qual a resposta?"));

        assertThat(resposta.conteudo()).isEqualTo("Resposta da IA");
        assertThat(resposta.fontes()).hasSize(1);
        assertThat(resposta.fontes().get(0).documentoId()).isEqualTo(documentoId);
        assertThat(resposta.fontes().get(0).secao()).isEqualTo("intro");
        assertThat(resposta.fontes().get(0).link()).isEqualTo("/documentos/" + documentoId + "#intro");

        // Pergunta do usuário e resposta da IA são persistidas como duas mensagens.
        verify(mensagemRepository, times(2)).save(any(MensagemEntity.class));
        verifyNoInteractions(ragAssistantComFerramentas);
    }

    @Test
    void respostaDoAssistenteSemTextoUsaMensagemDeFallbackEmVezDeNulo() {
        UUID conversaId = UUID.randomUUID();
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setUsuarioId(USUARIO_A);

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_A);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_A)).thenReturn(Optional.of(conversa));
        when(currentUserProvider.isEditorOuAdmin()).thenReturn(false);
        // Resultado sem texto, só com fonte — simula o modelo terminando o turno sem
        // nenhum conteúdo textual (ex.: só uma chamada de ferramenta, sem comentário).
        when(ragAssistantSomenteLeitura.responder(any(), any()))
                .thenReturn(Result.<String>builder().content(null).build());

        MensagemResponseDTO resposta = chatService.enviarMensagem(conversaId, new MensagemRequestDTO("Oi"));

        assertThat(resposta.conteudo()).isNotBlank();
        verify(mensagemRepository, times(2)).save(argThat(m -> m.getConteudo() != null));
    }

    @Test
    void enviarMensagemDevolvePropostaDeDocumentoPendenteLidaDoRascunho() {
        UUID conversaId = UUID.randomUUID();
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setUsuarioId(USUARIO_A);

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_A);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_A)).thenReturn(Optional.of(conversa));
        when(currentUserProvider.isEditorOuAdmin()).thenReturn(true);
        when(ragAssistantComFerramentas.responder(any(), any()))
                .thenReturn(Result.<String>builder().content("Proposta preparada, confirma?").build());

        br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity rascunho =
                new br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity();
        rascunho.setId(UUID.randomUUID());
        rascunho.setTipo(br.com.example.senac.businessDocsAi.document.entity.TipoRascunho.CRIAR);
        rascunho.setCategoriaId(7L);
        rascunho.setTitulo("Política X");
        rascunho.setConteudoHtml("<p>conteúdo</p>");
        rascunho.setStatus(br.com.example.senac.businessDocsAi.document.entity.StatusRascunho.PENDENTE);

        when(rascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(
                conversaId, br.com.example.senac.businessDocsAi.document.entity.StatusRascunho.PENDENTE))
                .thenReturn(Optional.of(rascunho));

        br.com.example.senac.businessDocsAi.categories.entity.CategoryEntity categoria =
                new br.com.example.senac.businessDocsAi.categories.entity.CategoryEntity();
        categoria.setId(7L);
        categoria.setName("ERP / Fiscal");
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(categoria));

        MensagemResponseDTO resposta = chatService.enviarMensagem(conversaId, new MensagemRequestDTO("Cria documento"));

        assertThat(resposta.conteudo()).isEqualTo("Proposta preparada, confirma?");
        assertThat(resposta.propostaDocumento()).isNotNull();
        assertThat(resposta.propostaDocumento().titulo()).isEqualTo("Política X");
        assertThat(resposta.propostaDocumento().conteudoHtml()).isEqualTo("<p>conteúdo</p>");
        assertThat(resposta.propostaDocumento().categoriaNome()).isEqualTo("ERP / Fiscal");
        assertThat(resposta.documentoConfirmado()).isNull();
    }

    @Test
    void enviarMensagemDevolveDocumentoConfirmadoQuandoRascunhoFoiConfirmadoNesteTurno() {
        UUID conversaId = UUID.randomUUID();
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setUsuarioId(USUARIO_A);

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_A);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_A)).thenReturn(Optional.of(conversa));
        when(currentUserProvider.isEditorOuAdmin()).thenReturn(true);
        when(ragAssistantComFerramentas.responder(any(), any()))
                .thenReturn(Result.<String>builder().content("Confirmado.").build());

        when(rascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(
                conversaId, br.com.example.senac.businessDocsAi.document.entity.StatusRascunho.PENDENTE))
                .thenReturn(Optional.empty());

        UUID documentoId = UUID.randomUUID();
        br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity rascunhoConfirmado =
                new br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity();
        rascunhoConfirmado.setStatus(br.com.example.senac.businessDocsAi.document.entity.StatusRascunho.CONFIRMADO);
        rascunhoConfirmado.setConfirmadoEm(java.time.LocalDateTime.now().plusSeconds(1));
        rascunhoConfirmado.setDocumentoResultanteId(documentoId);

        when(rascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(
                conversaId, br.com.example.senac.businessDocsAi.document.entity.StatusRascunho.CONFIRMADO))
                .thenReturn(Optional.of(rascunhoConfirmado));

        br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO documentoDTO =
                new br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO(
                        documentoId, "Política X", "<p>conteúdo</p>", 1,
                        br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao.PENDENTE,
                        "Autor", java.time.LocalDateTime.now(), null, null, 7L, "ERP / Fiscal"
                );
        when(documentoService.buscarPorId(documentoId)).thenReturn(documentoDTO);

        MensagemResponseDTO resposta = chatService.enviarMensagem(conversaId, new MensagemRequestDTO("confirmo"));

        assertThat(resposta.documentoConfirmado()).isEqualTo(documentoDTO);
        assertThat(resposta.propostaDocumento()).isNull();
    }

    @Test
    void enviarMensagemPublicaEventoParaGeracaoDeTituloEmBackground() {
        UUID conversaId = UUID.randomUUID();
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setUsuarioId(USUARIO_A);

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_A);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_A)).thenReturn(Optional.of(conversa));
        when(currentUserProvider.isEditorOuAdmin()).thenReturn(false);
        when(ragAssistantSomenteLeitura.responder(any(), any()))
                .thenReturn(Result.<String>builder().content("ok").build());

        chatService.enviarMensagem(conversaId, new MensagemRequestDTO("Qual a política de férias?"));

        org.mockito.ArgumentCaptor<br.com.example.senac.businessDocsAi.chat.event.ConversaMensagemRecebidaEvent> captor =
                org.mockito.ArgumentCaptor.forClass(br.com.example.senac.businessDocsAi.chat.event.ConversaMensagemRecebidaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        assertThat(captor.getValue().conversaId()).isEqualTo(conversaId);
        assertThat(captor.getValue().perguntaTexto()).isEqualTo("Qual a política de férias?");
    }

    @Test
    void usuarioComumSempreUsaOAssistenteSomenteLeitura() {
        UUID conversaId = UUID.randomUUID();
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setUsuarioId(USUARIO_A);

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_A);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_A)).thenReturn(Optional.of(conversa));
        when(currentUserProvider.isEditorOuAdmin()).thenReturn(false);
        when(ragAssistantSomenteLeitura.responder(any(), any()))
                .thenReturn(Result.<String>builder().content("ok").build());

        chatService.enviarMensagem(conversaId, new MensagemRequestDTO("Oi"));

        verify(ragAssistantSomenteLeitura).responder(eq(conversaId), eq("Oi"));
        verifyNoInteractions(ragAssistantComFerramentas);
    }

    @Test
    void editorOuAdminUsaOAssistenteComFerramentas() {
        UUID conversaId = UUID.randomUUID();
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setUsuarioId(USUARIO_A);

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_A);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_A)).thenReturn(Optional.of(conversa));
        when(currentUserProvider.isEditorOuAdmin()).thenReturn(true);
        when(ragAssistantComFerramentas.responder(any(), any()))
                .thenReturn(Result.<String>builder().content("ok").build());

        chatService.enviarMensagem(conversaId, new MensagemRequestDTO("Cria um documento sobre X"));

        verify(ragAssistantComFerramentas).responder(eq(conversaId), eq("Cria um documento sobre X"));
        verifyNoInteractions(ragAssistantSomenteLeitura);
    }

    @Test
    void contextoDeConversaEhLimpoMesmoQuandoOAssistenteFalha() {
        UUID conversaId = UUID.randomUUID();
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setUsuarioId(USUARIO_A);

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_A);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_A)).thenReturn(Optional.of(conversa));
        when(currentUserProvider.isEditorOuAdmin()).thenReturn(false);
        when(ragAssistantSomenteLeitura.responder(any(), any())).thenThrow(new RuntimeException("falha simulada"));

        assertThatThrownBy(() -> chatService.enviarMensagem(conversaId, new MensagemRequestDTO("Oi")))
                .isInstanceOf(RuntimeException.class);

        assertThatThrownBy(ConversaContextHolder::atual).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void enviarMensagemComArquivoSemTextoAudioOuAnexoLancaBadRequest() {
        UUID conversaId = UUID.randomUUID();
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setUsuarioId(USUARIO_A);

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_A);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_A)).thenReturn(Optional.of(conversa));

        assertThatThrownBy(() -> chatService.enviarMensagemComArquivo(conversaId, null, null, null))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(ragAssistantSomenteLeitura, ragAssistantComFerramentas);
        verify(mensagemRepository, never()).save(any());
    }

    @Test
    void enviarMensagemComAudioTranscreveSalvaOArquivoEEnviaATranscricaoAoAssistente() {
        UUID conversaId = UUID.randomUUID();
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setUsuarioId(USUARIO_A);

        MockMultipartFile audio = new MockMultipartFile("audio", "pergunta.mp3", "audio/mpeg", "conteudo-binario".getBytes());

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_A);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_A)).thenReturn(Optional.of(conversa));
        when(currentUserProvider.isEditorOuAdmin()).thenReturn(false);
        when(audioTranscricaoService.transcrever(audio)).thenReturn("Qual é a política de férias?");
        when(ragAssistantSomenteLeitura.responder(any(), any()))
                .thenReturn(Result.<String>builder().content("Resposta sobre férias").build());

        chatService.enviarMensagemComArquivo(conversaId, null, audio, null);

        verify(armazenamentoAnexoService).salvar(conversaId, audio);
        verify(ragAssistantSomenteLeitura).responder(
                eq(conversaId), argThat(texto -> texto.contains("Qual é a política de férias?"))
        );
    }

    @Test
    void enviarMensagemComAnexoExtraiTextoESalvaOArquivo() {
        UUID conversaId = UUID.randomUUID();
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setUsuarioId(USUARIO_A);

        MockMultipartFile anexo = new MockMultipartFile("anexo", "politica.txt", "text/plain", "Trinta dias de férias por ano.".getBytes());

        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_A);
        when(conversaRepository.findByIdAndUsuarioId(conversaId, USUARIO_A)).thenReturn(Optional.of(conversa));
        when(currentUserProvider.isEditorOuAdmin()).thenReturn(false);
        when(anexoTextoExtractorService.extrairTexto(anexo)).thenReturn("Trinta dias de férias por ano.");
        when(ragAssistantSomenteLeitura.responder(any(), any()))
                .thenReturn(Result.<String>builder().content("ok").build());

        chatService.enviarMensagemComArquivo(conversaId, "Cadastre isso como documentação", null, anexo);

        verify(armazenamentoAnexoService).salvar(conversaId, anexo);
        verify(ragAssistantSomenteLeitura).responder(eq(conversaId), argThat(texto ->
                texto.contains("Cadastre isso como documentação")
                        && texto.contains("politica.txt")
                        && texto.contains("Trinta dias de férias por ano.")
        ));
    }
}
