package br.com.example.senac.businessDocsAi.user.controller;

import br.com.example.senac.businessDocsAi.security.jwt.JwtAuthenticationFilter;
import br.com.example.senac.businessDocsAi.user.Enum.UserEnum;
import br.com.example.senac.businessDocsAi.user.dto.UserDTO;
import br.com.example.senac.businessDocsAi.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova, com contexto Spring real + AOP de method security, que a leitura de usuários
 * (listar, buscar por ID, buscar por email) é restrita a ADMIN — essas rotas não tinham
 * {@code @PreAuthorize} e qualquer usuário autenticado conseguia listar todo mundo.
 */
@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import(UserControllerSecurityTest.MethodSecurityTestConfig.class)
class UserControllerSecurityTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403AoListarUsuarios() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void editorRecebe403AoListarUsuarios() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403AoBuscarUsuarioPorId() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioRecebe403AoBuscarUsuarioPorEmail() throws Exception {
        mockMvc.perform(get("/users/email/alguem@exemplo.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsegueListarUsuarios() throws Exception {
        when(userService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsegueBuscarUsuarioPorId() throws Exception {
        when(userService.findById(anyLong())).thenReturn(usuario());

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsegueBuscarUsuarioPorEmail() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(usuario());

        mockMvc.perform(get("/users/email/alguem@exemplo.com"))
                .andExpect(status().isOk());
    }

    private UserDTO usuario() {
        return UserDTO.builder()
                .id(1L)
                .name("Usuário Teste")
                .email("alguem@exemplo.com")
                .role(UserEnum.USUARIO)
                .build();
    }
}
