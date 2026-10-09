package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.document.event.GeracaoEstruturadaListener;
import br.com.example.senac.businessDocsAi.document.job.GeracaoEstruturadaJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Etapa 13.4 — com a flag ligada, todas as peças do worker existem como bean e o contexto
 * sobe sem erro (prova a fiação: ChatModel dedicado + Validator + beanValidator + renderer +
 * ObjectMapper + os 2 repositórios, mais o listener/job que dependem do service).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "bdocs.documentacao-estruturada.enabled=true",
        "app.ai.anthropic-api-key=fake-key-so-para-passar-da-validacao-de-construcao"
})
class GeracaoEstruturadaBeansLigadoComAFlagTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void todosOsBeansDoWorkerExistemComAFlagLigada() {
        assertThat(context.getBeansOfType(GeracaoEstruturadaService.class)).hasSize(1);
        assertThat(context.getBeansOfType(GeracaoEstruturadaListener.class)).hasSize(1);
        assertThat(context.getBeansOfType(GeracaoEstruturadaJob.class)).hasSize(1);
        assertThat(context.containsBean("chatModelGeracaoEstruturada")).isTrue();
    }
}
