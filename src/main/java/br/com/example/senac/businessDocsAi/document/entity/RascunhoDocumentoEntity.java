package br.com.example.senac.businessDocsAi.document.entity;

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
 * Proposta de criação/atualização de documento feita pela IA no chat. Só vira um
 * {@link DocumentoEntity} de verdade (com histórico de versão e indexação) quando
 * confirmada — e só pode ser confirmada num turno de conversa posterior ao turno em que foi
 * proposta (ver {@code turnoCriacao}). Serve de auditoria enquanto a conversa existir, mesmo
 * quando nunca confirmada — mas é apagada junto quando a conversa é excluída (ver
 * {@code ChatService#excluirConversa}).
 */
@Entity
@Table(name = "documento_rascunho")
@Getter
@Setter
@NoArgsConstructor
public class RascunhoDocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversa_id", nullable = false)
    private UUID conversaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRascunho tipo;

    @Column(name = "documento_id_alvo")
    private UUID documentoIdAlvo;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(nullable = false, length = 500)
    private String titulo;

    @Column(name = "conteudo_html", nullable = false, columnDefinition = "TEXT")
    private String conteudoHtml;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRascunho status;

    @Column(name = "turno_criacao", nullable = false)
    private UUID turnoCriacao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;
}
