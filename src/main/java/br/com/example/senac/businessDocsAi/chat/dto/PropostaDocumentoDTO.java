package br.com.example.senac.businessDocsAi.chat.dto;

import java.util.UUID;

/**
 * Proposta de criação/atualização de documento pendente de confirmação nesta conversa,
 * lida diretamente do rascunho ({@code RascunhoDocumentoEntity}) — não é um texto gerado pela
 * IA. Permite ao frontend renderizar o documento proposto, formatado, numa área separada da
 * mensagem de chat (que fica só com o texto conversacional).
 */
public record PropostaDocumentoDTO(
        UUID rascunhoId,
        String tipo,
        UUID documentoIdAlvo,
        Long categoriaId,
        String categoriaNome,
        String titulo,
        String conteudoHtml,
        // Geração assíncrona (Etapa 13.5) — aditivo. "PENDENTE" é o único valor possível
        // pra uma proposta do fluxo legado (sempre foi assim, antes deste campo existir).
        // "GERANDO"/"ERRO_GERACAO" só aparecem pra propostas da tool estruturada (Etapa
        // 13.3). conteudoHtml vem null enquanto status=GERANDO.
        String status,
        String erroGeracao
) {
}
