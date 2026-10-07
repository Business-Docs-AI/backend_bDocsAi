package br.com.example.senac.businessDocsAi.document.tool;

import br.com.example.senac.businessDocsAi.document.dto.MacroprocessoResponseDTO;
import br.com.example.senac.businessDocsAi.document.service.MacroprocessoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MacroprocessoToolsTest {

    @Mock
    private MacroprocessoService macroprocessoService;

    private MacroprocessoTools tools;

    @BeforeEach
    void setUp() {
        tools = new MacroprocessoTools(macroprocessoService);
    }

    @Test
    void listarMacroprocessosSemNenhumCadastradoAvisaClaramente() {
        when(macroprocessoService.listar()).thenReturn(List.of());

        String resposta = tools.listarMacroprocessos();

        assertThat(resposta).containsIgnoringCase("não há nenhum macroprocesso");
    }

    @Test
    void listarMacroprocessosFormataIdENome() {
        when(macroprocessoService.listar())
                .thenReturn(List.of(new MacroprocessoResponseDTO(1L, "Gestão de Pedidos", "desc")));

        String resposta = tools.listarMacroprocessos();

        assertThat(resposta).contains("ID: 1").contains("Gestão de Pedidos");
    }
}
