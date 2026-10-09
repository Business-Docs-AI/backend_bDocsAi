package br.com.example.senac.businessDocsAi.document.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Etapa 13.3 — com a flag desligada (default), {@code DocumentoEstruturadoTools} não existe
 * como bean, igual a {@code MacroprocessoTools}.
 */
@SpringBootTest
class DocumentoEstruturadoToolsDesligadoPorDefaultTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void beanNaoExisteComAFlagDesligada() {
        assertThat(context.getBeansOfType(DocumentoEstruturadoTools.class)).isEmpty();
    }
}
