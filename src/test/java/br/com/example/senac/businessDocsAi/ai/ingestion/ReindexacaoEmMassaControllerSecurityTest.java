package br.com.example.senac.businessDocsAi.ai.ingestion;

import br.com.example.senac.businessDocsAi.ai.retrieval.RagCicloVidaFiltroService;
import br.com.example.senac.businessDocsAi.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ReindexacaoEmMassaController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import(ReindexacaoEmMassaControllerSecurityTest.MethodSecurityTestConfig.class)
@TestPropertySource(properties = "bdocs.documentacao-estruturada.enabled=true")
class ReindexacaoEmMassaControllerSecurityTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReindexacaoEmMassaService reindexacaoEmMassaService;

    @MockitoBean
    private RagCicloVidaFiltroService ragCicloVidaFiltroService;

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403() throws Exception {
        mockMvc.perform(post("/admin/reindexacao")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void editorRecebe403() throws Exception {
        mockMvc.perform(post("/admin/reindexacao")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsegueDispararAReindexacao() throws Exception {
        when(reindexacaoEmMassaService.contarDocumentosAtivos()).thenReturn(5);

        mockMvc.perform(post("/admin/reindexacao")).andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403NoStatus() throws Exception {
        mockMvc.perform(get("/admin/reindexacao/status")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsegueLerOStatus() throws Exception {
        when(ragCicloVidaFiltroService.chunksComMetadadoIncompleto()).thenReturn(0L);
        when(ragCicloVidaFiltroService.filtroStatusVigente())
                .thenReturn(Optional.of(dev.langchain4j.store.embedding.filter.MetadataFilterBuilder
                        .metadataKey("status_ciclo_vida").isEqualTo("VIGENTE")));

        mockMvc.perform(get("/admin/reindexacao/status")).andExpect(status().isOk());
    }
}
