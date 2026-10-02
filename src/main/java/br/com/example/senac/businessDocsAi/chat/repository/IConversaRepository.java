package br.com.example.senac.businessDocsAi.chat.repository;

import br.com.example.senac.businessDocsAi.chat.entity.ConversaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IConversaRepository extends JpaRepository<ConversaEntity, UUID> {

    List<ConversaEntity> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    // Ponto único de checagem de posse: se a conversa não é do usuário, para quem
    // consulta ela simplesmente "não existe" (não distinguimos 404 de 403 aqui de propósito).
    Optional<ConversaEntity> findByIdAndUsuarioId(UUID id, Long usuarioId);
}
