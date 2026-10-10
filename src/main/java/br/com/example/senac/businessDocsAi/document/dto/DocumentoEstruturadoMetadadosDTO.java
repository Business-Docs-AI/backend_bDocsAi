package br.com.example.senac.businessDocsAi.document.dto;

import br.com.example.senac.businessDocsAi.document.entity.Confidencialidade;
import br.com.example.senac.businessDocsAi.document.entity.TipoDocumento;

import java.util.List;
import java.util.UUID;

/**
 * Bloco de metadados extraído de uma proposta de documento estruturado (decisão B3) — a ser
 * aplicado nas colunas de {@link br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity}
 * na confirmação (Etapa 13.3), nunca dentro do {@code conteudo_estruturado} salvo em
 * {@code documento}/{@code documento_versao}. Desacopla {@code DocumentoService} do pacote
 * de DTOs de IA ({@code document.dto.estruturado}).
 */
public record DocumentoEstruturadoMetadadosDTO(
        TipoDocumento tipoDocumento,
        Long macroprocessoId,
        UUID processoPaiId,
        String donoProcesso,
        String aprovador,
        Integer periodicidadeRevisaoMeses,
        Confidencialidade confidencialidade,
        List<String> tags,
        List<Long> areasParticipantes
) {
}
