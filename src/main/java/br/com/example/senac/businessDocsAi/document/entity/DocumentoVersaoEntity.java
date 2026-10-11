package br.com.example.senac.businessDocsAi.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "documento_versao")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoVersaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Column(name = "numero_versao", nullable = false)
    private int numeroVersao;

    @Column(nullable = false, length = 500)
    private String titulo;

    @Column(name = "conteudo_html", nullable = false, columnDefinition = "TEXT")
    private String conteudoHtml;

    @Column(name = "hash_conteudo", nullable = false, length = 64)
    private String hashConteudo;

    @Column(nullable = false)
    private String autor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "comentario_alteracao", length = 1000)
    private String comentarioAlteracao;

    // Snapshot do conteúdo estruturado DESTA versão (migration V12, decisão B3/C3) — NULL
    // quando a versão foi gerada pelo fluxo legado de HTML. Ver DocumentoService.aplicarNovaVersao.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conteudo_estruturado", columnDefinition = "jsonb")
    private String conteudoEstruturado;

    @Column(name = "versao_schema", length = 20)
    private String versaoSchema;
}
