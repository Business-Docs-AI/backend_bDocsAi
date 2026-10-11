package br.com.example.senac.businessDocsAi.categories.service;

import br.com.example.senac.businessDocsAi.categories.dto.CategoryResponseDTO;
import br.com.example.senac.businessDocsAi.categories.entity.CategoryEntity;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * {@code /categories} não tinha {@code @PreAuthorize} nas leituras, então qualquer usuário
 * autenticado via qualquer categoria do sistema. Esses testes provam que {@link
 * CategoryService#list()} e {@link CategoryService#findById(Long)} agora respeitam o mesmo
 * controle de acesso por categoria usado no resto da aplicação ({@link CategoriaAccessService}).
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private ICategoryRepository repository;

    @Mock
    private CategoriaAccessService categoriaAccessService;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(repository, categoriaAccessService);
    }

    @Test
    void listDevolveSomenteCategoriasAcessiveisParaUsuarioComum() {
        CategoryEntity acessivel = categoria(1L, "RH");
        CategoryEntity inacessivel = categoria(2L, "Financeiro");

        when(repository.findAll()).thenReturn(List.of(acessivel, inacessivel));
        when(categoriaAccessService.podeAcessarCategoria(1L)).thenReturn(true);
        when(categoriaAccessService.podeAcessarCategoria(2L)).thenReturn(false);

        List<CategoryResponseDTO> resultado = categoryService.list();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(1L);
    }

    @Test
    void listDevolveTodasAsCategoriasParaAdmin() {
        CategoryEntity categoriaA = categoria(1L, "RH");
        CategoryEntity categoriaB = categoria(2L, "Financeiro");

        when(repository.findAll()).thenReturn(List.of(categoriaA, categoriaB));
        when(categoriaAccessService.podeAcessarCategoria(1L)).thenReturn(true);
        when(categoriaAccessService.podeAcessarCategoria(2L)).thenReturn(true);

        List<CategoryResponseDTO> resultado = categoryService.list();

        assertThat(resultado).hasSize(2);
    }

    @Test
    void findByIdLancaAccessDeniedQuandoUsuarioNaoTemAcessoACategoria() {
        CategoryEntity categoria = categoria(2L, "Financeiro");

        when(repository.findById(2L)).thenReturn(Optional.of(categoria));
        doThrow(new AccessDeniedException("Você não tem acesso a esta categoria"))
                .when(categoriaAccessService).validarAcessoCategoria(2L);

        assertThatThrownBy(() -> categoryService.findById(2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findByIdLancaNotFoundQuandoCategoriaNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    private CategoryEntity categoria(Long id, String nome) {
        CategoryEntity categoria = new CategoryEntity();
        categoria.setId(id);
        categoria.setName(nome);
        return categoria;
    }
}
