package br.com.example.senac.businessDocsAi.ai.generation;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * O provedor do modelo de chat é escolhido em runtime por configuração
 * ({@code app.ai.chat-provider} / env {@code AI_CHAT_PROVIDER}), sem precisar mexer em
 * código para trocar entre Gemini, Anthropic e OpenAI (ou adicionar outro provedor no
 * futuro). O modelo de embedding é independente disso (ver {@code EmbeddingConfig}).
 */
@Configuration
public class ChatModelConfig {

    // @Primary (Etapa 13.2/R5): a partir desta etapa existe um segundo bean de ChatModel
    // (chatModelGeracaoEstruturada, abaixo), dedicado ao worker de geração assíncrona. Sem
    // @Primary, qualquer injeção de ChatModel sem qualificador (todo o resto do sistema —
    // RagAssistantConfig etc.) ficaria ambígua. @Primary garante que absolutamente nada além
    // do worker muda de comportamento: todo o resto continua recebendo exatamente este bean,
    // com a mesma config de sempre.
    @Primary
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

            // Sem maxTokens, o SDK usa um default baixo (1024). Numa resposta com várias
            // chamadas de ferramenta, a cota pode se esgotar antes de gerar texto algum,
            // cortando a resposta com stop_reason=max_tokens e nenhum conteúdo de texto
            // (confirmado em produção: ChatService precisou de um fallback só pra esse
            // caso). 8192 dá folga de sobra pras respostas deste chat interativo.
            //
            // CORREÇÃO (Etapa 11/13.2): este comentário atribuía o risco ao "thinking"
            // consumir a cota — mas thinking estendido está DESLIGADO aqui (não há
            // .thinkingType(...) configurado), confirmado por medição direta na
            // investigação da Etapa 11. O esgotamento da cota, quando acontece, vem do
            // próprio conteúdo da resposta/chamada de ferramenta, não de thinking.
            //
            // cacheSystemMessages/cacheTools: o system prompt (RagSystemPrompt, extenso) e
            // as specs das ferramentas (DocumentoTools/CategoriaTools) são IDÊNTICOS em toda
            // chamada de uma mesma conversa — sem cache, a Anthropic reprocessa esse bloco
            // grande do zero em cada ida-e-volta do laço de tool-calling (confirmado:
            // cache_read_input_tokens=0 em produção antes desta mudança). Com cache habilitado,
            // só a primeira chamada de cada conversa paga o custo de prefill completo; as
            // seguintes leem do cache da Anthropic — reduz latência por chamada justamente nos
            // fluxos com mais round trips (criar/confirmar documento ou categoria).
            case "anthropic" -> AnthropicChatModel.builder()
                    .apiKey(anthropicApiKey)
                    .modelName(anthropicModelName)
                    .maxTokens(8192)
                    .cacheSystemMessages(true)
                    .cacheTools(true)
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

    // Etapa 13.2: ChatModel DEDICADO ao worker de geração assíncrona do documento
    // estruturado — nunca usado pelo chat interativo (ver @Primary acima). Sempre Anthropic,
    // independente de app.ai.chat-provider: é o único provedor validado para esse fluxo nas
    // Etapas 10/11/11b (Gemini quebra em qualquer tool-calling via AiServices — ver plano,
    // "Problema separado: Gemini"). maxTokens/timeout bem maiores que o bean do chat
    // interativo, validados na Etapa 11b (um documento estruturado completo precisa de bem
    // mais que 8192 tokens de saída, e a latência real medida passou de 200s em alguns
    // casos). Só existe com a flag ligada —@ConditionalOnProperty, mesmo padrão de
    // MacroprocessoTools — para não gastar/validar uma chave da Anthropic quando a feature
    // está desligada.
    //
    // SEM logRequests/logResponses: isso dumparia o conteúdo completo do documento gerado
    // nos logs — o worker (Etapa 13.4) loga só tokens de entrada/saída e latência por
    // geração, nunca o conteúdo (R3).
    @Bean(name = "chatModelGeracaoEstruturada")
    @ConditionalOnProperty(prefix = "bdocs.documentacao-estruturada", name = "enabled", havingValue = "true")
    public ChatModel chatModelGeracaoEstruturada(
            @Value("${app.ai.anthropic-api-key}") String anthropicApiKey,
            @Value("${app.ai.anthropic-chat-model}") String anthropicModelName,
            @Value("${bdocs.documentacao-estruturada.geracao.max-tokens}") int maxTokens,
            @Value("${bdocs.documentacao-estruturada.geracao.timeout-segundos}") int timeoutSegundos
    ) {
        return AnthropicChatModel.builder()
                .apiKey(anthropicApiKey)
                .modelName(anthropicModelName)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSegundos))
                .build();
    }
}
