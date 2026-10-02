package br.com.example.senac.businessDocsAi.categories.service;

import br.com.example.senac.businessDocsAi.categories.repository.IUsuarioCategoriaRepository;
import br.com.example.senac.businessDocsAi.security.CurrentUserProvider;
import br.com.example.senac.businessDocsAi.user.Enum.UserEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Ponto único de controle de acesso por categoria: ADMIN tem acesso irrestrito; EDITOR e
 * USUARIO só acessam (consultar, criar, atualizar) documentos das categorias em que
 * estiverem cadastrados (ver {@code usuario_categoria}). Um usuário sem nenhuma categoria
 * vinculada não acessa nenhum documento.
 */
@Service
@RequiredArgsConstructor
public class CategoriaAccessService {

    private final IUsuarioCategoriaRepository usuarioCategoriaRepository;
    private final CurrentUserProvider currentUserProvider;

    public boolean isAdmin() {
        return currentUserProvider.getCurrentRole() == UserEnum.ADMIN;
    }

    public Set<Long> categoriasDoUsuarioAtual() {
        return usuarioCategoriaRepository.findCategoriaIdsByUsuarioId(currentUserProvider.getCurrentUserId());
    }

    public boolean podeAcessarCategoria(Long categoriaId) {
        if (isAdmin()) {
            return true;
        }
        return categoriaId != null && categoriasDoUsuarioAtual().contains(categoriaId);
    }

    public void validarAcessoCategoria(Long categoriaId) {
        if (!podeAcessarCategoria(categoriaId)) {
            throw new AccessDeniedException("Você não tem acesso a esta categoria");
        }
    }
}
