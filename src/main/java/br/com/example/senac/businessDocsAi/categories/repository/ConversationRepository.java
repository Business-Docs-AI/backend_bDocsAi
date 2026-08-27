package br.com.example.senac.businessDocsAi.categories.repository;

import br.com.example.senac.businessDocsAi.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}