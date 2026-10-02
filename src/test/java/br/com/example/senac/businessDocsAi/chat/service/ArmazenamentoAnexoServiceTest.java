package br.com.example.senac.businessDocsAi.chat.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercita I/O de disco real (o serviço escreve sob {@code uploads/chat/<conversaId>/},
 * caminho fixo — ver {@link ArmazenamentoAnexoService}) numa pasta por teste identificada
 * por um UUID novo, removida no {@link #limpar()}.
 */
class ArmazenamentoAnexoServiceTest {

    private final ArmazenamentoAnexoService service = new ArmazenamentoAnexoService();

    private UUID conversaId;

    @AfterEach
    void limpar() throws IOException {
        if (conversaId == null) {
            return;
        }

        Path diretorio = Paths.get("uploads", "chat", conversaId.toString());
        if (!Files.exists(diretorio)) {
            return;
        }

        try (Stream<Path> arquivos = Files.walk(diretorio)) {
            arquivos.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    @Test
    void salvarGravaOArquivoNoDiretorioDaConversa() throws IOException {
        conversaId = UUID.randomUUID();
        MockMultipartFile arquivo = new MockMultipartFile("anexo", "notas.txt", "text/plain", "conteúdo".getBytes());

        service.salvar(conversaId, arquivo);

        Path diretorio = Paths.get("uploads", "chat", conversaId.toString());
        assertThat(Files.isDirectory(diretorio)).isTrue();

        try (Stream<Path> arquivos = Files.list(diretorio)) {
            assertThat(arquivos.count()).isEqualTo(1);
        }
    }

    @Test
    void salvarSanitizaNomeComComponentesDeCaminhoEmbutidos() throws IOException {
        conversaId = UUID.randomUUID();
        MockMultipartFile arquivo = new MockMultipartFile(
                "anexo", "../../etc/passwd", "text/plain", "conteúdo".getBytes()
        );

        service.salvar(conversaId, arquivo);

        Path diretorio = Paths.get("uploads", "chat", conversaId.toString());
        try (Stream<Path> arquivos = Files.list(diretorio)) {
            String nomeGravado = arquivos.findFirst().orElseThrow().getFileName().toString();
            assertThat(nomeGravado).doesNotContain("..").doesNotContain("/").endsWith("passwd");
        }

        assertThat(Files.exists(Paths.get("etc", "passwd"))).isFalse();
    }

    @Test
    void excluirTudoDaConversaRemoveODiretorioInteiro() {
        conversaId = UUID.randomUUID();
        MockMultipartFile arquivo = new MockMultipartFile("anexo", "notas.txt", "text/plain", "conteúdo".getBytes());
        service.salvar(conversaId, arquivo);

        Path diretorio = Paths.get("uploads", "chat", conversaId.toString());
        assertThat(Files.exists(diretorio)).isTrue();

        service.excluirTudoDaConversa(conversaId);

        assertThat(Files.exists(diretorio)).isFalse();
    }

    @Test
    void excluirTudoDaConversaNaoFalhaQuandoDiretorioNaoExiste() {
        conversaId = UUID.randomUUID();

        service.excluirTudoDaConversa(conversaId);
    }
}
