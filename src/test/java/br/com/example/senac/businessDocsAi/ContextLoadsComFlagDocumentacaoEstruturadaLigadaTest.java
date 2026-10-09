package br.com.example.senac.businessDocsAi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Com a flag ligada, MacroprocessoTools passa a existir e é injetado como
 * Optional&lt;MacroprocessoTools&gt; em RagAssistantConfig.ragAssistantComFerramentas — este
 * teste garante que o contexto inteiro ainda sobe sem erro nesse caminho (o
 * BusinessDocsAiApplicationTests comum só cobre o caminho com a flag desligada, Optional.empty()).
 *
 * <p>Etapa 13.2: a flag ligada também cria o bean {@code chatModelGeracaoEstruturada}
 * (AnthropicChatModel), cujo builder valida eagerly que apiKey não é branco — por isso a
 * chave fake abaixo (nunca usada pra chamar a API de verdade neste teste, só pra passar da
 * validação de construção). Sem isso, este teste quebra em qualquer ambiente sem uma
 * ANTHROPIC_API_KEY real configurada (ex.: CI).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "bdocs.documentacao-estruturada.enabled=true",
        "app.ai.anthropic-api-key=fake-key-so-para-passar-da-validacao-de-construcao"
})
class ContextLoadsComFlagDocumentacaoEstruturadaLigadaTest {

    @Test
    void contextLoadsComAFlagLigada() {
    }
}
