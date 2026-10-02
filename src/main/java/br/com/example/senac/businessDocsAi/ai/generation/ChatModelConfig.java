package br.com.example.senac.businessDocsAi.ai.generation;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O provedor do modelo de chat é escolhido em runtime por configuração
 * ({@code app.ai.chat-provider} / env {@code AI_CHAT_PROVIDER}), sem precisar mexer em
 * código para trocar entre Gemini, Anthropic e OpenAI (ou adicionar outro provedor no
 * futuro). O modelo de embedding é independente disso (ver {@code EmbeddingConfig}).
 */
@Configuration
public class ChatModelConfig {

    @Bean
    public ChatModel chatModel(
            @Value("${app.ai.chat-provider}") String provider,
            @Value("${app.ai.openai-api-key}") String openAiApiKey,
            @Value("${app.ai.openai-chat-model}") String openAiModelName,
            @Value("${app.ai.anthropic-api-key}") String anthropicApiKey,
            @Value("${app.ai.anthropic-chat-model}") String anthropicModelName,
            @Value("${app.ai.gemini-api-key}") String geminiApiKey,
            @Value("${app.ai.gemini-chat-model}") String geminiModelName
    ) {
        return switch (provider.toLowerCase()) {
            case "openai" -> OpenAiChatModel.builder()
                    .apiKey(openAiApiKey)
                    .modelName(openAiModelName)
                    .build();

            case "anthropic" -> AnthropicChatModel.builder()
                    .apiKey(anthropicApiKey)
                    .modelName(anthropicModelName)
                    .build();

            case "gemini" -> GoogleAiGeminiChatModel.builder()
                    .apiKey(geminiApiKey)
                    .modelName(geminiModelName)
                    .build();

            default -> throw new IllegalStateException(
                    "Provedor de chat de IA desconhecido: '" + provider
                            + "' (valores aceitos: 'openai', 'anthropic', 'gemini')"
            );
        };
    }
}
