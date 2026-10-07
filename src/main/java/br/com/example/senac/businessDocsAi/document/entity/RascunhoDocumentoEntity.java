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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    // Preenchido só na confirmação: para CRIAR é o ID do documento novo (não existe antes
    // disso); para ATUALIZAR é o mesmo valor de documentoIdAlvo. Permite ao ChatService
    // devolver o documento resultante ao frontend sem precisar adivinhar qual foi, já que
    // documentoIdAlvo é sempre nulo no caso de criação.
    @Column(name = "documento_resultante_id")
    private UUID documentoResultanteId;

    // Diferente de DocumentoEntity/DocumentoVersaoEntity: aqui o JSON pode conter TUDO (é só
    // uma proposta) — conteúdo E metadados juntos. A separação (B3) acontece só na
    // confirmação, ao aplicar nas colunas de DocumentoEntity (Etapa 13) — nenhum código
    // ainda escreve neste campo (fica pronto pra quando a tool estruturada existir).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conteudo_estruturado", columnDefinition = "jsonb")
    private String conteudoEstruturado;

    @Column(name = "versao_schema", length = 20)
    private String versaoSchema;
}
