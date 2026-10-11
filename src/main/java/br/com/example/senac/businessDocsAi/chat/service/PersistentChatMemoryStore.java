package br.com.example.senac.businessDocsAi.chat.service;

import br.com.example.senac.businessDocsAi.chat.entity.MensagemEntity;
import br.com.example.senac.businessDocsAi.chat.entity.Papel;
import br.com.example.senac.businessDocsAi.chat.repository.IMensagemRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alimenta a janela de memória do LangChain4j a partir do histórico já persistido em
 * chat_mensagem — {@code chat_mensagem} é a fonte da verdade entre turnos (inclusive das
 * fontes citadas nas respostas, que um ChatMessage puro não carrega).
 *
 * <p>Mas DENTRO de um único turno, o laço de tool-calling do AiServices chama
 * {@code chatMemoryProvider.get(memoryId)} de novo a cada rodada — e cada chamada recria a
 * {@code MessageWindowChatMemory}, que se popula via {@link #getMessages}. Se esse método só
 * lesse {@code chat_mensagem} (que só ganha a mensagem final depois que o turno inteiro
 * termina, em {@code ChatService}), o modelo veria o mesmo contexto em toda rodada — sem
 * nenhum registro de ter chamado uma ferramenta antes — e repetiria a mesma chamada
 * indefinidamente (visto na prática: {@code input_tokens} idêntico em toda rodada, até
 * estourar o limite de 100 round trips do langchain4j). Por isso este cache em memória:
 * {@link #updateMessages} grava o que o AiServices acumulou durante o turno (incluindo as
 * chamadas/resultados de ferramenta), e {@link #getMessages} prioriza esse cache sobre o
 * banco enquanto o turno estiver em andamento. {@link ChatService} invalida o cache
 * (ver {@link #invalidar}) depois de persistir a mensagem final do turno, para o próximo
 * turno recomeçar a partir do {@code chat_mensagem} já atualizado (e não arrastar pra sempre
 * o "ruído" intermediário das chamadas de ferramenta).</p>
 */
@Component
@RequiredArgsConstructor
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final IMensagemRepository mensagemRepository;

    private final Map<UUID, List<ChatMessage>> cacheDoTurno = new ConcurrentHashMap<>();

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        UUID conversaId = (UUID) memoryId;

        List<ChatMessage> emAndamento = cacheDoTurno.get(conversaId);
        if (emAndamento != null) {
            return emAndamento;
        }

        return mensagemRepository.findByConversaIdOrderByCriadoEmAsc(conversaId).stream()
                .map(this::toChatMessage)
                .toList();
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        cacheDoTurno.put((UUID) memoryId, messages);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        cacheDoTurno.remove((UUID) memoryId);
    }

    // Chamado pelo ChatService ao final de cada turno (mensagem final já persistida em
    // chat_mensagem) e na exclusão da conversa — sem isso, o próximo turno herdaria pra
    // sempre o historico intermediário de chamadas de ferramenta deste turno.
    public void invalidar(UUID conversaId) {
        cacheDoTurno.remove(conversaId);
    }

    private ChatMessage toChatMessage(MensagemEntity mensagem) {
        if (mensagem.getPapel() == Papel.ASSISTANT) {
            return AiMessage.from(mensagem.getConteudo());
        }
        return UserMessage.from(mensagem.getConteudo());
    }
}
