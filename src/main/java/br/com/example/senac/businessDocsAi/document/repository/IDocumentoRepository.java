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

    // Inclui documentos soft-deletados (deletado=true) de propósito: a linha continua
    // existindo e com ela a FK fk_documento_categoria, que bloquearia a exclusão da
    // categoria mesmo que a listagem (filtrada por deletado=false) não mostre nada.
    long countByCategoriaId(Long categoriaId);
}
