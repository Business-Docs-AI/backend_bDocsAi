package br.com.example.senac.businessDocsAi.ai.ingestion;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Divide o HTML do documento em seções respeitando os limites de h1–h3, extraindo texto
 * limpo (nunca HTML cru) para servir de base à indexação semântica.
 */
@Component
public class HtmlSectionSplitter {

    private static final Set<String> TAGS_DE_SECAO = Set.of("h1", "h2", "h3");

    public List<Secao> dividir(String tituloDocumento, String html) {

        Document document = Jsoup.parse(html == null ? "" : html);

        List<Secao> secoes = new ArrayList<>();
        StringBuilder textoAtual = new StringBuilder();
        String[] tituloAtual = {tituloDocumento};
        String[] ancoraAtual = {"documento"};

        document.body().traverse((node, depth) -> {
            if (isTituloDeSecao(node)) {
                fecharSecaoAtual(secoes, tituloAtual[0], ancoraAtual[0], textoAtual);

                Element heading = (Element) node;
                tituloAtual[0] = heading.text();
                ancoraAtual[0] = heading.hasAttr("id") ? heading.attr("id") : gerarAncora(heading.text());

            } else if (node instanceof TextNode textNode) {
                String texto = textNode.text();
                if (!texto.isBlank()) {
                    textoAtual.append(texto).append(' ');
                }
            }
        });

        fecharSecaoAtual(secoes, tituloAtual[0], ancoraAtual[0], textoAtual);

        return secoes;
    }

    private boolean isTituloDeSecao(Node node) {
        return node instanceof Element element && TAGS_DE_SECAO.contains(element.tagName());
    }

    private void fecharSecaoAtual(List<Secao> secoes, String titulo, String ancora, StringBuilder texto) {
        String conteudo = texto.toString().trim().replaceAll("\\s+", " ");

        if (titulo != null && !conteudo.isBlank()) {
            secoes.add(new Secao(ancora, titulo, conteudo));
        }

        texto.setLength(0);
    }

    private String gerarAncora(String titulo) {
        // Remove acentos (NFD decompõe "ç"/"ã" em letra base + marca combinante, que
        // então é descartada) para não gerar âncoras truncadas como "se-o-sem-id".
        String semAcentos = Normalizer.normalize(titulo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String base = semAcentos.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");

        return base.isBlank() ? UUID.randomUUID().toString() : base;
    }

    public record Secao(String ancora, String titulo, String texto) {
    }
}
