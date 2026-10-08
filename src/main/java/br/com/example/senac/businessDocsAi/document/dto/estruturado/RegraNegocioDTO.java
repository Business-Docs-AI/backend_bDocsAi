package br.com.example.senac.businessDocsAi.document.dto.estruturado;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegraNegocioDTO(
        @NotBlank(message = "O ID da regra é obrigatório (ex.: RN-01)")
        @Description("ID curto e único da regra, ex.: RN-01, RN-02...")
        String id,

        @NotBlank(message = "A descrição da regra é obrigatória")
        @Description("O que a regra determina, em uma frase objetiva")
        String descricao,

        @NotNull(message = "O tipo da regra é obrigatório")
        @Description("Tipo da regra: CRITERIO, RESTRICAO, APROVACAO, PRAZO ou CALCULO")
        TipoRegraNegocio tipo,

        @Description("De onde vem essa regra (política, norma, acordo, etc.), se souber")
        String fonte
) {
}
