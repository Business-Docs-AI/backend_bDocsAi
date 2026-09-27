package br.com.example.senac.businessDocsAi.categories.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequestDTO(

        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must have at most 100 characters")
        String name,

        @Size(max = 500, message = "Category description must have at most 500 characters")
        String description
) {
}