package br.com.example.senac.businessDocsAi.chat.service;

import br.com.example.senac.businessDocsAi.exception.BadRequestException;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

/**
 * Extrai o texto de um documento anexado no chat, para que ele entre na conversa (e,
 * dali, na geração de documentação pela IA) como texto comum — sujeito, inclusive, à mesma
 * conversão para Markdown de textos grandes usada para texto colado direto ({@link
 * MarkdownConversorService}).
 *
 * <p>Tipos suportados nesta primeira versão: texto puro (.txt, .md), HTML (.html, .htm) e
 * PDF (via o mesmo modelo multimodal usado no chat — sem biblioteca de parsing dedicada).
 * Formatos binários como .docx ainda não são suportados.</p>
 */
@Service
@RequiredArgsConstructor
public class AnexoTextoExtractorService {

    private static final Set<String> EXTENSOES_TEXTO_PURO = Set.of("txt", "md");
    private static final Set<String> EXTENSOES_HTML = Set.of("html", "htm");

    private static final String PROMPT_EXTRACAO_PDF = """
            Extraia literalmente todo o texto deste PDF, na ordem em que aparece, sem \
            resumir, comentar ou reformatar — devolva apenas o texto puro contido no \
            documento.""";

    private final ChatModel chatModel;

    public String extrairTexto(MultipartFile anexo) {
        String extensao = extensaoDe(anexo.getOriginalFilename());

        if (EXTENSOES_TEXTO_PURO.contains(extensao)) {
            return lerComoTexto(anexo);
        }

        if (EXTENSOES_HTML.contains(extensao)) {
            return Jsoup.parse(lerComoTexto(anexo)).text();
        }

        if ("pdf".equals(extensao)) {
            return extrairTextoDoPdf(anexo);
        }

        throw new BadRequestException(
                "Tipo de arquivo não suportado: ." + extensao + ". Envie .txt, .md, .html ou .pdf."
        );
    }

    private String lerComoTexto(MultipartFile arquivo) {
        try {
            return new String(arquivo.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo anexado", e);
        }
    }

    private String extrairTextoDoPdf(MultipartFile pdf) {
        try {
            String base64 = Base64.getEncoder().encodeToString(pdf.getBytes());

            UserMessage mensagem = UserMessage.from(
                    TextContent.from(PROMPT_EXTRACAO_PDF),
                    PdfFileContent.from(base64, "application/pdf")
            );

            return chatModel.chat(mensagem).aiMessage().text();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o PDF anexado", e);
        }
    }

    private String extensaoDe(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.contains(".")) {
            return "";
        }
        return nomeArquivo.substring(nomeArquivo.lastIndexOf('.') + 1).toLowerCase();
    }
}
