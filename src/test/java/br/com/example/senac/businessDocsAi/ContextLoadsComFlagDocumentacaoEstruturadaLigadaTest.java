package br.com.example.senac.businessDocsAi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Com a flag ligada, MacroprocessoTools passa a existir e é injetado como
 * Optional&lt;MacroprocessoTools&gt; em RagAssistantConfig.ragAssistantComFerramentas — este
 * teste garante que o contexto inteiro ainda sobe sem erro nesse caminho (o
 * BusinessDocsAiApplicationTests comum só cobre o caminho com a flag desligada, Optional.empty()).
 */
@SpringBootTest
@TestPropertySource(properties = "bdocs.documentacao-estruturada.enabled=true")
class ContextLoadsComFlagDocumentacaoEstruturadaLigadaTest {

    @Test
    void contextLoadsComAFlagLigada() {
    }
}
