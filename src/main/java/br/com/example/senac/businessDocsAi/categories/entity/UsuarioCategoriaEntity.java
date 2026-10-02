package br.com.example.senac.businessDocsAi.categories.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario_categoria")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioCategoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    public UsuarioCategoriaEntity(Long usuarioId, Long categoriaId) {
        this.usuarioId = usuarioId;
        this.categoriaId = categoriaId;
    }
}
