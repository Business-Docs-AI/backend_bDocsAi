package br.com.example.senac.businessDocsAi.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_mensagem")
@Getter
@Setter
@NoArgsConstructor
public class MensagemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversa_id", nullable = false)
    private UUID conversaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    // JSON serializado (lista de FonteDTO) — texto puro, sem tipo jsonb nativo do
    // Hibernate, para não arriscar dupla serialização.
    @Column(columnDefinition = "TEXT")
    private String fontes;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
