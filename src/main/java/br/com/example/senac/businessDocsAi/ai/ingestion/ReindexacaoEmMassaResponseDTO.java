package br.com.example.senac.businessDocsAi.ai.ingestion;

/** Resposta imediata (202) do disparo da reindexação em massa — o trabalho em si roda em
 * segundo plano; {@code documentosEnfileirados} é a contagem no momento do disparo. */
public record ReindexacaoEmMassaResponseDTO(int documentosEnfileirados, String mensagem) {
}
