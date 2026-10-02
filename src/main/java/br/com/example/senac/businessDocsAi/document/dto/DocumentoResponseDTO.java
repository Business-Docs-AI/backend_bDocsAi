package br.com.example.senac.businessDocsAi.document.dto;

import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentoResponseDTO(
        UUID id,
        String titulo,
        String conteudoHtml,
        int versaoAtual,
        StatusIndexacao statusIndexacao,
        String criadoPor,
        LocalDateTime criadoEm,
        String atualizadoPor,
        LocalDateTime atualizadoEm,
        Long categoriaId,
        String categoriaNome
) {
}
