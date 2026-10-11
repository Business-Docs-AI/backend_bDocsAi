package br.com.example.senac.businessDocsAi.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversaResponseDTO(
        UUID id,
        String titulo,
        LocalDateTime criadoEm
) {
}
