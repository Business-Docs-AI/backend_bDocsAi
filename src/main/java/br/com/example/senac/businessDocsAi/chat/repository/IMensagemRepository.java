package br.com.example.senac.businessDocsAi.chat.repository;

import br.com.example.senac.businessDocsAi.chat.entity.MensagemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IMensagemRepository extends JpaRepository<MensagemEntity, UUID> {

    List<MensagemEntity> findByConversaIdOrderByCriadoEmAsc(UUID conversaId);

    // Geração assíncrona (R4): material-fonte do worker é o histórico PERSISTIDO até o
    // momento da solicitação (não a janela de memória do chat) — exclui qualquer mensagem
    // de um turno POSTERIOR ao da solicitação.
    List<MensagemEntity> findByConversaIdAndCriadoEmLessThanEqualOrderByCriadoEmAsc(
            UUID conversaId, LocalDateTime ate
    );

    void deleteByConversaId(UUID conversaId);
}
