package br.com.example.senac.businessDocsAi.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Guarda em disco local o arquivo original (áudio ou documento) enviado numa mensagem de
 * chat — fase dev/local; um ambiente com múltiplas instâncias precisaria de um storage
 * externo (S3 ou equivalente) no lugar disto.
 *
 * <p>Cada conversa tem seu próprio diretório ({@code uploads/chat/<conversaId>/}), o que
 * torna a limpeza ao excluir a conversa trivial: basta remover o diretório inteiro (ver
 * {@link #excluirTudoDaConversa}), em vez de rastrear arquivo por arquivo numa tabela.</p>
 */
@Service
public class ArmazenamentoAnexoService {

    private static final Logger log = LoggerFactory.getLogger(ArmazenamentoAnexoService.class);
    private static final Path RAIZ = Paths.get("uploads", "chat");

    public void salvar(UUID conversaId, MultipartFile arquivo) {
        try {
            Path diretorioConversa = RAIZ.resolve(conversaId.toString());
            Files.createDirectories(diretorioConversa);

            String nomeSanitizado = sanitizarNomeArquivo(arquivo.getOriginalFilename());
            Path destino = diretorioConversa.resolve(UUID.randomUUID() + "-" + nomeSanitizado);

            arquivo.transferTo(destino);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar o arquivo enviado no chat", e);
        }
    }

    public void excluirTudoDaConversa(UUID conversaId) {
        Path diretorioConversa = RAIZ.resolve(conversaId.toString());

        if (!Files.isDirectory(diretorioConversa)) {
            return;
        }

        try (Stream<Path> arquivos = Files.walk(diretorioConversa)) {
            arquivos.sorted(Comparator.reverseOrder()).forEach(this::excluirQuietamente);
        } catch (IOException e) {
            log.warn("Falha ao limpar os arquivos da conversa {}", conversaId, e);
        }
    }

    private void excluirQuietamente(Path caminho) {
        try {
            Files.deleteIfExists(caminho);
        } catch (IOException e) {
            log.warn("Não foi possível remover o arquivo {}", caminho, e);
        }
    }

    // Remove qualquer componente de caminho do nome original (ex.: "../../etc/passwd") e
    // qualquer caractere fora de um conjunto seguro, para que o nome enviado pelo cliente
    // nunca determine onde o arquivo é escrito.
    private String sanitizarNomeArquivo(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "arquivo";
        }

        String semCaminho = Paths.get(nomeOriginal).getFileName().toString();
        return semCaminho.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
