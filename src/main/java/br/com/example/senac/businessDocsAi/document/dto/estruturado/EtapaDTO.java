package br.com.example.senac.businessDocsAi.document.dto.estruturado;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Etapa atômica do fluxo — uma ação por item (decisão 6, regra 4). Referencia regras de
 * negócio pelos IDs ({@code regrasAplicaveis}), nunca embutindo o texto delas.
 *
 * <p>{@code proximaEtapaId} e {@code decisao} são mutuamente exclusivos: uma etapa segue
 * direto pra próxima (id fixo) OU ramifica numa decisão com ≥2 opções — nunca os dois
 * juntos (validado em {@code DocumentoEstruturadoValidator}). A última etapa do fluxo não
 * tem nenhum dos dois.</p>
 */
public record EtapaDTO(
        @NotBlank(message = "O ID da etapa é obrigatório (ex.: E01)")
        @Description("ID curto e único da etapa, ex.: E01, E02...")
        String id,

        @NotBlank(message = "O nome da etapa é obrigatório")
        @Description("Nome da etapa, no imperativo e em frase curta (ex.: \"Validar o pedido\")")
        String nome,

        @NotBlank(message = "A descrição da etapa é obrigatória")
        @Description("O que acontece nesta etapa, em detalhe")
        String descricao,

        @NotBlank(message = "O responsável da etapa é obrigatório")
        @Description("PAPEL responsável pela etapa (ex.: \"Analista Financeiro\") — nunca o nome de uma pessoa")
        String responsavel,

        @Description("Sistema ou ferramenta usada nesta etapa, se houver")
        String sistema,

        @NotNull(message = "A lista de entradas é obrigatória (pode ser vazia)")
        @Description("O que esta etapa recebe/precisa pra executar — lista vazia se não houver nada a destacar")
        List<String> entradas,

        @NotNull(message = "A lista de saídas é obrigatória (pode ser vazia)")
        @Description("O que esta etapa produz/entrega — lista vazia se não houver nada a destacar")
        List<String> saidas,

        @NotNull(message = "A lista de regras aplicáveis é obrigatória (pode ser vazia)")
        @Description("IDs (ex.: RN-01) das regras de negócio que valem pra esta etapa — nunca o texto da regra, só o ID")
        List<String> regrasAplicaveis,

        @Description("ID (ex.: E04) da próxima etapa, quando o fluxo só segue direto (sem decisão). Null se esta etapa tiver uma decisão, ou se for a última do fluxo.")
        String proximaEtapaId,

        @Valid
        @Description("Decisão que ramifica o fluxo a partir desta etapa, quando houver. Null se o fluxo seguir direto (proximaEtapaId) ou se for a última etapa.")
        DecisaoDTO decisao
) {
}
