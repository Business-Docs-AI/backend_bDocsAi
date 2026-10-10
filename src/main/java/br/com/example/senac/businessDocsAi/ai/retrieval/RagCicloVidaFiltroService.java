package br.com.example.senac.businessDocsAi.ai.retrieval;

import br.com.example.senac.businessDocsAi.document.entity.StatusCicloVida;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Etapa 16 (B5) — langchain4j 1.18.0 não tem filtro IS NULL (confirmado: só
 * IsEqualTo/IsNotEqualTo/IsGreaterThan.../IsIn/IsNotIn/ContainsString + And/Or/Not), então a
 * regra "vigente por padrão" (decisão 7/C4) só pode virar um pré-filtro seguro se TODO chunk
 * tiver um valor EFETIVO gravado (nunca omitido — ver IndexacaoService). Esta classe decide,
 * a cada busca, se é seguro aplicar esse pré-filtro: só quando a flag está ligada E não há
 * nenhum chunk órfão (indexado antes da Etapa 14, sem os metadados novos) — senão, o
 * pré-filtro fica desligado automaticamente (a busca funciona como hoje, só com o filtro
 * pós-busca) até a reindexação em massa (Etapa 15) ser rodada. Nunca depende de alguém
 * lembrar de rodar a reindexação antes de ligar a flag.
 */
@Component
public class RagCicloVidaFiltroService {

    private static final Logger log = LoggerFactory.getLogger(RagCicloVidaFiltroService.class);

    // Só status_ciclo_vida — categoria_id é legitimamente nulo pra documento sem categoria
    // (achado em produção, ver IndexacaoService), não um indício de chunk órfão/nunca
    // reindexado desde a Etapa 14. status_ciclo_vida, ao contrário, é SEMPRE gravado desde
    // a Etapa 14/16 (com um valor efetivo, nunca omitido) — null nele é prova confiável de
    // "nunca reindexado desde a Etapa 14".
    private static final String SQL_CHUNKS_SEM_METADADO =
            "SELECT count(*) FROM documento_embedding WHERE status_ciclo_vida IS NULL";

    private final JdbcTemplate jdbcTemplate;
    private final boolean flagHabilitada;
    private final Duration cacheTtl;

    private volatile long chunksSemMetadadoCache = -1;
    private volatile Instant verificadoEm = Instant.EPOCH;

    public RagCicloVidaFiltroService(
            JdbcTemplate jdbcTemplate,
            @Value("${bdocs.rag.filtros-ciclo-vida.enabled}") boolean flagHabilitada,
            @Value("${bdocs.rag.filtros-ciclo-vida.cache-ttl-minutos}") long cacheTtlMinutos
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.flagHabilitada = flagHabilitada;
        this.cacheTtl = Duration.ofMinutes(cacheTtlMinutos);
    }

    /** Filtro pronto pra passar pro pgvector, ou vazio se o pré-filtro não deve ser aplicado
     * agora (flag desligada, ou metadados incompletos). */
    public Optional<Filter> filtroStatusVigente() {
        if (!flagHabilitada) {
            return Optional.empty();
        }

        if (chunksComMetadadoIncompleto() > 0) {
            return Optional.empty();
        }

        return Optional.of(MetadataFilterBuilder.metadataKey("status_ciclo_vida").isEqualTo(StatusCicloVida.VIGENTE.name()));
    }

    /** O valor cru da flag — usado pelo filtro PÓS-busca (lê direto de {@code documento},
     * nunca do metadado do chunk, então não tem o problema de chunk órfão/B5 que o
     * pré-filtro tem; não precisa considerar {@link #chunksComMetadadoIncompleto()}). Com a
     * flag desligada, nem o pré- nem o pós-filtro de ciclo de vida aplicam — "nenhuma
     * mudança" em relação a antes da Etapa 16, pra qualquer papel (ADMIN incluído). */
    public boolean flagHabilitada() {
        return flagHabilitada;
    }

    /** Quantos chunks ainda não têm status_ciclo_vida (nunca reindexados desde a Etapa 14)
     * — exposto também pro endpoint ADMIN de status da reindexação. */
    public long chunksComMetadadoIncompleto() {
        Instant agora = Instant.now();

        if (Duration.between(verificadoEm, agora).compareTo(cacheTtl) >= 0) {
            atualizarCache(agora);
        }

        return chunksSemMetadadoCache;
    }

    private synchronized void atualizarCache(Instant agora) {
        // Outra thread já pode ter atualizado enquanto esta esperava o lock.
        if (Duration.between(verificadoEm, agora).compareTo(cacheTtl) < 0) {
            return;
        }

        Long contagem = jdbcTemplate.queryForObject(SQL_CHUNKS_SEM_METADADO, Long.class);
        chunksSemMetadadoCache = contagem != null ? contagem : 0;
        verificadoEm = agora;

        if (chunksSemMetadadoCache > 0) {
            log.warn(
                    "Pré-filtro de ciclo de vida do RAG DESLIGADO: {} chunk(s) sem "
                            + "status_ciclo_vida (nunca reindexados desde a Etapa 14). "
                            + "Rode POST /admin/reindexacao para corrigir.",
                    chunksSemMetadadoCache
            );
        }
    }
}
