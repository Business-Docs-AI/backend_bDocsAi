package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.document.event.GeracaoEstruturadaListener;
import br.com.example.senac.businessDocsAi.document.job.GeracaoEstruturadaJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Etapa 13.4 — com a flag desligada (default), nenhuma peça do worker de geração assíncrona
 * existe como bean (nem o ChatModel dedicado da Etapa 13.2).
 */
@SpringBootTest
class GeracaoEstruturadaBeansDesligadoPorDefaultTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void nenhumBeanDoWorkerExisteComAFlagDesligada() {
        assertThat(context.getBeansOfType(GeracaoEstruturadaService.class)).isEmpty();
        assertThat(context.getBeansOfType(GeracaoEstruturadaListener.class)).isEmpty();
        assertThat(context.getBeansOfType(GeracaoEstruturadaJob.class)).isEmpty();
        assertThat(context.containsBean("chatModelGeracaoEstruturada")).isFalse();
    }
}
