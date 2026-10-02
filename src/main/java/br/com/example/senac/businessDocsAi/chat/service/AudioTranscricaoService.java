package br.com.example.senac.businessDocsAi.chat.service;

import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;

/**
 * Transcreve áudio enviado no chat usando o mesmo {@link ChatModel} já configurado para o
 * chat de texto (Gemini, por padrão — aceita áudio nativamente como entrada multimodal, sem
 * precisar de um serviço de transcrição dedicado como o Whisper).
 */
@Service
@RequiredArgsConstructor
public class AudioTranscricaoService {

    private static final String PROMPT_TRANSCRICAO = """
            Transcreva literalmente o áudio a seguir para texto, no mesmo idioma em que foi \
            falado. Responda apenas com a transcrição, sem comentários, resumo ou formatação \
            adicional.""";

    private final ChatModel chatModel;

    public String transcrever(MultipartFile audio) {
        byte[] bytes;
        try {
            bytes = audio.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o áudio enviado", e);
        }

        String base64 = Base64.getEncoder().encodeToString(bytes);
        String mimeType = audio.getContentType() != null ? audio.getContentType() : "audio/mpeg";

        UserMessage mensagem = UserMessage.from(
                TextContent.from(PROMPT_TRANSCRICAO),
                AudioContent.from(base64, mimeType)
        );

        return chatModel.chat(mensagem).aiMessage().text();
    }
}
