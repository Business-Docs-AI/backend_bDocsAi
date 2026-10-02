package br.com.example.senac.businessDocsAi.chat.service;

import br.com.example.senac.businessDocsAi.chat.dto.MensagemRequestDTO;
import br.com.example.senac.businessDocsAi.chat.dto.MensagemResponseDTO;
import br.com.example.senac.businessDocsAi.chat.entity.ConversaEntity;
import br.com.example.senac.businessDocsAi.chat.entity.MensagemEntity;
import br.com.example.senac.businessDocsAi.chat.repository.IConversaRepository;
import br.com.example.senac.businessDocsAi.chat.repository.IMensagemRepository;
import br.com.example.senac.businessDocsAi.document.repository.IRascunhoDocumentoRepository;
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
    private RagAssistant ragAssistantSomenteLeitura;

    @Mock
    private RagAssistant ragAssistantComFerramentas;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                conversaRepository, mensagemRepository, rascunhoRepository,
                ragAssistantSomenteLeitura, ragAssistantComFerramentas,
                currentUserProvider, new ObjectMapper(), new MarkdownConversorService(2000)
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
}
