package br.com.example.senac.businessDocsAi.document.repository;

import br.com.example.senac.businessDocsAi.document.entity.DocumentoVersaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IDocumentoVersaoRepository extends JpaRepository<DocumentoVersaoEntity, UUID> {

    List<DocumentoVersaoEntity> findByDocumentoIdOrderByNumeroVersaoDesc(UUID documentoId);

    Optional<DocumentoVersaoEntity> findByDocumentoIdAndNumeroVersao(UUID documentoId, int numeroVersao);
}
