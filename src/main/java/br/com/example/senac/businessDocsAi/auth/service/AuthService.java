package br.com.example.senac.businessDocsAi.auth.service;

import br.com.example.senac.businessDocsAi.auth.dto.LoginRequestDTO;
import br.com.example.senac.businessDocsAi.auth.dto.LoginResponseDTO;
import br.com.example.senac.businessDocsAi.exception.UnauthorizedException;
import br.com.example.senac.businessDocsAi.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponseDTO login(LoginRequestDTO request) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException | org.springframework.security.authentication.DisabledException e) {
            throw new UnauthorizedException("Email ou senha inválidos");
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("Não foi possível autenticar: " + e.getMessage());
        }

        String token = jwtService.generateToken(request.email());

        return new LoginResponseDTO(token, "Bearer", jwtService.getExpirationMinutes());
    }
}
