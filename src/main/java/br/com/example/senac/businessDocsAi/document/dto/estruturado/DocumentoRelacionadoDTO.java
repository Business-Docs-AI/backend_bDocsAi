package br.com.example.senac.businessDocsAi.document.dto.estruturado;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.NotBlank;

public record DocumentoRelacionadoDTO(
        @NotBlank(message = "O ID do documento relacionado é obrigatório")
        @Description("ID (UUID) de um documento já existente na base, encontrado via buscarDocumentos")
        String documentoId,

        @NotBlank(message = "O tipo de relação é obrigatório")
        @Description("Como este documento se relaciona com o outro, em texto livre (ex.: \"executa a política X\", \"aciona o processo Y\", \"detalhado pela instrução Z\")")
        String tipoRelacao
) {
}
