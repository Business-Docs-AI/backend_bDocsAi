package br.com.example.senac.businessDocsAi.document.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerServiceTest {

    private final HtmlSanitizerService sanitizer = new HtmlSanitizerService();

    // P2: o renderizador do documento estruturado (Etapa 9) gera tabelas pra RACI/SIPOC —
    // confirma que a safelist atual (Safelist.relaxed()) já permite essas tags, sem precisar
    // alterar a safelist existente.
    @Test
    void tabelaSobreviveASanitizacao() {
        String html = "<table><thead><tr><th>Coluna</th></tr></thead>"
                + "<tbody><tr><td>Valor</td></tr></tbody></table>";

        String resultado = sanitizer.sanitize(html);

        assertThat(resultado).contains("<table>", "<thead>", "<tbody>", "<tr>", "<th>", "<td>", "Coluna", "Valor");
    }

    @Test
    void listasEStrongSobrevivemASanitizacao() {
        String html = "<ul><li><strong>Item</strong> um</li></ul>";

        String resultado = sanitizer.sanitize(html);

        assertThat(resultado).contains("<ul>", "<li>", "<strong>", "Item", "um");
    }
}
