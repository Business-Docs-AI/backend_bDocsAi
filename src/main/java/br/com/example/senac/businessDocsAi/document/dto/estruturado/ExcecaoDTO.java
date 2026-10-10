package br.com.example.senac.businessDocsAi.document.dto.estruturado;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.NotBlank;

public record ExcecaoDTO(
        @NotBlank(message = "O ID da exceção é obrigatório (ex.: EX-01)")
        @Description("ID curto e único da exceção, ex.: EX-01, EX-02...")
        String id,

        @NotBlank(message = "O gatilho da exceção é obrigatório")
        @Description("O que dispara essa exceção (o que sai do fluxo normal)")
        String gatilho,

        @NotBlank(message = "O tratamento da exceção é obrigatório")
        @Description("Como essa exceção deve ser tratada")
        String tratamento,

        @NotBlank(message = "O papel que aciona o tratamento é obrigatório")
        @Description("PAPEL responsável por tratar essa exceção — nunca o nome de uma pessoa")
        String acionar
) {
}
