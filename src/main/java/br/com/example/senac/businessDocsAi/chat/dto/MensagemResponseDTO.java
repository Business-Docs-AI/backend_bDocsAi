package br.com.example.senac.businessDocsAi.chat.dto;

import br.com.example.senac.businessDocsAi.chat.entity.Papel;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MensagemResponseDTO(
        UUID id,
        Papel papel,
        String conteudo,
        List<FonteDTO> fontes,
        // Populados somente na resposta de um envio de mensagem (enviarMensagem/
        // enviarMensagemComArquivo), nunca no histórico devolvido por buscarConversa — para
        // o estado pendente de uma conversa já aberta, ver GET /chat/conversas/{id}/rascunho-pendente.
        PropostaDocumentoDTO propostaDocumento,
        PropostaCategoriaDTO propostaCategoria,
        DocumentoResponseDTO documentoConfirmado,
        LocalDateTime criadoEm
) {
}
