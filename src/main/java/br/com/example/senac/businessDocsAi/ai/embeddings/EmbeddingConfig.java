package br.com.example.senac.businessDocsAi.ai.embeddings;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.pgvector.DefaultMetadataStorageConfig;
import dev.langchain4j.store.embedding.pgvector.MetadataStorageMode;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

/**
 * O provedor de embedding é escolhido em runtime ({@code app.ai.embedding-provider} / env
 * {@code AI_EMBEDDING_PROVIDER}): "local" (padrão) roda o modelo all-MiniLM-L6-v2
 * quantizado em processo via ONNX — sem chave, sem chamada de rede — e "openai" usa a API
 * da OpenAI. A dimensão do vetor gravado no pgvector muda de acordo com o provedor (384
 * para o local, 1536 por padrão para o `text-embedding-3-small` da OpenAI).
 */
@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${app.ai.embedding-provider}") String provider,
            @Value("${app.ai.openai-api-key}") String openAiApiKey,
            @Value("${app.ai.embedding-model}") String openAiModelName
    ) {
        return switch (provider.toLowerCase()) {
            case "local" -> new AllMiniLmL6V2QuantizedEmbeddingModel();

            case "openai" -> OpenAiEmbeddingModel.builder()
                    .apiKey(openAiApiKey)
                    .modelName(openAiModelName)
                    .build();

            default -> throw new IllegalStateException(
                    "Provedor de embedding desconhecido: '" + provider
                            + "' (valores aceitos: 'local', 'openai')"
            );
        };
    }

    // Reaproveita o DataSource/pool já gerenciado pelo Spring (o mesmo do JPA) em vez de
    // abrir uma segunda conexão com host/porta/credenciais duplicadas.
    @Bean
    public PgVectorEmbeddingStore embeddingStore(
            DataSource dataSource,
            @Value("${app.ai.embedding-provider}") String provider,
            @Value("${app.ai.embedding-dimension}") int openAiDimension,
            @Value("${app.ai.local-embedding-dimension}") int localDimension
    ) {
        int dimension = "local".equalsIgnoreCase(provider) ? localDimension : openAiDimension;

        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("documento_embedding")
                .dimension(dimension)
                .createTable(true)
                // Sem índice ivfflat de propósito: com poucas linhas (documentação interna
                // de uma empresa não chega nem perto de precisar de busca aproximada), um
                // índice ivfflat com "lists" mal dimensionado para o volume de dados faz a
                // busca por vizinho mais próximo retornar resultados incompletos/errados de
                // forma não-determinística (confirmado na prática: com só 2 chunks e
                // lists=100, o retriever chegou a não encontrar nada). Scan sequencial é
                // exato e rápido o suficiente até a base crescer bem além do realista aqui;
                // se um dia isso virar gargalo, reavaliar com HNSW ou um "lists" calculado
                // a partir da contagem real de linhas.
                .useIndex(false)
                .metadataStorageConfig(DefaultMetadataStorageConfig.builder()
                        .storageMode(MetadataStorageMode.COLUMN_PER_KEY)
                        .columnDefinitions(List.of(
                                "documento_id uuid",
                                "versao integer",
                                "titulo text",
                                "secao text"
                        ))
                        .indexes(List.of("documento_id"))
                        .build())
                .build();
    }
}
