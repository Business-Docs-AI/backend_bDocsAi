package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.document.event.GeracaoEstruturadaListener;
import br.com.example.senac.businessDocsAi.document.job.GeracaoEstruturadaJob;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Autowired
    private GeracaoEstruturadaService service;

    @Autowired
    @Qualifier("chatModelGeracaoEstruturada")
    private ChatModel chatModelGeracaoEstruturada;

    @Autowired
    private ChatModel chatModel;

    @Test
    void todosOsBeansDoWorkerExistemComAFlagLigada() {
        assertThat(context.getBeansOfType(GeracaoEstruturadaService.class)).hasSize(1);
        assertThat(context.getBeansOfType(GeracaoEstruturadaListener.class)).hasSize(1);
        assertThat(context.getBeansOfType(GeracaoEstruturadaJob.class)).hasSize(1);
        assertThat(context.containsBean("chatModelGeracaoEstruturada")).isTrue();
    }

    // Prova, em runtime (não só "os beans existem"), que o worker de fato RECEBE o bean
    // dedicado — nunca o @Primary do chat interativo. Motivo de existir este teste: Lombok
    // não copia @Qualifier do campo para o parâmetro do construtor gerado, então a
    // ambiguidade entre os 2 beans de ChatModel dependeria do fallback do Spring por nome
    // (nome do parâmetro == nome do bean) — funciona, mas quebra em silêncio (cai pro
    // @Primary sem erro) se esse nome algum dia divergir. Por isso GeracaoEstruturadaService
    // agora tem um construtor explícito com @Qualifier no parâmetro.
    @Test
    void workerRecebeOBeanDedicadoDeChatModel() {
        ChatModel injetadoNoService = (ChatModel) ReflectionTestUtils.getField(service, "chatModelGeracaoEstruturada");

        assertThat(injetadoNoService).isSameAs(chatModelGeracaoEstruturada);
        assertThat(injetadoNoService).isNotSameAs(chatModel);
    }

    // R3 já tem seu próprio mecanismo de tentativas — as tentativas automáticas do CLIENTE
    // HTTP do langchain4j (default 2) devem ficar desligadas no bean do worker, senão cada
    // tentativa do R3 multiplicaria silenciosamente o custo/latência internamente.
    @Test
    void beanDedicadoTemTentativasHttpDesligadasPorDefault() {
        int maxRetries = (int) ReflectionTestUtils.getField(chatModelGeracaoEstruturada, "maxRetries");

        assertThat(maxRetries).isZero();
    }
}
