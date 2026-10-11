package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.document.dto.estruturado.DecisaoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DecisaoOpcaoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoRelacionadoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.EscopoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.EtapaDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.ExcecaoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.GlossarioEntryDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.RaciEntryDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.RegraNegocioDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.SipocDTO;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Entities;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Renderiza {@link DocumentoEstruturadoDTO} em HTML determinístico (sempre a mesma entrada
 * produz a mesma saída — nada de IA aqui). Duas regras importantes:
 *
 * <ul>
 *   <li>Todo texto que vem do LLM é inserido como TEXTO escapado ({@link Entities#escape(String)}),
 *   nunca como HTML cru — um "objetivo" contendo {@code <script>} nunca se torna uma tag,
 *   fica literalmente visível como texto. Depois disso o resultado inteiro ainda passa pelo
 *   {@link HtmlSanitizerService} normal, como qualquer outro conteúdo.</li>
 *   <li>Cada etapa/regra/exceção vira um {@code <h3>} com o ID no próprio texto (ex.:
 *   "RN-01 — ..."), de propósito: o {@code HtmlSectionSplitter} já corta por h1/h2/h3 sem
 *   nenhuma mudança nele, então cada item atômico já nasce como um chunk próprio na
 *   indexação.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class EstruturaDocumentoHtmlRenderer {

    private final HtmlSanitizerService htmlSanitizerService;

    public String renderizar(DocumentoEstruturadoDTO dto) {
        StringBuilder html = new StringBuilder();

        html.append(h2("Objetivo")).append(p(dto.objetivo()));
        html.append(renderizarEscopo(dto.escopo()));
        html.append(h2("Gatilho")).append(p(dto.gatilho()));
        html.append(renderizarRaci(dto.raci()));
        html.append(renderizarFluxo(dto.fluxo()));

        if (dto.sipoc() != null) {
            html.append(renderizarSipoc(dto.sipoc()));
        }
        if (!dto.regrasNegocio().isEmpty()) {
            html.append(renderizarRegras(dto.regrasNegocio()));
        }
        if (!dto.excecoes().isEmpty()) {
            html.append(renderizarExcecoes(dto.excecoes()));
        }
        if (!dto.sistemasFerramentas().isEmpty()) {
            html.append(h2("Sistemas e Ferramentas")).append(ul(dto.sistemasFerramentas()));
        }
        if (!dto.riscosControles().isEmpty()) {
            html.append(h2("Riscos e Controles")).append(ul(dto.riscosControles()));
        }
        if (!dto.indicadores().isEmpty()) {
            html.append(h2("Indicadores")).append(ul(dto.indicadores()));
        }
        if (!dto.glossario().isEmpty()) {
            html.append(renderizarGlossario(dto.glossario()));
        }
        if (!dto.documentosRelacionados().isEmpty()) {
            html.append(renderizarDocumentosRelacionados(dto.documentosRelacionados()));
        }

        return htmlSanitizerService.sanitize(html.toString());
    }

    private String renderizarEscopo(EscopoDTO escopo) {
        StringBuilder sb = new StringBuilder();
        sb.append(h2("Escopo"));
        sb.append("<p><strong>Início:</strong> ").append(escape(escopo.inicio())).append("</p>");
        sb.append("<p><strong>Fim:</strong> ").append(escape(escopo.fim())).append("</p>");
        if (!escopo.inclui().isEmpty()) {
            sb.append("<p><strong>Inclui:</strong></p>").append(ul(escopo.inclui()));
        }
        if (!escopo.naoInclui().isEmpty()) {
            sb.append("<p><strong>Não inclui:</strong></p>").append(ul(escopo.naoInclui()));
        }
        return sb.toString();
    }

    private String renderizarRaci(List<RaciEntryDTO> raci) {
        StringBuilder sb = new StringBuilder();
        sb.append(h2("Matriz RACI"));
        sb.append("<table><thead><tr><th>Etapa</th><th>Responsável (R)</th><th>Aprovador (A)</th>"
                + "<th>Consultados (C)</th><th>Informados (I)</th></tr></thead><tbody>");
        for (RaciEntryDTO linha : raci) {
            sb.append("<tr><td>").append(escape(linha.etapaId())).append("</td><td>")
                    .append(escape(linha.responsavel())).append("</td><td>")
                    .append(escape(linha.aprovador())).append("</td><td>")
                    .append(escape(String.join(", ", linha.consultados()))).append("</td><td>")
                    .append(escape(String.join(", ", linha.informados()))).append("</td></tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String renderizarFluxo(List<EtapaDTO> fluxo) {
        StringBuilder sb = new StringBuilder();
        sb.append(h2("Fluxo"));
        for (EtapaDTO etapa : fluxo) {
            sb.append(h3(etapa.id(), etapa.nome()));
            sb.append(p(etapa.descricao()));
            sb.append("<p><strong>Responsável:</strong> ").append(escape(etapa.responsavel())).append("</p>");
            if (etapa.sistema() != null && !etapa.sistema().isBlank()) {
                sb.append("<p><strong>Sistema:</strong> ").append(escape(etapa.sistema())).append("</p>");
            }
            if (!etapa.entradas().isEmpty()) {
                sb.append("<p><strong>Entradas:</strong></p>").append(ul(etapa.entradas()));
            }
            if (!etapa.saidas().isEmpty()) {
                sb.append("<p><strong>Saídas:</strong></p>").append(ul(etapa.saidas()));
            }
            if (!etapa.regrasAplicaveis().isEmpty()) {
                sb.append("<p><strong>Regras aplicáveis:</strong> ")
                        .append(escape(String.join(", ", etapa.regrasAplicaveis()))).append("</p>");
            }
            if (etapa.decisao() != null) {
                sb.append(renderizarDecisao(etapa.decisao()));
            } else if (etapa.proximaEtapaId() != null) {
                sb.append("<p><strong>Próxima etapa:</strong> ").append(escape(etapa.proximaEtapaId())).append("</p>");
            }
        }
        return sb.toString();
    }

    private String renderizarDecisao(DecisaoDTO decisao) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p><strong>Decisão:</strong> ").append(escape(decisao.pergunta())).append("</p>");
        sb.append("<ul>");
        for (DecisaoOpcaoDTO opcao : decisao.opcoes()) {
            sb.append("<li>").append(escape(opcao.resposta())).append(" → ")
                    .append(escape(opcao.proximaEtapaId())).append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private String renderizarSipoc(SipocDTO sipoc) {
        StringBuilder sb = new StringBuilder();
        sb.append(h2("SIPOC"));
        sb.append("<p><strong>Fornecedores:</strong></p>").append(ul(sipoc.fornecedores()));
        sb.append("<p><strong>Entradas:</strong></p>").append(ul(sipoc.entradas()));
        sb.append("<p><strong>Saídas:</strong></p>").append(ul(sipoc.saidas()));
        sb.append("<p><strong>Clientes:</strong></p>").append(ul(sipoc.clientes()));
        return sb.toString();
    }

    private String renderizarRegras(List<RegraNegocioDTO> regras) {
        StringBuilder sb = new StringBuilder();
        sb.append(h2("Regras de Negócio"));
        for (RegraNegocioDTO regra : regras) {
            sb.append(h3(regra.id(), regra.descricao()));
            sb.append("<p><strong>Tipo:</strong> ").append(escape(regra.tipo().name())).append("</p>");
            if (regra.fonte() != null && !regra.fonte().isBlank()) {
                sb.append("<p><strong>Fonte:</strong> ").append(escape(regra.fonte())).append("</p>");
            }
        }
        return sb.toString();
    }

    private String renderizarExcecoes(List<ExcecaoDTO> excecoes) {
        StringBuilder sb = new StringBuilder();
        sb.append(h2("Exceções"));
        for (ExcecaoDTO excecao : excecoes) {
            sb.append(h3(excecao.id(), excecao.gatilho()));
            sb.append("<p><strong>Tratamento:</strong> ").append(escape(excecao.tratamento())).append("</p>");
            sb.append("<p><strong>Acionar:</strong> ").append(escape(excecao.acionar())).append("</p>");
        }
        return sb.toString();
    }

    private String renderizarGlossario(List<GlossarioEntryDTO> glossario) {
        StringBuilder sb = new StringBuilder();
        sb.append(h2("Glossário"));
        sb.append("<ul>");
        for (GlossarioEntryDTO entrada : glossario) {
            sb.append("<li><strong>").append(escape(entrada.sigla())).append(":</strong> ")
                    .append(escape(entrada.significado())).append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private String renderizarDocumentosRelacionados(List<DocumentoRelacionadoDTO> relacionados) {
        StringBuilder sb = new StringBuilder();
        sb.append(h2("Documentos Relacionados"));
        sb.append("<ul>");
        for (DocumentoRelacionadoDTO relacionado : relacionados) {
            sb.append("<li>").append(escape(relacionado.tipoRelacao()))
                    .append(" (").append(escape(relacionado.documentoId())).append(")</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private String h2(String texto) {
        return "<h2>" + escape(texto) + "</h2>";
    }

    // ID no próprio texto do heading (ex.: "RN-01 — ...") — decisão da Etapa 9: assim o
    // HtmlSectionSplitter já gera um chunk por item atômico, sem precisar mudar o splitter.
    private String h3(String id, String titulo) {
        return "<h3>" + escape(id + " — " + titulo) + "</h3>";
    }

    private String p(String texto) {
        return "<p>" + escape(texto) + "</p>";
    }

    private String ul(List<String> itens) {
        StringBuilder sb = new StringBuilder("<ul>");
        for (String item : itens) {
            sb.append("<li>").append(escape(item)).append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private String escape(String texto) {
        return texto == null ? "" : Entities.escape(texto);
    }
}
