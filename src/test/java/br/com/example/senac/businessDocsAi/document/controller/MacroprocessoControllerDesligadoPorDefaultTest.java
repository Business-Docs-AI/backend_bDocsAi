package br.com.example.senac.businessDocsAi.document.controller;

import br.com.example.senac.businessDocsAi.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sem @TestPropertySource ligando a flag, o default de application.yaml (false) vale — o
 * controller é @ConditionalOnProperty e não deve nem existir no contexto. Prova a regra 12:
 * "com a flag desligada, o sistema se comporta exatamente como hoje" (a rota simplesmente
 * não existe).
 */
@WebMvcTest(
        controllers = MacroprocessoController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
class MacroprocessoControllerDesligadoPorDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void rotaNaoExisteComAFlagDesligada() throws Exception {
        mockMvc.perform(get("/macroprocessos")).andExpect(status().isNotFound());
    }
}
