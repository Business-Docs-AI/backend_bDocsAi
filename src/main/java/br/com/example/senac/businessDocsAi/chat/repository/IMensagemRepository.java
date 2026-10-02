package br.com.example.senac.businessDocsAi.chat.repository;

import br.com.example.senac.businessDocsAi.chat.entity.MensagemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IMensagemRepository extends JpaRepository<MensagemEntity, UUID> {

    List<MensagemEntity> findByConversaIdOrderByCriadoEmAsc(UUID conversaId);

    void deleteByConversaId(UUID conversaId);
}
