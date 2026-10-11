package br.com.example.senac.businessDocsAi.chat.dto;

/**
 * Estado atual de propostas pendentes de uma conversa (no máximo uma de cada tipo por vez).
 * Usado pelo frontend ao reabrir uma conversa, para restaurar o que está pendente de
 * confirmação sem precisar reenviar uma mensagem — ver também os mesmos campos, já populados
 * a cada envio de mensagem, em {@link MensagemResponseDTO}.
 */
public record RascunhoPendenteResponseDTO(
        PropostaDocumentoDTO propostaDocumento,
        PropostaCategoriaDTO propostaCategoria
) {
}
