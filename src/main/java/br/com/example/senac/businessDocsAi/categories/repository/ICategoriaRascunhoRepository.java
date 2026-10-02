package br.com.example.senac.businessDocsAi.categories.repository;

import br.com.example.senac.businessDocsAi.categories.entity.CategoriaRascunhoEntity;
import br.com.example.senac.businessDocsAi.categories.entity.StatusRascunhoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ICategoriaRascunhoRepository extends JpaRepository<CategoriaRascunhoEntity, Long> {

    Optional<CategoriaRascunhoEntity> findFirstByConversaIdAndStatusOrderByCriadoEmDesc(
            UUID conversaId, StatusRascunhoCategoria status
    );

    void deleteByConversaId(UUID conversaId);
}
