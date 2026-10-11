package br.com.example.senac.businessDocsAi.document.dto;

import jakarta.validation.constraints.NotBlank;

public record MacroprocessoRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        String descricao
) {
}
