package br.com.example.senac.businessDocsAi.document.dto.estruturado;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.NotBlank;

public record GlossarioEntryDTO(
        @NotBlank(message = "A sigla/termo é obrigatória")
        @Description("Sigla ou termo técnico usado no documento (ex.: \"SLA\")")
        String sigla,

        @NotBlank(message = "O significado é obrigatório")
        @Description("O que essa sigla/termo significa, em linguagem simples")
        String significado
) {
}
