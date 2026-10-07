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

import java.util.UUID;

/**
 * Vínculo N:N documento↔categoria, APENAS INFORMATIVO (decisão 3) — "este documento também
 * é relevante pra essa área, mas ela não é a dona dele". NUNCA concede acesso: o único
 * controle de acesso continua sendo {@code categoria_id} (área dona) em {@code documento},
 * verificado via {@code usuario_categoria}/{@code CategoriaAccessService} — esta classe não
 * é referenciada em nenhum ponto de controle de acesso, de propósito.
 */
@Entity
@Table(name = "documento_area_participante")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoAreaParticipanteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    public DocumentoAreaParticipanteEntity(UUID documentoId, Long categoriaId) {
        this.documentoId = documentoId;
        this.categoriaId = categoriaId;
    }
}
