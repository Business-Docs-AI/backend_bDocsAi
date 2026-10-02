package br.com.example.senac.businessDocsAi.chat.service;

import java.util.UUID;

/**
 * Carrega, na mesma thread da requisição, em qual conversa e em qual "turno" (uma chamada de
 * {@link ChatService#enviarMensagem}) as ferramentas de IA estão operando — sem que o modelo
 * precise (ou consiga) informar esses IDs sozinho. É a base da trava que impede confirmar um
 * rascunho de documento no mesmo turno em que ele foi proposto.
 */
public final class ConversaContextHolder {

    public record Contexto(UUID conversaId, UUID turnoAtual) {
    }

    private static final ThreadLocal<Contexto> CONTEXTO = new ThreadLocal<>();

    private ConversaContextHolder() {
    }

    public static void iniciar(UUID conversaId, UUID turnoAtual) {
        CONTEXTO.set(new Contexto(conversaId, turnoAtual));
    }

    public static Contexto atual() {
        Contexto contexto = CONTEXTO.get();

        if (contexto == null) {
            throw new IllegalStateException(
                    "Nenhum contexto de conversa ativo nesta thread — as ferramentas de "
                            + "documento só podem ser chamadas durante o processamento de uma mensagem de chat."
            );
        }

        return contexto;
    }

    public static void limpar() {
        CONTEXTO.remove();
    }
}
