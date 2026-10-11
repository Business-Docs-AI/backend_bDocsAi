package br.com.example.senac.businessDocsAi.auth;

import br.com.example.senac.businessDocsAi.auth.dto.LoginRequestDTO;
import br.com.example.senac.businessDocsAi.auth.dto.LoginResponseDTO;
import br.com.example.senac.businessDocsAi.auth.service.AuthService;
import br.com.example.senac.businessDocsAi.exception.UnauthorizedException;
import br.com.example.senac.businessDocsAi.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void deveRetornarTokenQuandoCredenciaisValidas() {
        LoginRequestDTO request = new LoginRequestDTO("user@example.com", "senha123");

        when(jwtService.generateToken("user@example.com")).thenReturn("token-gerado");
        when(jwtService.getExpirationMinutes()).thenReturn(60L);

        LoginResponseDTO response = authService.login(request);

        assertThat(response.token()).isEqualTo("token-gerado");
        assertThat(response.expiresInMinutes()).isEqualTo(60L);
    }

    @Test
    void deveLancarUnauthorizedQuandoCredenciaisInvalidas() {
        LoginRequestDTO request = new LoginRequestDTO("user@example.com", "senhaErrada");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }
}
