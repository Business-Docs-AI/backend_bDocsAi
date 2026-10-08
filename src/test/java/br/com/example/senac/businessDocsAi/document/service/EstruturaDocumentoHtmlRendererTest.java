package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.ai.ingestion.HtmlSectionSplitter;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTOBeanValidationTest;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.EtapaDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.RaciEntryDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.SipocDTO;
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
                .raci(List.of(new RaciEntryDTO("E01", "Papel", "Papel2", List.of(), List.of())))
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

    // P2: RACI é renderizado como <table> e SIPOC como <ul> — confirma que a safelist atual
    // (Safelist.relaxed(), sem alteração) preserva tabela e todo o conteúdo depois de
    // renderizar() já ter passado pelo HtmlSanitizerService internamente.
    @Test
    void raciETemSipocSobrevivemAoRenderizarESanitizar() {
        DocumentoEstruturadoDTO dto = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder()
                .raci(List.of(new RaciEntryDTO(
                        "E01", "Atendente", "Supervisor", List.of("Financeiro"), List.of("Cliente")
                )))
                .sipoc(new SipocDTO(
                        List.of("Fornecedor X"), List.of("Pedido"), List.of("Nota Fiscal"), List.of("Cliente final")
                ))
                .build();

        String html = renderer.renderizar(dto);

        // A tabela do RACI, com cabeçalho e os valores das colunas, sobrevive à sanitização.
        assertThat(html)
                .contains("<table>", "<thead>", "<tbody>", "<th>", "<td>")
                .contains("Atendente", "Supervisor", "Financeiro", "Cliente");

        // SIPOC (listas) e todo o conteúdo também sobrevivem.
        assertThat(html).contains("SIPOC", "Fornecedor X", "Pedido", "Nota Fiscal", "Cliente final");

        // E o splitter ainda consegue extrair as seções normalmente depois da sanitização.
        List<String> titulos = splitter.dividir(dto.titulo(), html).stream()
                .map(HtmlSectionSplitter.Secao::titulo)
                .toList();
        assertThat(titulos).contains("Matriz RACI", "SIPOC");
    }
}
