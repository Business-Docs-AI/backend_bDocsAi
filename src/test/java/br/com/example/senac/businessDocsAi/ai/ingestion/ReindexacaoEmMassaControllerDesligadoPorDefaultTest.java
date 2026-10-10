package br.com.example.senac.businessDocsAi.ai.ingestion;

import br.com.example.senac.businessDocsAi.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sem @TestPropertySource ligando a flag, o default de application.yaml (false) vale — este
 * controller é @ConditionalOnProperty e não deve existir no contexto.
 */
@WebMvcTest(
        controllers = ReindexacaoEmMassaController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
class ReindexacaoEmMassaControllerDesligadoPorDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void rotaNaoExisteComAFlagDesligada() throws Exception {
        mockMvc.perform(post("/admin/reindexacao")).andExpect(status().isNotFound());
    }
}
