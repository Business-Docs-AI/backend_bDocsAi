package br.com.example.senac.businessDocsAi.chat.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioTranscricaoServiceTest {

    @Mock
    private ChatModel chatModel;

    @Test
    void transcreverEnviaOAudioAoModeloEDevolveOTextoDaResposta() {
        AudioTranscricaoService service = new AudioTranscricaoService(chatModel);

        MockMultipartFile audio = new MockMultipartFile(
                "audio", "pergunta.mp3", "audio/mpeg", "bytes-de-audio".getBytes()
        );

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        when(chatModel.chat(captor.capture())).thenReturn(
                ChatResponse.builder().aiMessage(AiMessage.from("Qual é a política de férias?")).build()
        );

        String resultado = service.transcrever(audio);

        assertThat(resultado).isEqualTo("Qual é a política de férias?");

        UserMessage mensagemEnviada = (UserMessage) captor.getValue();
        assertThat(mensagemEnviada.contents()).hasSize(2);
        assertThat(mensagemEnviada.contents().get(0)).isInstanceOf(TextContent.class);
        assertThat(mensagemEnviada.contents().get(1)).isInstanceOf(AudioContent.class);

        AudioContent audioContent = (AudioContent) mensagemEnviada.contents().get(1);
        assertThat(audioContent.audio().mimeType()).isEqualTo("audio/mpeg");
    }
}
