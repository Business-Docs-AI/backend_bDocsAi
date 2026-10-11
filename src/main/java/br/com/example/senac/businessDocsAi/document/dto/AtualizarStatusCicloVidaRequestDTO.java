package br.com.example.senac.businessDocsAi.document.dto;

import br.com.example.senac.businessDocsAi.document.entity.StatusCicloVida;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusCicloVidaRequestDTO(
        @NotNull(message = "O novo status é obrigatório")
        StatusCicloVida novoStatus
) {
}
