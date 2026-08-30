package br.com.example.senac.businessDocsAi.categories.controller.repository;

import br.com.example.senac.businessDocsAi.categories.controller.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}