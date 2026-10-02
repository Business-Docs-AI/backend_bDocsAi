package br.com.example.senac.businessDocsAi.chat.dto;

import br.com.example.senac.businessDocsAi.chat.entity.Papel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MensagemResponseDTO(
        UUID id,
        Papel papel,
        String conteudo,
        List<FonteDTO> fontes,
        LocalDateTime criadoEm
) {
}
