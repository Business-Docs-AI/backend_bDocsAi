package br.com.example.senac.businessDocsAi.categories.service;

import br.com.example.senac.businessDocsAi.categories.dto.CategoryResponseDTO;
import br.com.example.senac.businessDocsAi.categories.entity.CategoryEntity;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.categories.repository.IUsuarioCategoriaRepository;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoAreaParticipanteRepository;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import br.com.example.senac.businessDocsAi.exception.BadRequestException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    private IDocumentoRepository documentoRepository;

    @Mock
    private IUsuarioCategoriaRepository usuarioCategoriaRepository;

    @Mock
    private IDocumentoAreaParticipanteRepository documentoAreaParticipanteRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(
                repository,
                categoriaAccessService,
                documentoRepository,
                usuarioCategoriaRepository,
                documentoAreaParticipanteRepository
        );
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

    @Test
    void deleteLancaNotFoundQuandoCategoriaNaoExiste() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(documentoRepository, usuarioCategoriaRepository, documentoAreaParticipanteRepository);
    }

    @Test
    void deleteRemoveCategoriaQuandoNaoHaVinculos() {
        when(repository.existsById(1L)).thenReturn(true);
        when(documentoRepository.countByCategoriaId(1L)).thenReturn(0L);
        when(usuarioCategoriaRepository.countByCategoriaId(1L)).thenReturn(0L);
        when(documentoAreaParticipanteRepository.countByCategoriaId(1L)).thenReturn(0L);

        categoryService.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteLancaBadRequestQuandoHaDocumentoVinculado() {
        when(repository.existsById(1L)).thenReturn(true);
        when(documentoRepository.countByCategoriaId(1L)).thenReturn(2L);
        when(usuarioCategoriaRepository.countByCategoriaId(1L)).thenReturn(0L);
        when(documentoAreaParticipanteRepository.countByCategoriaId(1L)).thenReturn(0L);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("2 documento(s)");

        verify(repository, org.mockito.Mockito.never()).deleteById(1L);
    }

    @Test
    void deleteLancaBadRequestQuandoHaUsuarioVinculado() {
        when(repository.existsById(1L)).thenReturn(true);
        when(documentoRepository.countByCategoriaId(1L)).thenReturn(0L);
        when(usuarioCategoriaRepository.countByCategoriaId(1L)).thenReturn(1L);
        when(documentoAreaParticipanteRepository.countByCategoriaId(1L)).thenReturn(0L);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("1 usuário(s)");

        verify(repository, org.mockito.Mockito.never()).deleteById(1L);
    }

    private CategoryEntity categoria(Long id, String nome) {
        CategoryEntity categoria = new CategoryEntity();
        categoria.setId(id);
        categoria.setName(nome);
        return categoria;
    }
}
