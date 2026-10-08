package br.com.example.senac.businessDocsAi.document.dto.estruturado;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.NotBlank;

public record DecisaoOpcaoDTO(
        @NotBlank(message = "A resposta da opção é obrigatória")
        @Description("Uma resposta possível à pergunta da decisão (ex.: \"Aprovado\", \"Rejeitado\")")
        String resposta,

        @NotBlank(message = "O ID da próxima etapa da opção é obrigatório")
        @Description("ID (ex.: E04) da etapa seguinte quando esta resposta for escolhida — nunca uma etapa aninhada, só o ID")
        String proximaEtapaId
) {
}
