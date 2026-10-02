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

            // Sem maxTokens, o SDK usa um default baixo (1024) — e o "thinking" do Claude
            // Sonnet entra nessa mesma cota. Numa resposta com várias chamadas de
            // ferramenta, o thinking sozinho pode consumir a cota inteira antes de gerar
            // qualquer texto, cortando a resposta com stop_reason=max_tokens e nenhum
            // conteúdo de texto (confirmado em produção: ChatService precisou de um
            // fallback só pra esse caso). 8192 dá folga de sobra pro thinking + a resposta.
            case "anthropic" -> AnthropicChatModel.builder()
                    .apiKey(anthropicApiKey)
                    .modelName(anthropicModelName)
                    .maxTokens(8192)
                    .logRequests(true)
                    .logResponses(true)
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
