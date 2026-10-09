package br.com.example.senac.businessDocsAi.ai.generation;

import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Etapa 13.2 (R5) — com a flag ligada, existem 2 beans de {@link ChatModel}: o {@code
 * chatModel} original ({@code @Primary}, usado pelo chat interativo) e o {@code
 * chatModelGeracaoEstruturada} (dedicado ao worker). Este teste prova que todo ponto do
 * sistema que injeta {@code ChatModel} SEM qualificador continua recebendo o bean original
 * sem ambiguidade — exatamente como antes desta etapa — e que o bean do worker é uma
 * instância DIFERENTE, nunca reaproveitada pelo chat interativo.
 */
@SpringBootTest
@TestPropertySource(properties = "bdocs.documentacao-estruturada.enabled=true")
class ChatModelConfigPrimarioTest {

    // Sem qualificador — é exatamente a mesma forma como RagAssistantConfig (chat
    // interativo) injeta ChatModel hoje. Se @Primary não estivesse correto, este campo
    // falharia a subir o contexto (NoUniqueBeanDefinitionException), não só a asserção.
    @Autowired
    private ChatModel chatModel;

    @Autowired
    @Qualifier("chatModelGeracaoEstruturada")
    private ChatModel chatModelGeracaoEstruturada;

    @Autowired
    private ApplicationContext context;

    @Test
    void chatModelSemQualificadorResolveParaOBeanPrimario() {
        assertThat(context.getBean(ChatModel.class)).isSameAs(chatModel);
    }

    @Test
    void beanDoWorkerEUmaInstanciaDiferenteDoChatInterativo() {
        assertThat(chatModelGeracaoEstruturada).isNotSameAs(chatModel);
    }
}
