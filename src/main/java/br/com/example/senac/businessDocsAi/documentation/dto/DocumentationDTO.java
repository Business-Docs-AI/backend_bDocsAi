package br.com.example.senac.businessDocsAi.documentation.dto;

import lombok.Builder;

@Builder
public record DocumentationDTO(
        Long id,
        String title,
        String content,
        Long categoryId,
        String createdBy
) {
}