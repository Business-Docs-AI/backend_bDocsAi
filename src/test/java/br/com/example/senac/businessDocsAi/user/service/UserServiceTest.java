package br.com.example.senac.businessDocsAi.user.service;

import br.com.example.senac.businessDocsAi.categories.entity.CategoryEntity;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.categories.repository.IUsuarioCategoriaRepository;
import br.com.example.senac.businessDocsAi.user.Enum.UserEnum;
import br.com.example.senac.businessDocsAi.user.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduz o bug em que editar um usuário removendo apenas uma categoria (mantendo as
 * demais) falhava com violação de uk_usuario_categoria: o delete+insert do vínculo
 * usuario_categoria ocorria na mesma transação sem flush, e o Hibernate agrupa todos os
 * INSERTs antes dos DELETEs no flush, violando a constraint única quando há sobreposição
 * entre a lista antiga e a nova.
 */
@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ICategoryRepository categoryRepository;

    @Autowired
    private IUsuarioCategoriaRepository usuarioCategoriaRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateComSobreposicaoParcialDeCategoriasNaoViolaConstraintUnica() {
        CategoryEntity categoriaA = criarCategoria("Categoria A (teste update usuário)");
        CategoryEntity categoriaB = criarCategoria("Categoria B (teste update usuário)");

        UserDTO criado = userService.create(UserDTO.builder()
                .name("Usuário Teste Categorias")
                .email("usuario.teste.categorias@exemplo.com")
                .password("senha123")
                .role(UserEnum.USUARIO)
                .categoriaIds(List.of(categoriaA.getId(), categoriaB.getId()))
                .build());

        UserDTO atualizado = userService.update(criado.getId(), UserDTO.builder()
                .name(criado.getName())
                .email(criado.getEmail())
                .role(criado.getRole())
                .categoriaIds(List.of(categoriaA.getId()))
                .build());

        assertThat(atualizado.getCategoriaIds()).containsExactly(categoriaA.getId());
        assertThat(usuarioCategoriaRepository.findByUsuarioId(criado.getId()))
                .extracting(vinculo -> vinculo.getCategoriaId())
                .containsExactly(categoriaA.getId());
    }

    private CategoryEntity criarCategoria(String nome) {
        CategoryEntity categoria = new CategoryEntity();
        categoria.setName(nome);
        categoria.setDescription("desc");
        return categoryRepository.save(categoria);
    }
}
