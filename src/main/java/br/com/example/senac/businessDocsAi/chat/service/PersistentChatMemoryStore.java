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
import java.util.UUID;

/**
 * Alimenta a janela de memória do LangChain4j a partir do histórico já persistido em
 * chat_mensagem — {@code chat_mensagem} é a fonte da verdade (inclusive das fontes citadas
 * nas respostas, que um ChatMessage puro não carrega); este store só faz a leitura para o
 * prompt. A escrita "de verdade" acontece no ChatService, mensagem a mensagem.
 */
@Component
@RequiredArgsConstructor
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final IMensagemRepository mensagemRepository;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        UUID conversaId = (UUID) memoryId;

        return mensagemRepository.findByConversaIdOrderByCriadoEmAsc(conversaId).stream()
                .map(this::toChatMessage)
                .toList();
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        // Intencionalmente vazio: cada mensagem já é persistida individualmente pelo
        // ChatService (junto com as fontes da resposta), então não há nada extra a
        // sincronizar aqui — este método só existe para satisfazer a interface.
    }

    @Override
    public void deleteMessages(Object memoryId) {
        // Intencionalmente vazio: a exclusão de verdade acontece em
        // ChatService.excluirConversa, na mesma operação que remove a conversa.
    }

    private ChatMessage toChatMessage(MensagemEntity mensagem) {
        if (mensagem.getPapel() == Papel.ASSISTANT) {
            return AiMessage.from(mensagem.getConteudo());
        }
        return UserMessage.from(mensagem.getConteudo());
    }
}
