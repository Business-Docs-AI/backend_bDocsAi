package br.com.example.senac.businessDocsAi.categories.tool;

import br.com.example.senac.businessDocsAi.categories.dto.CategoryRequestDTO;
import br.com.example.senac.businessDocsAi.categories.dto.CategoryResponseDTO;
import br.com.example.senac.businessDocsAi.categories.entity.CategoriaRascunhoEntity;
import br.com.example.senac.businessDocsAi.categories.entity.StatusRascunhoCategoria;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoriaRascunhoRepository;
import br.com.example.senac.businessDocsAi.categories.service.CategoryService;
import br.com.example.senac.businessDocsAi.chat.service.ConversaContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaToolsTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private ICategoriaRascunhoRepository categoriaRascunhoRepository;

    private CategoriaTools categoriaTools;

    private final UUID conversaId = UUID.randomUUID();
    private final UUID turnoAtual = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        categoriaTools = new CategoriaTools(categoryService, categoriaRascunhoRepository);
        ConversaContextHolder.iniciar(conversaId, turnoAtual);
    }

    @AfterEach
    void tearDown() {
        ConversaContextHolder.limpar();
    }

    @Test
    void prepararCriacaoCategoriaApenasGravaRascunhoSemChamarCategoryService() {
        when(categoriaRascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(conversaId, StatusRascunhoCategoria.PENDENTE))
                .thenReturn(Optional.empty());
        when(categoriaRascunhoRepository.save(any(CategoriaRascunhoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String resposta = categoriaTools.prepararCriacaoCategoria("Jurídico", "Contratos e questões legais");

        ArgumentCaptor<CategoriaRascunhoEntity> captor = ArgumentCaptor.forClass(CategoriaRascunhoEntity.class);
        verify(categoriaRascunhoRepository).save(captor.capture());

        CategoriaRascunhoEntity salvo = captor.getValue();
        assertThat(salvo.getConversaId()).isEqualTo(conversaId);
        assertThat(salvo.getNome()).isEqualTo("Jurídico");
        assertThat(salvo.getDescricao()).isEqualTo("Contratos e questões legais");
        assertThat(salvo.getStatus()).isEqualTo(StatusRascunhoCategoria.PENDENTE);
        assertThat(salvo.getTurnoCriacao()).isEqualTo(turnoAtual);

        assertThat(resposta).contains("Jurídico");
        verifyNoInteractions(categoryService);
    }

    @Test
    void prepararCriacaoCategoriaReaproveitaRascunhoPendenteExistentePreservandoOTurnoOriginal() {
        UUID turnoAnterior = UUID.randomUUID();
        CategoriaRascunhoEntity existente = new CategoriaRascunhoEntity();
        existente.setId(10L);
        existente.setConversaId(conversaId);
        existente.setNome("Jurídico");
        existente.setDescricao("descrição antiga");
        existente.setStatus(StatusRascunhoCategoria.PENDENTE);
        existente.setTurnoCriacao(turnoAnterior);
        existente.setCriadoEm(LocalDateTime.now().minusMinutes(5));

        when(categoriaRascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(conversaId, StatusRascunhoCategoria.PENDENTE))
                .thenReturn(Optional.of(existente));
        when(categoriaRascunhoRepository.save(any(CategoriaRascunhoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        categoriaTools.prepararCriacaoCategoria("Jurídico", "descrição nova");

        ArgumentCaptor<CategoriaRascunhoEntity> captor = ArgumentCaptor.forClass(CategoriaRascunhoEntity.class);
        verify(categoriaRascunhoRepository).save(captor.capture());

        CategoriaRascunhoEntity salvo = captor.getValue();
        assertThat(salvo.getId()).isEqualTo(10L);
        assertThat(salvo.getDescricao()).isEqualTo("descrição nova");
        // O turno não pode ser resetado — é o que permite confirmar no turno seguinte.
        assertThat(salvo.getTurnoCriacao()).isEqualTo(turnoAnterior);
    }

    @Test
    void confirmarCriacaoCategoriaSemNenhumRascunhoDevolveMensagemSemChamarCategoryService() {
        when(categoriaRascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(conversaId, StatusRascunhoCategoria.PENDENTE))
                .thenReturn(Optional.empty());

        String resposta = categoriaTools.confirmarCriacaoCategoria();

        assertThat(resposta).contains("Não há nenhuma proposta de categoria pendente");
        verifyNoInteractions(categoryService);
    }

    @Test
    void confirmarCriacaoCategoriaNoMesmoTurnoDaPropostaEhRecusado() {
        CategoriaRascunhoEntity rascunho = rascunhoCriar(turnoAtual);

        when(categoriaRascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(conversaId, StatusRascunhoCategoria.PENDENTE))
                .thenReturn(Optional.of(rascunho));

        String resposta = categoriaTools.confirmarCriacaoCategoria();

        assertThat(resposta).containsIgnoringCase("ainda não é possível confirmar");
        assertThat(rascunho.getStatus()).isEqualTo(StatusRascunhoCategoria.PENDENTE);
        verifyNoInteractions(categoryService);
        verify(categoriaRascunhoRepository, never()).save(any());
    }

    @Test
    void confirmarCriacaoCategoriaDeTurnoAnteriorCriaACategoriaEMarcaConfirmado() {
        UUID turnoAnterior = UUID.randomUUID();
        CategoriaRascunhoEntity rascunho = rascunhoCriar(turnoAnterior);

        when(categoriaRascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(conversaId, StatusRascunhoCategoria.PENDENTE))
                .thenReturn(Optional.of(rascunho));
        when(categoryService.save(new CategoryRequestDTO("Jurídico", "Contratos e questões legais")))
                .thenReturn(new CategoryResponseDTO(5L, "Jurídico", "Contratos e questões legais"));

        String resposta = categoriaTools.confirmarCriacaoCategoria();

        verify(categoryService).save(any());
        assertThat(rascunho.getStatus()).isEqualTo(StatusRascunhoCategoria.CONFIRMADO);
        assertThat(rascunho.getConfirmadoEm()).isNotNull();
        assertThat(resposta).contains("Jurídico").contains("5");
    }

    @Test
    void confirmarCriacaoCategoriaQuandoQuemConfirmaNaoEhAdminDevolveMensagemClara() {
        UUID turnoAnterior = UUID.randomUUID();
        CategoriaRascunhoEntity rascunho = rascunhoCriar(turnoAnterior);

        when(categoriaRascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(conversaId, StatusRascunhoCategoria.PENDENTE))
                .thenReturn(Optional.of(rascunho));
        when(categoryService.save(any())).thenThrow(new AccessDeniedException("Acesso negado"));

        String resposta = categoriaTools.confirmarCriacaoCategoria();

        assertThat(resposta).containsIgnoringCase("administradores");
        assertThat(rascunho.getStatus()).isEqualTo(StatusRascunhoCategoria.PENDENTE);
        verify(categoriaRascunhoRepository, never()).save(any());
    }

    @Test
    void descartarCriacaoCategoriaMarcaDescartadoSemChamarCategoryService() {
        CategoriaRascunhoEntity rascunho = rascunhoCriar(UUID.randomUUID());

        when(categoriaRascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(conversaId, StatusRascunhoCategoria.PENDENTE))
                .thenReturn(Optional.of(rascunho));

        String resposta = categoriaTools.descartarCriacaoCategoria();

        assertThat(rascunho.getStatus()).isEqualTo(StatusRascunhoCategoria.DESCARTADO);
        assertThat(resposta).containsIgnoringCase("descartada");
        verifyNoInteractions(categoryService);
    }

    private CategoriaRascunhoEntity rascunhoCriar(UUID turno) {
        CategoriaRascunhoEntity rascunho = new CategoriaRascunhoEntity();
        rascunho.setConversaId(conversaId);
        rascunho.setNome("Jurídico");
        rascunho.setDescricao("Contratos e questões legais");
        rascunho.setStatus(StatusRascunhoCategoria.PENDENTE);
        rascunho.setTurnoCriacao(turno);
        rascunho.setCriadoEm(LocalDateTime.now());
        return rascunho;
    }
}
