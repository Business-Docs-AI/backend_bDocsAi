package br.com.example.senac.businessDocsAi.conversation.repository;

import br.com.example.senac.businessDocsAi.conversation.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IConversationRepository extends JpaRepository<ConversationEntity, Long> {
}
