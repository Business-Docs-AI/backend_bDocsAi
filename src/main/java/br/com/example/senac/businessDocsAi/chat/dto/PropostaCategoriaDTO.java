package br.com.example.senac.businessDocsAi.chat.dto;

/**
 * Proposta de criação de categoria pendente de confirmação nesta conversa, lida diretamente
 * do rascunho ({@code CategoriaRascunhoEntity}) — ver {@link PropostaDocumentoDTO}.
 */
public record PropostaCategoriaDTO(
        Long rascunhoId,
        String nome,
        String descricao
) {
}
