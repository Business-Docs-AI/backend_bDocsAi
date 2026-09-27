package br.com.example.senac.businessDocsAi.categories.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequestDTO(
        @NotBlank(message = "O nome da categoria é obrigatório")
        String name,
        String description
) {
}