package br.com.example.senac.businessDocsAi.chat.repository;

import br.com.example.senac.businessDocsAi.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
}
