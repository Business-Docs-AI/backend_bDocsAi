package br.com.example.senac.businessDocsAi.document.controller;

import br.com.example.senac.businessDocsAi.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sem @TestPropertySource ligando a flag, o default de application.yaml (false) vale — este
 * controller é @ConditionalOnProperty e não deve existir no contexto.
 */
@WebMvcTest(
        controllers = DocumentoStatusCicloVidaController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
class DocumentoStatusCicloVidaControllerDesligadoPorDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void rotaNaoExisteComAFlagDesligada() throws Exception {
        mockMvc.perform(patch("/documentos/" + UUID.randomUUID() + "/status-ciclo-vida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"OBSOLETO\"}"))
                .andExpect(status().isNotFound());
    }
}
