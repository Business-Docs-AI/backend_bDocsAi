package br.com.example.senac.businessDocsAi.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureFlagsTest {

    @Test
    void semPropriedadeNenhumaFicaDesligadaESemProvedores() {
        FeatureFlags flags = new FeatureFlags(false, "");

        assertThat(flags.documentacaoEstruturadaHabilitada()).isFalse();
        assertThat(flags.documentacaoEstruturadaHabilitadaPara("gemini")).isFalse();
    }

    @Test
    void ligadaMasSemProvedorNaListaNaoHabilitaNenhumProvedor() {
        FeatureFlags flags = new FeatureFlags(true, "");

        assertThat(flags.documentacaoEstruturadaHabilitada()).isTrue();
        assertThat(flags.documentacaoEstruturadaHabilitadaPara("gemini")).isFalse();
    }

    @Test
    void parseiaListaDeProvedoresSeparadaPorVirgulaIgnorandoEspacosEMaiusculas() {
        FeatureFlags flags = new FeatureFlags(true, "gemini, Anthropic");

        assertThat(flags.documentacaoEstruturadaHabilitadaPara("gemini")).isTrue();
        assertThat(flags.documentacaoEstruturadaHabilitadaPara("anthropic")).isTrue();
        assertThat(flags.documentacaoEstruturadaHabilitadaPara("openai")).isFalse();
    }

    @Test
    void provedorNuloNuncaHabilita() {
        FeatureFlags flags = new FeatureFlags(true, "gemini,anthropic");

        assertThat(flags.documentacaoEstruturadaHabilitadaPara(null)).isFalse();
    }
}
