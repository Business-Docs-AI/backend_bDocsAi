package br.com.example.senac.businessDocsAi.categories.service;

import br.com.example.senac.businessDocsAi.categories.repository.IUsuarioCategoriaRepository;
import br.com.example.senac.businessDocsAi.security.CurrentUserProvider;
import br.com.example.senac.businessDocsAi.user.Enum.UserEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Linha de base do único controle de acesso por categoria do sistema — ANTES das áreas
 * participantes (Etapa 6, join table {@code documento_area_participante}). O construtor de
 * {@link CategoriaAccessService} só recebe {@link IUsuarioCategoriaRepository} e
 * {@link CurrentUserProvider}: não há NENHUM jeito de uma área participante (puramente
 * informativa) influenciar o resultado de {@code podeAcessarCategoria}, estruturalmente — a
 * classe nem tem como consultar essa tabela.
 */
@ExtendWith(MockitoExtension.class)
class CategoriaAccessServiceTest {

    private static final Long USUARIO_ID = 1L;

    @Mock
    private IUsuarioCategoriaRepository usuarioCategoriaRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private CategoriaAccessService service;

    @BeforeEach
    void setUp() {
        service = new CategoriaAccessService(usuarioCategoriaRepository, currentUserProvider);
    }

    @Test
    void adminAcessaQualquerCategoriaSemConsultarVinculos() {
        when(currentUserProvider.getCurrentRole()).thenReturn(UserEnum.ADMIN);

        assertThat(service.podeAcessarCategoria(999L)).isTrue();
    }

    @Test
    void usuarioComCategoriaVinculadaAcessaEla() {
        when(currentUserProvider.getCurrentRole()).thenReturn(UserEnum.USUARIO);
        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_ID);
        when(usuarioCategoriaRepository.findCategoriaIdsByUsuarioId(USUARIO_ID)).thenReturn(Set.of(1L));

        assertThat(service.podeAcessarCategoria(1L)).isTrue();
    }

    @Test
    void usuarioSemACategoriaVinculadaNaoAcessa() {
        when(currentUserProvider.getCurrentRole()).thenReturn(UserEnum.USUARIO);
        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_ID);
        when(usuarioCategoriaRepository.findCategoriaIdsByUsuarioId(USUARIO_ID)).thenReturn(Set.of(1L));

        assertThat(service.podeAcessarCategoria(2L)).isFalse();
    }

    @Test
    void validarAcessoCategoriaLancaAccessDeniedQuandoNaoTemAcesso() {
        when(currentUserProvider.getCurrentRole()).thenReturn(UserEnum.USUARIO);
        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_ID);
        when(usuarioCategoriaRepository.findCategoriaIdsByUsuarioId(USUARIO_ID)).thenReturn(Set.of());

        assertThatThrownBy(() -> service.validarAcessoCategoria(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    // Regressão explícita (Etapa 6): mesmo "vinculando" (simulando, já que a classe não tem
    // meio de consultar isso) uma área participante à categoria 2, o resultado de acesso do
    // usuário não muda — ele continua sem acesso a ela, porque só usuario_categoria importa.
    @Test
    void vincularUmaAreaParticipanteNaoAlteraOResultadoDeAcesso() {
        when(currentUserProvider.getCurrentRole()).thenReturn(UserEnum.USUARIO);
        when(currentUserProvider.getCurrentUserId()).thenReturn(USUARIO_ID);
        when(usuarioCategoriaRepository.findCategoriaIdsByUsuarioId(USUARIO_ID)).thenReturn(Set.of(1L));

        // Categoria 2 é "área participante" de algum documento, mas o usuário não está em
        // usuario_categoria pra ela — CategoriaAccessService nem tem como saber da área
        // participante (não é injetada), então o resultado é sempre baseado só em
        // usuario_categoria, continuando negado.
        assertThat(service.podeAcessarCategoria(2L)).isFalse();
    }
}
