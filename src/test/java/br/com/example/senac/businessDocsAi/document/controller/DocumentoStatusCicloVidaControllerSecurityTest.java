package br.com.example.senac.businessDocsAi.document.controller;

import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;
import br.com.example.senac.businessDocsAi.document.entity.StatusCicloVida;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import br.com.example.senac.businessDocsAi.document.service.DocumentoService;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DocumentoStatusCicloVidaController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import(DocumentoStatusCicloVidaControllerSecurityTest.MethodSecurityTestConfig.class)
@TestPropertySource(properties = "bdocs.documentacao-estruturada.enabled=true")
class DocumentoStatusCicloVidaControllerSecurityTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final UUID DOC_ID = UUID.randomUUID();
    private static final String CORPO = """
            {"novoStatus":"OBSOLETO"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentoService documentoService;

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403() throws Exception {
        mockMvc.perform(patch("/documentos/" + DOC_ID + "/status-ciclo-vida")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void editorRecebe403() throws Exception {
        mockMvc.perform(patch("/documentos/" + DOC_ID + "/status-ciclo-vida")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsegueAtualizar() throws Exception {
        when(documentoService.atualizarStatusCicloVida(eq(DOC_ID), eq(StatusCicloVida.OBSOLETO)))
                .thenReturn(new DocumentoResponseDTO(
                        DOC_ID, "Título", "<p>x</p>", 1, StatusIndexacao.INDEXADO,
                        "Autor", LocalDateTime.now(), null, null, null, null
                ));

        mockMvc.perform(patch("/documentos/" + DOC_ID + "/status-ciclo-vida")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk());
    }
}
