package br.com.example.senac.businessDocsAi.document.dto;

import java.util.List;
import java.util.UUID;

public record ResultadoBuscaDTO(
        UUID documentoId,
        String titulo,
        double melhorScore,
        List<TrechoDTO> trechos
) {
}
