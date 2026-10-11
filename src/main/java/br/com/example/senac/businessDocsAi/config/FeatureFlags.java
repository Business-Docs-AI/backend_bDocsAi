package br.com.example.senac.businessDocsAi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Feature flags da documentação estruturada (pirâmide ISO 9001 / SIPOC / RACI / APQC).
 * Lidas só no startup — os beans de {@code AiServices} que vão depender delas
 * ({@code RagAssistantConfig}) são montados uma vez; mudar a flag em runtime não tem
 * efeito até reiniciar a aplicação.
 *
 * <p>Com {@code documentacaoEstruturadaHabilitada()} em false (default), nada do fluxo
 * novo existe em tempo de execução — o sistema se comporta exatamente como antes desta
 * entrega.</p>
 */
@Component
public class FeatureFlags {

    private final boolean documentacaoEstruturadaHabilitada;
    private final List<String> documentacaoEstruturadaProvedores;

    public FeatureFlags(
            @Value("${bdocs.documentacao-estruturada.enabled:false}") boolean documentacaoEstruturadaHabilitada,
            @Value("${bdocs.documentacao-estruturada.provedores:}") String documentacaoEstruturadaProvedoresCsv
    ) {
        this.documentacaoEstruturadaHabilitada = documentacaoEstruturadaHabilitada;
        this.documentacaoEstruturadaProvedores = documentacaoEstruturadaProvedoresCsv == null
                || documentacaoEstruturadaProvedoresCsv.isBlank()
                ? List.of()
                : Arrays.stream(documentacaoEstruturadaProvedoresCsv.split(","))
                        .map(String::trim)
                        .filter(provedor -> !provedor.isBlank())
                        .toList();
    }

    public boolean documentacaoEstruturadaHabilitada() {
        return documentacaoEstruturadaHabilitada;
    }

    /**
     * true só quando a flag geral está ligada E o provedor informado está na lista de
     * provedores habilitados para o caminho estruturado (ver propriedade
     * {@code bdocs.documentacao-estruturada.provedores} — decisão A3: nunca fixo no código).
     */
    public boolean documentacaoEstruturadaHabilitadaPara(String provedor) {
        return documentacaoEstruturadaHabilitada
                && provedor != null
                && documentacaoEstruturadaProvedores.stream().anyMatch(p -> p.equalsIgnoreCase(provedor));
    }
}
