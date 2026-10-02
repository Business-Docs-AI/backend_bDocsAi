package br.com.example.senac.businessDocsAi.categories.entity;

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

/**
 * Proposta de criação de categoria feita pela IA no chat (ver {@code CategoriaTools}). Só
 * vira uma {@link CategoryEntity} de verdade quando confirmada — e só pode ser confirmada
 * num turno de conversa posterior ao turno em que foi proposta (ver {@code turnoCriacao}),
 * mesma trava usada em {@code RascunhoDocumentoEntity}.
 */
@Entity
@Table(name = "categoria_rascunho")
@Getter
@Setter
@NoArgsConstructor
public class CategoriaRascunhoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversa_id", nullable = false)
    private UUID conversaId;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRascunhoCategoria status;

    @Column(name = "turno_criacao", nullable = false)
    private UUID turnoCriacao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;
}
