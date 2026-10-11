package br.com.example.senac.businessDocsAi.chat.dto;

import java.util.UUID;

public record FonteDTO(
        UUID documentoId,
        String titulo,
        String secao,
        String link
) {
}
