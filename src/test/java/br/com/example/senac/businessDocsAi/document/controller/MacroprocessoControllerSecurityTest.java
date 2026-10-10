package br.com.example.senac.businessDocsAi.document.controller;

import br.com.example.senac.businessDocsAi.document.dto.MacroprocessoResponseDTO;
import br.com.example.senac.businessDocsAi.document.service.MacroprocessoService;
import br.com.example.senac.businessDocsAi.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Com a flag ligada (via @TestPropertySource — o controller é @ConditionalOnProperty, não
 * existe sem isso), prova a matriz de permissões: leitura para qualquer autenticado,
 * escrita só ADMIN.
 */
@WebMvcTest(
        controllers = MacroprocessoController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import(MacroprocessoControllerSecurityTest.MethodSecurityTestConfig.class)
@TestPropertySource(properties = "bdocs.documentacao-estruturada.enabled=true")
class MacroprocessoControllerSecurityTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MacroprocessoService macroprocessoService;

    private static final String CORPO = """
            {"nome":"Gestão de Pedidos","descricao":"desc"}
            """;

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioConsegueListar() throws Exception {
        when(macroprocessoService.listar()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/macroprocessos")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioConsegueBuscarPorId() throws Exception {
        when(macroprocessoService.buscarPorId(1L)).thenReturn(new MacroprocessoResponseDTO(1L, "X", null));

        mockMvc.perform(get("/macroprocessos/1")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403AoCriar() throws Exception {
        mockMvc.perform(post("/macroprocessos").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void editorRecebe403AoCriar() throws Exception {
        mockMvc.perform(post("/macroprocessos").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403AoAtualizar() throws Exception {
        mockMvc.perform(put("/macroprocessos/1").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403AoExcluir() throws Exception {
        mockMvc.perform(delete("/macroprocessos/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsegueCriar() throws Exception {
        when(macroprocessoService.criar(any())).thenReturn(new MacroprocessoResponseDTO(1L, "Gestão de Pedidos", "desc"));

        mockMvc.perform(post("/macroprocessos").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsegueAtualizar() throws Exception {
        when(macroprocessoService.atualizar(eq(1L), any()))
                .thenReturn(new MacroprocessoResponseDTO(1L, "Gestão de Pedidos", "desc"));

        mockMvc.perform(put("/macroprocessos/1").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsegueExcluir() throws Exception {
        mockMvc.perform(delete("/macroprocessos/1")).andExpect(status().isNoContent());
    }
}
