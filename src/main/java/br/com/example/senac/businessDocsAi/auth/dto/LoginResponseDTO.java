package br.com.example.senac.businessDocsAi.auth.dto;

public record LoginResponseDTO(
        String token,
        String tokenType,
        long expiresInMinutes
) {
}
