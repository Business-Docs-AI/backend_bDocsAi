package br.com.example.senac.businessDocsAi.chat.event;

import java.util.UUID;

/**
 * Publicado após o commit da persistência da mensagem do usuário, para o título automático
 * da conversa ser gerado em background (ver {@code ChatTituloListener}) sem atrasar a
 * resposta do chat.
 */
public record ConversaMensagemRecebidaEvent(UUID conversaId, String perguntaTexto) {
}
