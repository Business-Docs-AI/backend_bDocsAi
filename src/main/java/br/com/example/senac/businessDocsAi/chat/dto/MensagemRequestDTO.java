package br.com.example.senac.businessDocsAi.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record MensagemRequestDTO(
        @NotBlank(message = "A pergunta é obrigatória")
        String pergunta
) {
}
