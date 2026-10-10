package br.com.example.senac.businessDocsAi.document.repository;

import br.com.example.senac.businessDocsAi.document.entity.DocumentoAreaParticipanteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IDocumentoAreaParticipanteRepository extends JpaRepository<DocumentoAreaParticipanteEntity, Long> {

    List<DocumentoAreaParticipanteEntity> findByDocumentoId(UUID documentoId);

    void deleteByDocumentoId(UUID documentoId);
}
