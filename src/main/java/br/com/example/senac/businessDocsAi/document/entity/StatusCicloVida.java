package br.com.example.senac.businessDocsAi.document.entity;

/**
 * Status de ciclo de vida do CONTEÚDO do documento (política/processo/etc.) — não confundir
 * com {@link StatusIndexacao}, que é sobre a indexação no RAG, nem com
 * {@code StatusRascunho}, que é sobre uma proposta da IA ainda não confirmada.
 *
 * <p>Não existe o valor "RASCUNHO" de propósito, para não colidir semanticamente com
 * {@code documento_rascunho} (proposta pendente de confirmação, algo que ainda nem é um
 * documento de verdade). Fluxo de revisão/aprovação entre os estados fica para uma entrega
 * futura — por ora, documentos novos/atualizados pelo fluxo atual vão direto para
 * {@link #VIGENTE}, preservando o comportamento de hoje.</p>
 */
public enum StatusCicloVida {
    EM_ELABORACAO,
    EM_REVISAO,
    VIGENTE,
    OBSOLETO
}
