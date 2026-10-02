package br.com.example.senac.businessDocsAi.document.repository;

import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusRascunho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IRascunhoDocumentoRepository extends JpaRepository<RascunhoDocumentoEntity, UUID> {

    Optional<RascunhoDocumentoEntity> findFirstByConversaIdAndStatusOrderByCriadoEmDesc(
            UUID conversaId, StatusRascunho status
    );

    void deleteByConversaId(UUID conversaId);
}
