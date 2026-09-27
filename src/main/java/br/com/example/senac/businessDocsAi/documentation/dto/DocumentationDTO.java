package br.com.example.senac.businessDocsAi.documentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record DocumentationDTO(
        Long id,

        @NotBlank(message = "O título é obrigatório")
        String title,

        @NotBlank(message = "O conteúdo é obrigatório")
        String content,

        @NotNull(message = "A categoria é obrigatória")
        Long categoryId,

        String createdBy
) {

}