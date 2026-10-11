package br.com.example.senac.businessDocsAi.chat.event;

import br.com.example.senac.businessDocsAi.chat.entity.ConversaEntity;
import br.com.example.senac.businessDocsAi.chat.repository.IConversaRepository;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatTituloListenerTest {

    @Mock
    private IConversaRepository conversaRepository;

    @Mock
    private ChatModel chatModel;

    private ChatTituloListener listener;

    private final UUID conversaId = UUID.randomUUID();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        listener = new ChatTituloListener(conversaRepository, chatModel);
    }

    @Test
    void mensagemCurtaNaoGeraTituloNemConsultaOBanco() {
        listener.aoReceberMensagem(new ConversaMensagemRecebidaEvent(conversaId, "olá"));

        verifyNoInteractions(conversaRepository, chatModel);
    }

    @Test
    void conversaQueJaTemTituloNaoEhSobrescrita() {
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setTitulo("Título já existente");

        when(conversaRepository.findById(conversaId)).thenReturn(Optional.of(conversa));

        listener.aoReceberMensagem(new ConversaMensagemRecebidaEvent(
                conversaId, "Uma mensagem bem longa com assunto real sobre férias e benefícios"
        ));

        verifyNoInteractions(chatModel);
        verify(conversaRepository, never()).save(any());
    }

    @Test
    void conversaInexistenteNaoGeraErro() {
        when(conversaRepository.findById(conversaId)).thenReturn(Optional.empty());

        listener.aoReceberMensagem(new ConversaMensagemRecebidaEvent(
                conversaId, "Uma mensagem bem longa com assunto real sobre férias e benefícios"
        ));

        verifyNoInteractions(chatModel);
        verify(conversaRepository, never()).save(any());
    }

    @Test
    void mensagemLongaComConversaSemTituloGeraEGravaOTitulo() {
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);
        conversa.setTitulo(null);

        when(conversaRepository.findById(conversaId)).thenReturn(Optional.of(conversa));
        when(chatModel.chat(anyString())).thenReturn("Política de Férias");

        listener.aoReceberMensagem(new ConversaMensagemRecebidaEvent(
                conversaId, "Uma mensagem bem longa com assunto real sobre férias e benefícios"
        ));

        ArgumentCaptor<ConversaEntity> captor = ArgumentCaptor.forClass(ConversaEntity.class);
        verify(conversaRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulo()).isEqualTo("Política de Férias");
    }

    @Test
    void respostaDoModeloComAspasTemAsAspasRemovidas() {
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);

        when(conversaRepository.findById(conversaId)).thenReturn(Optional.of(conversa));
        when(chatModel.chat(anyString())).thenReturn("\"Política de Férias\"");

        listener.aoReceberMensagem(new ConversaMensagemRecebidaEvent(
                conversaId, "Uma mensagem bem longa com assunto real sobre férias e benefícios"
        ));

        ArgumentCaptor<ConversaEntity> captor = ArgumentCaptor.forClass(ConversaEntity.class);
        verify(conversaRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulo()).isEqualTo("Política de Férias");
    }

    @Test
    void falhaAoChamarOModeloNaoPropagaExcecaoNemSalva() {
        ConversaEntity conversa = new ConversaEntity();
        conversa.setId(conversaId);

        when(conversaRepository.findById(conversaId)).thenReturn(Optional.of(conversa));
        when(chatModel.chat(anyString())).thenThrow(new RuntimeException("falha simulada"));

        listener.aoReceberMensagem(new ConversaMensagemRecebidaEvent(
                conversaId, "Uma mensagem bem longa com assunto real sobre férias e benefícios"
        ));

        verify(conversaRepository, never()).save(any());
    }
}
