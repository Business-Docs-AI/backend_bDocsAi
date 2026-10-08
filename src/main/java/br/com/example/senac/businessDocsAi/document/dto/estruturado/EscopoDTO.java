package br.com.example.senac.businessDocsAi.document.dto.estruturado;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EscopoDTO(
        @NotBlank(message = "O início do escopo é obrigatório")
        @Description("Evento ou condição que marca o INÍCIO do processo/procedimento")
        String inicio,

        @NotBlank(message = "O fim do escopo é obrigatório")
        @Description("Evento ou condição que marca o FIM do processo/procedimento")
        String fim,

        @NotNull(message = "A lista \"inclui\" é obrigatória (pode ser vazia)")
        @Description("O que ESTÁ dentro do escopo — lista vazia se não houver nada a destacar, nunca texto genérico")
        List<String> inclui,

        @NotNull(message = "A lista \"não inclui\" é obrigatória (pode ser vazia)")
        @Description("O que NÃO está dentro do escopo — lista vazia se não houver nada a destacar, nunca texto genérico")
        List<String> naoInclui
) {
}
