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

/**
 * Macroprocesso da arquitetura de processos (ex.: APQC) — complementar às categorias:
 * processo atravessa setores, então o setor (categoria/área dona) continua sendo o único
 * controle de acesso; isto aqui é só classificação/organização.
 */
@Entity
@Table(name = "macroprocesso")
@Getter
@Setter
@NoArgsConstructor
public class MacroprocessoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 1000)
    private String descricao;
}
