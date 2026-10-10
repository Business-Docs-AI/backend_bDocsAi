package br.com.example.senac.businessDocsAi.document.dto.estruturado;

import br.com.example.senac.businessDocsAi.document.entity.Confidencialidade;
import br.com.example.senac.businessDocsAi.document.entity.TipoDocumento;
import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Conteúdo + metadados de uma proposta de documento estruturado (PROCESSO ou PROCEDIMENTO
 * nesta primeira entrega — POLITICA/INSTRUCAO/REGISTRO continuam só no fluxo HTML legado).
 *
 * <p>Separação importante (decisão B3): os campos de CONTEÚDO abaixo (objetivo, escopo,
 * fluxo, regras...) mapeiam 1:1 com o HTML renderizado e são o que vai para a coluna
 * {@code conteudo_estruturado} do documento/versão. Os campos de METADADO (bloco ao final)
 * só alimentam as colunas de {@code documento} (Etapa 2/3/6) — nunca entram no jsonb de
 * conteúdo. Essa separação é aplicada na confirmação (Etapa 13), não aqui — este DTO é só
 * a "proposta completa", como ela chega da IA.</p>
 */
public record DocumentoEstruturadoDTO(

        @NotBlank(message = "O título é obrigatório")
        @Description("Título do documento")
        String titulo,

        @NotNull(message = "O tipo do documento é obrigatório")
        @Description("Tipo do documento — nesta versão, só PROCESSO ou PROCEDIMENTO são aceitos aqui")
        TipoDocumento tipoDocumento,

        @NotBlank(message = "O objetivo é obrigatório")
        @Description("Por que este processo/procedimento existe — o objetivo dele")
        String objetivo,

        @NotNull(message = "O escopo é obrigatório")
        @Valid
        @Description("Delimitação do processo: onde começa, onde termina, o que inclui e o que não inclui")
        EscopoDTO escopo,

        @NotBlank(message = "O gatilho é obrigatório")
        @Description("Evento que inicia o processo/procedimento")
        String gatilho,

        @NotNull(message = "A matriz RACI é obrigatória (pelo menos 1 linha)")
        @Size(min = 1, message = "A matriz RACI precisa de pelo menos 1 linha")
        @Valid
        @Description("Matriz RACI — quem é Responsável, Aprovador, Consultado e Informado em cada etapa")
        List<RaciEntryDTO> raci,

        @NotNull(message = "O fluxo é obrigatório (pelo menos 1 etapa)")
        @Size(min = 1, message = "O fluxo precisa de pelo menos 1 etapa")
        @Valid
        @Description("As etapas atômicas do processo/procedimento, em ordem, cada uma com ID próprio")
        List<EtapaDTO> fluxo,

        @Valid
        @Description("SIPOC (Fornecedores, Entradas, Saídas, Clientes) — opcional, null se não fizer sentido pro material fornecido")
        SipocDTO sipoc,

        @NotNull(message = "A lista de regras de negócio é obrigatória (pode ser vazia)")
        @Valid
        @Description("Regras de negócio atômicas aplicáveis, cada uma com ID próprio — lista vazia se não houver nenhuma, nunca texto genérico")
        List<RegraNegocioDTO> regrasNegocio,

        @NotNull(message = "A lista de exceções é obrigatória (pode ser vazia)")
        @Valid
        @Description("Exceções ao fluxo normal, cada uma com ID próprio — lista vazia se não houver nenhuma")
        List<ExcecaoDTO> excecoes,

        @NotNull(message = "A lista de sistemas/ferramentas é obrigatória (pode ser vazia)")
        @Description("Sistemas ou ferramentas usados no processo/procedimento — lista vazia se não houver")
        List<String> sistemasFerramentas,

        @NotNull(message = "A lista de riscos e controles é obrigatória (pode ser vazia)")
        @Description("Riscos identificados e os controles que os mitigam — lista vazia se não houver")
        List<String> riscosControles,

        @NotNull(message = "A lista de indicadores é obrigatória (pode ser vazia)")
        @Description("Indicadores do processo (SLA, tempo médio, volume, taxa de erro, retrabalho) — lista vazia se não houver")
        List<String> indicadores,

        @NotNull(message = "O glossário é obrigatório (pode ser vazio)")
        @Valid
        @Description("Siglas e termos técnicos usados no documento, com o significado — lista vazia se não houver nenhuma sigla")
        List<GlossarioEntryDTO> glossario,

        @NotNull(message = "A lista de documentos relacionados é obrigatória (pode ser vazia)")
        @Valid
        @Description("Outros documentos já existentes na base relacionados a este — lista vazia se não houver nenhum")
        List<DocumentoRelacionadoDTO> documentosRelacionados,

        @NotNull(message = "A lista de pendências é obrigatória (pode ser vazia)")
        @Description("Perguntas/informações que faltaram no material fornecido e precisam ser respondidas pelo usuário — lista vazia se nada faltou")
        List<String> pendencias,

        // --- Bloco de metadados (decisão C2a) — NUNCA entram no conteúdo estruturado
        // salvo, só alimentam as colunas de documento na confirmação (Etapa 13). ---

        @Description("ID do macroprocesso (obtido via listarMacroprocessos), se algum servir — null se nenhum servir (registre em pendencias)")
        Long macroprocessoId,

        @Description("ID (UUID, como texto) do documento que é o processo PAI deste, se houver hierarquia")
        String processoPaiId,

        @NotNull(message = "A lista de áreas participantes é obrigatória (pode ser vazia)")
        @Description("IDs de categoria das áreas que participam deste processo além da área dona — lista vazia se só a área dona participar")
        List<Long> areasParticipantes,

        @Description("PAPEL dono do processo — nunca o nome de uma pessoa")
        String donoProcesso,

        @Description("PAPEL aprovador deste documento — nunca o nome de uma pessoa")
        String aprovador,

        @Description("Periodicidade de revisão deste documento, em meses (ex.: 12 = revisão anual)")
        Integer periodicidadeRevisaoMeses,

        @Description("Confidencialidade do documento: PUBLICO, INTERNO, CONFIDENCIAL ou RESTRITO")
        Confidencialidade confidencialidade,

        @NotNull(message = "A lista de tags é obrigatória (pode ser vazia)")
        @Description("Palavras-chave livres pra facilitar a busca — lista vazia se não houver nenhuma")
        List<String> tags
) {
    // Versão do schema gravada em documento/documento_versao/documento_rascunho.versao_schema
    // (Etapa 13) — permite evoluir o formato do conteúdo estruturado no futuro sem quebrar
    // documentos já salvos com uma versão anterior.
    public static final String VERSAO_SCHEMA_ATUAL = "1.0";
}
