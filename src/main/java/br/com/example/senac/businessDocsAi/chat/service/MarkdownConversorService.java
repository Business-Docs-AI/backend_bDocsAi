package br.com.example.senac.businessDocsAi.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Quando o usuário cola um texto muito grande no chat (em vez de digitar uma pergunta
 * curta), ele costuma vir sem nenhuma estrutura — um bloco só, com marcadores de lista
 * usando símbolos variados (•, *, -) e sem distinção clara de parágrafos. Convertendo para
 * Markdown antes de persistir, a estrutura original (parágrafos, listas) é preservada em
 * vez de se perder num texto corrido quando a IA ou o front processarem essa mensagem depois.
 */
@Service
public class MarkdownConversorService {

    private static final Pattern MARCADOR_LISTA = Pattern.compile("^[•*▪◦‣·-]\\s+");
    private static final Pattern ITEM_NUMERADO = Pattern.compile("^(\\d+)[.)]\\s+");
    private static final Pattern QUEBRAS_DE_PARAGRAFO = Pattern.compile("\n{2,}");
    private static final Pattern QUEBRA_DE_LINHA = Pattern.compile("\r\n|\r");

    private final int limiteCaracteres;

    public MarkdownConversorService(
            @Value("${app.chat.limite-caracteres-texto-grande}") int limiteCaracteres
    ) {
        this.limiteCaracteres = limiteCaracteres;
    }

    public boolean ehTextoGrande(String texto) {
        return texto != null && texto.length() >= limiteCaracteres;
    }

    // Devolve o texto original quando não é "grande" o suficiente para justificar a
    // conversão; caso contrário, devolve a versão em Markdown.
    public String converterSeNecessario(String texto) {
        if (!ehTextoGrande(texto)) {
            return texto;
        }
        return converterParaMarkdown(texto);
    }

    private String converterParaMarkdown(String texto) {
        String normalizado = QUEBRA_DE_LINHA.matcher(texto).replaceAll("\n");

        return Arrays.stream(QUEBRAS_DE_PARAGRAFO.split(normalizado))
                .map(String::strip)
                .filter(paragrafo -> !paragrafo.isEmpty())
                .map(this::formatarParagrafo)
                .collect(Collectors.joining("\n\n"));
    }

    private String formatarParagrafo(String paragrafo) {
        return Arrays.stream(paragrafo.split("\n"))
                .map(this::formatarLinha)
                .collect(Collectors.joining("\n"));
    }

    private String formatarLinha(String linha) {
        String semEspacos = linha.strip();
        if (semEspacos.isEmpty()) {
            return "";
        }

        Matcher marcador = MARCADOR_LISTA.matcher(semEspacos);
        if (marcador.find()) {
            return "- " + marcador.replaceFirst("");
        }

        Matcher numerado = ITEM_NUMERADO.matcher(semEspacos);
        if (numerado.find()) {
            return numerado.group(1) + ". " + numerado.replaceFirst("");
        }

        return semEspacos;
    }
}
