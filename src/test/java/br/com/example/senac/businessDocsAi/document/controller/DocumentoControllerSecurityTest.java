package br.com.example.senac.businessDocsAi.document.controller;

import br.com.example.senac.businessDocsAi.ai.retrieval.PesquisaService;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoVersaoResponseDTO;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova, de ponta a ponta (contexto Spring real + AOP de method security), que o
 * {@code @PreAuthorize} declarado no controller bloqueia com 403 exatamente conforme a
 * matriz de permissões pedida. Não sobe banco/JWT real: {@link JwtAuthenticationFilter} é
 * excluído do contexto e a identidade do usuário vem de {@link WithMockUser}.
 */
@WebMvcTest(
        controllers = DocumentoController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import(DocumentoControllerSecurityTest.MethodSecurityTestConfig.class)
class DocumentoControllerSecurityTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final UUID DOC_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentoService documentoService;

    @MockitoBean
    private PesquisaService pesquisaService;

    private static final String CORPO_DOCUMENTO = """
            {"titulo":"Título","conteudoHtml":"<p>Conteúdo</p>","categoriaId":1}
            """;

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403AoCriarDocumento() throws Exception {
        mockMvc.perform(post("/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_DOCUMENTO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403AoAtualizarDocumento() throws Exception {
        mockMvc.perform(put("/documentos/" + DOC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_DOCUMENTO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403AoExcluirDocumento() throws Exception {
        mockMvc.perform(delete("/documentos/" + DOC_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403AoListarVersoes() throws Exception {
        mockMvc.perform(get("/documentos/" + DOC_ID + "/versoes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403AoRestaurarVersao() throws Exception {
        mockMvc.perform(post("/documentos/" + DOC_ID + "/versoes/1/restaurar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403AoForcarReindexacao() throws Exception {
        mockMvc.perform(post("/documentos/" + DOC_ID + "/reindexar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403AoBuscarVersaoEspecifica() throws Exception {
        mockMvc.perform(get("/documentos/" + DOC_ID + "/versoes/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioConsegueVisualizarDocumento() throws Exception {
        when(documentoService.buscarPorId(DOC_ID)).thenReturn(documentoResponse());

        mockMvc.perform(get("/documentos/" + DOC_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioConsegueUsarBuscaSemantica() throws Exception {
        when(pesquisaService.buscar("termo")).thenReturn(List.of());

        mockMvc.perform(get("/documentos/busca").param("q", "termo"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void editorConsegueCriarDocumento() throws Exception {
        when(documentoService.criar(any())).thenReturn(documentoResponse());

        mockMvc.perform(post("/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_DOCUMENTO))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void editorConsegueListarVersoes() throws Exception {
        when(documentoService.listarVersoes(DOC_ID)).thenReturn(List.of());

        mockMvc.perform(get("/documentos/" + DOC_ID + "/versoes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void editorConsegueBuscarVersaoEspecifica() throws Exception {
        when(documentoService.buscarVersao(DOC_ID, 1)).thenReturn(versaoResponse());

        mockMvc.perform(get("/documentos/" + DOC_ID + "/versoes/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void editorRecebe403AoExcluirDocumento() throws Exception {
        mockMvc.perform(delete("/documentos/" + DOC_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void editorRecebe403AoRestaurarVersao() throws Exception {
        mockMvc.perform(post("/documentos/" + DOC_ID + "/versoes/1/restaurar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void editorRecebe403AoForcarReindexacao() throws Exception {
        mockMvc.perform(post("/documentos/" + DOC_ID + "/reindexar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsegueExcluirDocumento() throws Exception {
        mockMvc.perform(delete("/documentos/" + DOC_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsegueRestaurarVersao() throws Exception {
        when(documentoService.restaurarVersao(DOC_ID, 1)).thenReturn(documentoResponse());

        mockMvc.perform(post("/documentos/" + DOC_ID + "/versoes/1/restaurar"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsegueForcarReindexacao() throws Exception {
        mockMvc.perform(post("/documentos/" + DOC_ID + "/reindexar"))
                .andExpect(status().isNoContent());
    }

    private DocumentoResponseDTO documentoResponse() {
        return new DocumentoResponseDTO(
                DOC_ID, "Título", "<p>Conteúdo</p>", 1, StatusIndexacao.INDEXADO,
                "Autor", LocalDateTime.now(), null, null, null, null
        );
    }

    private DocumentoVersaoResponseDTO versaoResponse() {
        return new DocumentoVersaoResponseDTO(
                UUID.randomUUID(), 1, "Título", "<p>Conteúdo</p>", "Autor", LocalDateTime.now(), null
        );
    }
}
