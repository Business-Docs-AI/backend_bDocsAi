package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.document.dto.MacroprocessoRequestDTO;
import br.com.example.senac.businessDocsAi.document.dto.MacroprocessoResponseDTO;
import br.com.example.senac.businessDocsAi.document.entity.MacroprocessoEntity;
import br.com.example.senac.businessDocsAi.document.repository.IMacroprocessoRepository;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MacroprocessoServiceTest {

    @Mock
    private IMacroprocessoRepository repository;

    private MacroprocessoService service;

    @BeforeEach
    void setUp() {
        service = new MacroprocessoService(repository);
    }

    @Test
    void listarDevolveTodosMapeados() {
        MacroprocessoEntity entidade = entidade(1L, "Gestão de Pedidos", "desc");
        when(repository.findAll()).thenReturn(List.of(entidade));

        List<MacroprocessoResponseDTO> resultado = service.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nome()).isEqualTo("Gestão de Pedidos");
    }

    @Test
    void buscarPorIdLancaNotFoundQuandoNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void criarSalvaEDevolveOMacroprocesso() {
        when(repository.save(any(MacroprocessoEntity.class))).thenAnswer(inv -> {
            MacroprocessoEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        MacroprocessoResponseDTO resultado = service.criar(new MacroprocessoRequestDTO("Gestão de Pedidos", "desc"));

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("Gestão de Pedidos");
    }

    @Test
    void atualizarLancaNotFoundQuandoNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, new MacroprocessoRequestDTO("X", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void excluirLancaNotFoundQuandoNaoExiste() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.excluir(99L)).isInstanceOf(NotFoundException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void excluirChamaDeleteQuandoExiste() {
        when(repository.existsById(1L)).thenReturn(true);

        service.excluir(1L);

        verify(repository).deleteById(1L);
    }

    private MacroprocessoEntity entidade(Long id, String nome, String descricao) {
        MacroprocessoEntity entidade = new MacroprocessoEntity();
        entidade.setId(id);
        entidade.setNome(nome);
        entidade.setDescricao(descricao);
        return entidade;
    }
}
