package br.com.example.senac.businessDocsAi.categories.repository;

import br.com.example.senac.businessDocsAi.categories.entity.UsuarioCategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface IUsuarioCategoriaRepository extends JpaRepository<UsuarioCategoriaEntity, Long> {

    List<UsuarioCategoriaEntity> findByUsuarioId(Long usuarioId);

    @Query("select uc.categoriaId from UsuarioCategoriaEntity uc where uc.usuarioId = :usuarioId")
    Set<Long> findCategoriaIdsByUsuarioId(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);
}
