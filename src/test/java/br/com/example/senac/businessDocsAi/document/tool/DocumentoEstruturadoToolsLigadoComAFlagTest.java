package br.com.example.senac.businessDocsAi.document.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Etapa 13.3 — com a flag ligada, {@code DocumentoEstruturadoTools} existe como bean. Nunca
 * verifica se a tool está de fato na lista de tools do assistente (AiServices não expõe
 * isso) — a lógica de gate por provedor (decisão A3) é coberta por {@code FeatureFlagsTest}
 * (a função pura) e pelo contexto subir sem erro aqui (prova que a injeção Optional +
 * leitura de app.ai.chat-provider em RagAssistantConfig está correta).
 */
@SpringBootTest
@TestPropertySource(properties = "bdocs.documentacao-estruturada.enabled=true")
class DocumentoEstruturadoToolsLigadoComAFlagTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void beanExisteComAFlagLigada() {
        assertThat(context.getBeansOfType(DocumentoEstruturadoTools.class)).hasSize(1);
    }
}
