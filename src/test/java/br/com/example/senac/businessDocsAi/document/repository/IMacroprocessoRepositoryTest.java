package br.com.example.senac.businessDocsAi.document.repository;

import br.com.example.senac.businessDocsAi.document.entity.MacroprocessoEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class IMacroprocessoRepositoryTest {

    @Autowired
    private IMacroprocessoRepository repository;

    @Test
    void salvaEEncontraUmMacroprocesso() {
        MacroprocessoEntity macroprocesso = new MacroprocessoEntity();
        macroprocesso.setNome("Gestão de Pedidos");
        macroprocesso.setDescricao("Do recebimento ao faturamento do pedido.");

        MacroprocessoEntity salvo = repository.save(macroprocesso);

        Optional<MacroprocessoEntity> encontrado = repository.findById(salvo.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNome()).isEqualTo("Gestão de Pedidos");
        assertThat(encontrado.get().getDescricao()).isEqualTo("Do recebimento ao faturamento do pedido.");
    }
}
