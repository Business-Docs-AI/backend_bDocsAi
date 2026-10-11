package br.com.example.senac.businessDocsAi.document.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class HtmlSanitizerService {

    // Preserva o atributo "id" nas tags para servir de âncora de seção na indexação/links.
    private static final Safelist SAFELIST = Safelist.relaxed()
            .addAttributes(":all", "id");

    public String sanitize(String html) {
        if (html == null) {
            return "";
        }

        return Jsoup.clean(html, SAFELIST);
    }
}
