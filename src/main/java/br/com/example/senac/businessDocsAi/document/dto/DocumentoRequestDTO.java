package br.com.example.senac.businessDocsAi.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentoRequestDTO(
        @NotBlank(message = "O título é obrigatório")
        String titulo,

        @NotBlank(message = "O conteúdo é obrigatório")
        String conteudoHtml,

        String comentarioAlteracao,

        @NotNull(message = "A categoria é obrigatória")
        Long categoriaId
) {
}
