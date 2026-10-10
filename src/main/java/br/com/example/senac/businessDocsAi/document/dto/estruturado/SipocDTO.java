package br.com.example.senac.businessDocsAi.document.dto.estruturado;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SipocDTO(
        @NotNull(message = "A lista de fornecedores é obrigatória (pode ser vazia)")
        @Description("Fornecedores (Suppliers) do processo — lista vazia se não houver")
        List<String> fornecedores,

        @NotNull(message = "A lista de entradas é obrigatória (pode ser vazia)")
        @Description("Entradas (Inputs) do processo — lista vazia se não houver")
        List<String> entradas,

        @NotNull(message = "A lista de saídas é obrigatória (pode ser vazia)")
        @Description("Saídas (Outputs) do processo — lista vazia se não houver")
        List<String> saidas,

        @NotNull(message = "A lista de clientes é obrigatória (pode ser vazia)")
        @Description("Clientes (Customers) do processo — lista vazia se não houver")
        List<String> clientes
) {
}
