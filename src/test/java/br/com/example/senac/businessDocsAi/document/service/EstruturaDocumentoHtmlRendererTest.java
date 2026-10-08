package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.ai.ingestion.HtmlSectionSplitter;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTOBeanValidationTest;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.EtapaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EstruturaDocumentoHtmlRendererTest {

    private EstruturaDocumentoHtmlRenderer renderer;
    private HtmlSectionSplitter splitter;

    @BeforeEach
    void setUp() {
        renderer = new EstruturaDocumentoHtmlRenderer(new HtmlSanitizerService());
        splitter = new HtmlSectionSplitter();
    }

    @Test
    void idaEVoltaProduzUmChunkPorItemAtomico() {
        DocumentoEstruturadoDTO dto = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder().build();

        String html = renderer.renderizar(dto);
        List<HtmlSectionSplitter.Secao> secoes = splitter.dividir(dto.titulo(), html);

        List<String> titulos = secoes.stream().map(HtmlSectionSplitter.Secao::titulo).toList();

        assertThat(titulos).contains("Objetivo", "Escopo", "Gatilho", "Matriz RACI");
        // Cada etapa do fluxo é seu próprio chunk, com o ID no título da seção.
        assertThat(titulos).anyMatch(t -> t.contains("E01") && t.contains("Receber pedido"));
        assertThat(titulos).anyMatch(t -> t.contains("E02") && t.contains("Validar pedido"));
        // A regra de negócio da fixture também é seu próprio chunk.
        assertThat(titulos).anyMatch(t -> t.contains("RN-01"));
    }

    @Test
    void secoesOpcionaisAusentesNaoGeramChunkVazio() {
        // Fixture padrão tem sipoc=null, excecoes/sistemas/riscos/indicadores/glossario/
        // documentosRelacionados vazios — nenhum desses headers deveria aparecer.
        DocumentoEstruturadoDTO dto = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder().build();

        String html = renderer.renderizar(dto);
        List<String> titulos = splitter.dividir(dto.titulo(), html).stream()
                .map(HtmlSectionSplitter.Secao::titulo)
                .toList();

        assertThat(titulos).doesNotContain("SIPOC", "Exceções", "Sistemas e Ferramentas", "Glossário");
    }

    @Test
    void textoDoLlmComTagsNuncaViraHtmlExecutavelNemSecaoFalsa() {
        EtapaDTO etapaComInjecao = new EtapaDTO(
                "E01", "<h2>Secao Falsa Injetada</h2>", "<script>alert(1)</script>", "Papel", null,
                List.of(), List.of(), List.of(), null, null
        );
        DocumentoEstruturadoDTO dto = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder()
                .fluxo(List.of(etapaComInjecao))
                .raci(List.of(new br.com.example.senac.businessDocsAi.document.dto.estruturado.RaciEntryDTO(
                        "E01", "Papel", "Papel2", List.of(), List.of()
                )))
                .build();

        String html = renderer.renderizar(dto);

        // Nunca aparece como tag executável/real.
        assertThat(html).doesNotContain("<script>");

        // E não cria uma seção "Secao Falsa Injetada" de verdade no splitter — o texto
        // malicioso fica só como conteúdo de texto dentro da seção "E01 — ...", nunca como
        // um heading real que o splitter interpretaria como novo limite de seção.
        List<String> titulos = splitter.dividir(dto.titulo(), html).stream()
                .map(HtmlSectionSplitter.Secao::titulo)
                .toList();
        assertThat(titulos).doesNotContain("Secao Falsa Injetada");
    }
}
