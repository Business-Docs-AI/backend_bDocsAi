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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    // Metadados de processo/governança (migration V9) — todos nullable, aditivos. Ver
    // DocumentoService.criar(): os dois primeiros são setados explicitamente lá, porque o
    // Hibernate envia NULL explícito para um campo não setado no objeto Java, o que
    // bypassaria o DEFAULT do banco (o DEFAULT só vale pra inserts que omitem a coluna).
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 20)
    private TipoDocumento tipoDocumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_ciclo_vida", length = 20)
    private StatusCicloVida statusCicloVida;

    @Column(name = "dono_processo")
    private String donoProcesso;

    @Column(name = "aprovador")
    private String aprovador;

    @Column(name = "data_vigencia")
    private LocalDate dataVigencia;

    @Column(name = "proxima_revisao")
    private LocalDate proximaRevisao;

    // Em meses (ex.: 12 = revisão anual) — usado para calcular proximaRevisao (Etapa 17).
    @Column(name = "periodicidade_revisao")
    private Integer periodicidadeRevisaoMeses;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidencialidade", length = 20)
    private Confidencialidade confidencialidade;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags;
}
