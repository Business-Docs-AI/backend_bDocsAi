package br.com.example.senac.businessDocsAi.document.dto.estruturado;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Decisões referenciam a próxima etapa SÓ pelo ID (nunca uma {@code EtapaDTO} aninhada) —
 * o Gemini tem limitações conhecidas com schemas recursivos/muito profundos.
 */
public record DecisaoDTO(
        @NotBlank(message = "A pergunta da decisão é obrigatória")
        @Description("A pergunta/critério que define qual caminho seguir (ex.: \"Pedido aprovado pelo gestor?\")")
        String pergunta,

        @NotNull(message = "As opções da decisão são obrigatórias")
        @Size(min = 2, message = "Uma decisão precisa de pelo menos 2 opções")
        @Valid
        @Description("As opções possíveis de resposta — pelo menos 2, cada uma apontando pro ID da próxima etapa")
        List<DecisaoOpcaoDTO> opcoes
) {
}
