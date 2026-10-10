package br.com.example.senac.businessDocsAi.ai.retrieval;

import dev.langchain4j.store.embedding.filter.Filter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Etapa 16 (B5) — langchain4j 1.18.0 não tem filtro IS NULL; esta classe decide, a cada
 * busca, se é seguro aplicar o pré-filtro de status_ciclo_vida (flag ligada E nenhum chunk
 * órfão em documento_embedding).
 */
@ExtendWith(MockitoExtension.class)
class RagCicloVidaFiltroServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    // Com a flag desligada, filtroStatusVigente() curto-circuita ANTES de consultar o banco
    // (nunca precisa saber se os metadados estão completos) — nenhum stub de queryForObject
    // necessário aqui.
    @Test
    void comAFlagDesligadaNuncaAplicaOPreFiltroMesmoComMetadadosCompletos() {
        RagCicloVidaFiltroService service = new RagCicloVidaFiltroService(jdbcTemplate, false, 5);

        assertThat(service.filtroStatusVigente()).isEmpty();
    }

    // Mandatório (Etapa 16): com chunks legados sem metadado, o pré-filtro fica desligado —
    // tanto com a flag ligada quanto desligada. Resultado da busca é o mesmo nos dois casos
    // (PesquisaServiceTest/RagAssistantConfigTest confirmam que "sem filtro" == comportamento
    // de antes da Etapa 16).
    @Test
    void comMetadadosIncompletosNuncaAplicaOPreFiltroIndependenteDaFlag() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(3L);

        RagCicloVidaFiltroService comFlagLigada = new RagCicloVidaFiltroService(jdbcTemplate, true, 5);
        assertThat(comFlagLigada.filtroStatusVigente()).isEmpty();

        RagCicloVidaFiltroService comFlagDesligada = new RagCicloVidaFiltroService(jdbcTemplate, false, 5);
        assertThat(comFlagDesligada.filtroStatusVigente()).isEmpty();
    }

    @Test
    void comAFlagLigadaEMetadadosCompletosAplicaOPreFiltro() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        RagCicloVidaFiltroService service = new RagCicloVidaFiltroService(jdbcTemplate, true, 5);

        Optional<Filter> filtro = service.filtroStatusVigente();

        assertThat(filtro).isPresent();
    }

    @Test
    void chunksComMetadadoIncompletoDevolveAContagemReal() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(7L);
        RagCicloVidaFiltroService service = new RagCicloVidaFiltroService(jdbcTemplate, true, 5);

        assertThat(service.chunksComMetadadoIncompleto()).isEqualTo(7L);
    }

    @Test
    void naoConsultaOBancoDeNovoDentroDoTtlDoCache() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        RagCicloVidaFiltroService service = new RagCicloVidaFiltroService(jdbcTemplate, true, 5);

        service.chunksComMetadadoIncompleto();
        service.chunksComMetadadoIncompleto();
        service.chunksComMetadadoIncompleto();

        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Long.class));
    }

    // TTL=0: toda chamada é tratada como expirada — confirma que o cache de fato reconsulta
    // o banco quando o TTL vence (não fica "preso" num estado antigo pra sempre).
    @Test
    void reconsultaOBancoQuandoOTtlDoCacheJaExpirou() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        RagCicloVidaFiltroService service = new RagCicloVidaFiltroService(jdbcTemplate, true, 0);

        service.chunksComMetadadoIncompleto();
        service.chunksComMetadadoIncompleto();

        verify(jdbcTemplate, times(2)).queryForObject(anyString(), eq(Long.class));
    }

    // Decisão ADMIN/OBSOLETO (2026-10-10): flagHabilitada() é o valor cru — não considera
    // chunksComMetadadoIncompleto(), ao contrário de filtroStatusVigente() — usado pelo
    // filtro PÓS-busca, que lê status direto de `documento` (sem risco de chunk órfão).
    @Test
    void flagHabilitadaDevolveOValorCru() {
        RagCicloVidaFiltroService comFlagLigada = new RagCicloVidaFiltroService(jdbcTemplate, true, 5);
        assertThat(comFlagLigada.flagHabilitada()).isTrue();

        RagCicloVidaFiltroService comFlagDesligada = new RagCicloVidaFiltroService(jdbcTemplate, false, 5);
        assertThat(comFlagDesligada.flagHabilitada()).isFalse();
    }
}
