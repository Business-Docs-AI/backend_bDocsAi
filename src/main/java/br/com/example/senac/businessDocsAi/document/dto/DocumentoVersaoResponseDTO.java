package br.com.example.senac.businessDocsAi.document.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentoVersaoResponseDTO(
        UUID id,
        int numeroVersao,
        String titulo,
        String conteudoHtml,
        String autor,
        LocalDateTime criadoEm,
        String comentarioAlteracao
) {
}
