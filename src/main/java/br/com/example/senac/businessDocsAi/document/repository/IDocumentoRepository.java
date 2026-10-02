package br.com.example.senac.businessDocsAi.document.repository;

import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IDocumentoRepository extends JpaRepository<DocumentoEntity, UUID> {

    List<DocumentoEntity> findByDeletadoFalseAndStatusIndexacaoIn(List<StatusIndexacao> status);

    List<DocumentoEntity> findByDeletadoFalseOrderByTituloAsc();

    List<DocumentoEntity> findByDeletadoFalseAndCategoriaIdOrderByTituloAsc(Long categoriaId);
}
