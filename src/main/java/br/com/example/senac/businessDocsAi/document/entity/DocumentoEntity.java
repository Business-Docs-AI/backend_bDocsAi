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

@Entity
@Table(name = "documento")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String titulo;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(name = "conteudo_html", nullable = false, columnDefinition = "TEXT")
    private String conteudoHtml;

    @Column(name = "hash_conteudo", nullable = false, length = 64)
    private String hashConteudo;

    @Column(name = "versao_atual", nullable = false)
    private int versaoAtual;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_indexacao", nullable = false, length = 20)
    private StatusIndexacao statusIndexacao;

    @Column(name = "criado_por", nullable = false)
    private String criadoPor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_por")
    private String atualizadoPor;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(nullable = false)
    private boolean deletado;

    @Column(name = "excluido_em")
    private LocalDateTime excluidoEm;
}
