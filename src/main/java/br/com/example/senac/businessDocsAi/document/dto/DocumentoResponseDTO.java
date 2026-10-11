package br.com.example.senac.businessDocsAi.document.dto;

import br.com.example.senac.businessDocsAi.document.entity.Confidencialidade;
import br.com.example.senac.businessDocsAi.document.entity.StatusCicloVida;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import br.com.example.senac.businessDocsAi.document.entity.TipoDocumento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DocumentoResponseDTO(
        UUID id,
        String titulo,
        String conteudoHtml,
        int versaoAtual,
        StatusIndexacao statusIndexacao,
        String criadoPor,
        LocalDateTime criadoEm,
        String atualizadoPor,
        LocalDateTime atualizadoEm,
        Long categoriaId,
        String categoriaNome,
        // Metadados de processo/governança (Etapa 2) — todos opcionais, aditivos.
        TipoDocumento tipoDocumento,
        StatusCicloVida statusCicloVida,
        String donoProcesso,
        String aprovador,
        LocalDate dataVigencia,
        LocalDate proximaRevisao,
        Integer periodicidadeRevisaoMeses,
        Confidencialidade confidencialidade,
        List<String> tags,
        // Hierarquia de processo (Etapa 3) — processoPaiId vem null se o pai estiver
        // soft-deletado (nunca expõe um vínculo "fantasma").
        Long macroprocessoId,
        UUID processoPaiId
) {
    // Construtor de compatibilidade com a assinatura anterior (sem os metadados da Etapa 2)
    // — evita reescrever todo call site existente que ainda não precisa desses campos. Novo
    // código (produção) deve preferir o construtor canônico, com os valores reais.
    public DocumentoResponseDTO(
            UUID id,
            String titulo,
            String conteudoHtml,
            int versaoAtual,
            StatusIndexacao statusIndexacao,
            String criadoPor,
            LocalDateTime criadoEm,
            String atualizadoPor,
            LocalDateTime atualizadoEm,
            Long categoriaId,
            String categoriaNome
    ) {
        this(
                id, titulo, conteudoHtml, versaoAtual, statusIndexacao, criadoPor, criadoEm, atualizadoPor,
                atualizadoEm, categoriaId, categoriaNome,
                null, null, null, null, null, null, null, null, null,
                null, null
        );
    }
}
