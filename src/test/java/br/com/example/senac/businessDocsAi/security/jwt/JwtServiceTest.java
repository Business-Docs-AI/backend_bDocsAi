package br.com.example.senac.businessDocsAi.security.jwt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-key-with-at-least-256-bits-of-length-for-hs256",
            60
    );

    @Test
    void deveGerarTokenComEmailComoSubject() {
        String token = jwtService.generateToken("user@example.com");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void deveConsiderarTokenValidoQuandoAssinaturaEExpiracaoOk() {
        String token = jwtService.generateToken("user@example.com");

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void deveConsiderarTokenInvalidoQuandoAdulterado() {
        String token = jwtService.generateToken("user@example.com");
        String tokenAdulterado = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.isTokenValid(tokenAdulterado)).isFalse();
    }

    @Test
    void deveConsiderarTokenInvalidoQuandoExpirado() {
        JwtService jwtServiceComExpiracaoImediata = new JwtService(
                "test-secret-key-with-at-least-256-bits-of-length-for-hs256",
                0
        );

        String token = jwtServiceComExpiracaoImediata.generateToken("user@example.com");

        assertThat(jwtServiceComExpiracaoImediata.isTokenValid(token)).isFalse();
    }
}
