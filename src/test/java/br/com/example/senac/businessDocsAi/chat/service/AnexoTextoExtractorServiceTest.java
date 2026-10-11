package br.com.example.senac.businessDocsAi.chat.service;

import br.com.example.senac.businessDocsAi.exception.BadRequestException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnexoTextoExtractorServiceTest {

    @Mock
    private ChatModel chatModel;

    private AnexoTextoExtractorService extractor;

    @BeforeEach
    void setUp() {
        extractor = new AnexoTextoExtractorService(chatModel);
    }

    @Test
    void arquivoTxtEhLidoDiretamenteComoTexto() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "anexo", "notas.txt", "text/plain", "Conteúdo em texto puro.".getBytes()
        );

        assertThat(extractor.extrairTexto(arquivo)).isEqualTo("Conteúdo em texto puro.");
    }

    @Test
    void arquivoMarkdownEhLidoDiretamenteComoTexto() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "anexo", "notas.md", "text/markdown", "# Título\nConteúdo".getBytes()
        );

        assertThat(extractor.extrairTexto(arquivo)).isEqualTo("# Título\nConteúdo");
    }

    @Test
    void arquivoHtmlTemAsTagsRemovidasRestandoSoOTexto() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "anexo", "pagina.html", "text/html",
                "<html><body><h1>Política de Férias</h1><p>Trinta dias por ano</p></body></html>".getBytes()
        );

        String resultado = extractor.extrairTexto(arquivo);

        assertThat(resultado).contains("Política de Férias").contains("Trinta dias por ano");
        assertThat(resultado).doesNotContain("<h1>").doesNotContain("<p>");
    }

    @Test
    void arquivoPdfUsaOChatModelParaExtrairOTexto() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "anexo", "documento.pdf", "application/pdf", "bytes-de-pdf".getBytes()
        );

        when(chatModel.chat(any(ChatMessage.class))).thenReturn(
                ChatResponse.builder().aiMessage(AiMessage.from("Texto extraído do PDF")).build()
        );

        assertThat(extractor.extrairTexto(arquivo)).isEqualTo("Texto extraído do PDF");
    }

    @Test
    void extensaoNaoSuportadaLancaBadRequest() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "anexo", "planilha.xlsx", "application/vnd.ms-excel", "bytes".getBytes()
        );

        assertThatThrownBy(() -> extractor.extrairTexto(arquivo))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void arquivoSemExtensaoLancaBadRequest() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "anexo", "semextensao", "application/octet-stream", "bytes".getBytes()
        );

        assertThatThrownBy(() -> extractor.extrairTexto(arquivo))
                .isInstanceOf(BadRequestException.class);
    }
}
