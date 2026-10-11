package br.com.example.senac.businessDocsAi.document.dto.estruturado;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Uma linha da matriz RACI — referencia a etapa pelo ID (nunca aninhada), mesma regra das
 * decisões (sem estrutura recursiva).
 */
public record RaciEntryDTO(
        @NotBlank(message = "O ID da etapa da linha RACI é obrigatório")
        @Description("ID da etapa (ex.: E02) a que esta linha da matriz RACI se refere")
        String etapaId,

        @NotBlank(message = "O responsável (R) é obrigatório")
        @Description("PAPEL Responsável (R) por executar a etapa — nunca o nome de uma pessoa")
        String responsavel,

        @NotBlank(message = "O aprovador (A) é obrigatório")
        @Description("PAPEL Aprovador (A) da etapa — nunca o nome de uma pessoa")
        String aprovador,

        @NotNull(message = "A lista de consultados é obrigatória (pode ser vazia)")
        @Description("PAPÉIS Consultados (C) antes da execução — lista vazia se não houver nenhum")
        List<String> consultados,

        @NotNull(message = "A lista de informados é obrigatória (pode ser vazia)")
        @Description("PAPÉIS Informados (I) depois da execução — lista vazia se não houver nenhum")
        List<String> informados
) {
}
