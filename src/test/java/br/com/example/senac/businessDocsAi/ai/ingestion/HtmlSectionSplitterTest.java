package br.com.example.senac.businessDocsAi.ai.ingestion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSectionSplitterTest {

    private final HtmlSectionSplitter splitter = new HtmlSectionSplitter();

    @Test
    void deveDividirPorTitulosH1AH3() {
        String html = """
                <h1 id="intro">Introdução</h1>
                <p>Texto da introdução.</p>
                <h2 id="detalhes">Detalhes</h2>
                <p>Texto dos detalhes.</p>
                """;

        List<HtmlSectionSplitter.Secao> secoes = splitter.dividir("Documento X", html);

        assertThat(secoes).hasSize(2);
        assertThat(secoes.get(0).ancora()).isEqualTo("intro");
        assertThat(secoes.get(0).titulo()).isEqualTo("Introdução");
        assertThat(secoes.get(0).texto()).contains("Texto da introdução.");

        assertThat(secoes.get(1).ancora()).isEqualTo("detalhes");
        assertThat(secoes.get(1).texto()).contains("Texto dos detalhes.");
    }

    @Test
    void deveGerarAncoraQuandoTituloNaoTemId() {
        String html = "<h2>Seção Sem Id</h2><p>Conteúdo</p>";

        List<HtmlSectionSplitter.Secao> secoes = splitter.dividir("Documento X", html);

        assertThat(secoes).hasSize(1);
        assertThat(secoes.get(0).ancora()).isEqualTo("secao-sem-id");
    }

    @Test
    void conteudoAntesDoPrimeiroTituloUsaTituloDoDocumento() {
        String html = "<p>Conteúdo introdutório sem cabeçalho.</p><h2>Seção</h2><p>Resto</p>";

        List<HtmlSectionSplitter.Secao> secoes = splitter.dividir("Documento X", html);

        assertThat(secoes).hasSize(2);
        assertThat(secoes.get(0).titulo()).isEqualTo("Documento X");
        assertThat(secoes.get(0).ancora()).isEqualTo("documento");
        assertThat(secoes.get(0).texto()).contains("Conteúdo introdutório sem cabeçalho.");
    }

    @Test
    void documentoSemNenhumTituloViraUmaUnicaSecao() {
        String html = "<p>Só um parágrafo, sem cabeçalhos.</p>";

        List<HtmlSectionSplitter.Secao> secoes = splitter.dividir("Documento X", html);

        assertThat(secoes).hasSize(1);
        assertThat(secoes.get(0).titulo()).isEqualTo("Documento X");
        assertThat(secoes.get(0).texto()).contains("Só um parágrafo, sem cabeçalhos.");
    }
}
