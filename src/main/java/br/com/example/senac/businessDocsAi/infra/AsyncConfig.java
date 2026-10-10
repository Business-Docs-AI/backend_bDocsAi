package br.com.example.senac.businessDocsAi.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "indexacaoExecutor")
    public Executor indexacaoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("indexacao-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "tituloExecutor")
    public Executor tituloExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("titulo-conversa-");
        executor.initialize();
        return executor;
    }

    // Etapa 13.4: pool dedicado pro worker de geração estruturada — nunca compartilhado com
    // indexação/título, porque cada geração chama a Anthropic e pode levar ~130s (Etapa
    // 11b); um pool pequeno evita gastar tokens/custo em paralelo demais por engano.
    @Bean(name = "geracaoEstruturadaExecutor")
    public Executor geracaoEstruturadaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("geracao-estruturada-");
        executor.initialize();
        return executor;
    }

    // Etapa 15: pool dedicado e PEQUENO (nunca o indexacaoExecutor compartilhado) — uma
    // reindexação em massa pode varrer centenas de documentos; rodar num pool próprio evita
    // que ela entupa a fila da indexação normal (criar/editar um documento) atrás de si, e
    // limita quantos embeddings locais (CPU-bound, ONNX) rodam em paralelo de uma vez.
    @Bean(name = "reindexacaoEmMassaExecutor")
    public Executor reindexacaoEmMassaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("reindexacao-massa-");
        executor.initialize();
        return executor;
    }
}
