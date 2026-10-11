package br.com.example.senac.businessDocsAi.ai.ingestion;

/** Estado atual do pré-filtro de ciclo de vida do RAG (Etapa 16/B5) — {@code
 * chunksSemMetadado > 0} significa que o pré-filtro está desligado automaticamente até a
 * reindexação em massa (Etapa 15) ser rodada. */
public record ReindexacaoStatusResponseDTO(long chunksSemMetadado, boolean preFiltroCicloVidaAtivo) {
}
