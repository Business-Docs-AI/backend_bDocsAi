package br.com.example.senac.businessDocsAi.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownConversorServiceTest {

    private static final int LIMITE = 50;

    private MarkdownConversorService conversor;

    @BeforeEach
    void setUp() {
        conversor = new MarkdownConversorService(LIMITE);
    }

    @Test
    void textoMenorQueOLimiteNaoEhAlterado() {
        String textoCurto = "Qual é a política de home office?";

        assertThat(conversor.ehTextoGrande(textoCurto)).isFalse();
        assertThat(conversor.converterSeNecessario(textoCurto)).isEqualTo(textoCurto);
    }

    @Test
    void textoGrandeComParagrafosSeparadosPorLinhaEmBrancoViraParagrafosMarkdown() {
        String textoColado = "Primeiro parágrafo com bastante texto para passar do limite definido.\n\n"
                + "Segundo parágrafo, também longo, continuando a explicação do assunto tratado aqui.";

        String resultado = conversor.converterSeNecessario(textoColado);

        assertThat(resultado).isEqualTo(
                "Primeiro parágrafo com bastante texto para passar do limite definido.\n\n"
                        + "Segundo parágrafo, também longo, continuando a explicação do assunto tratado aqui."
        );
    }

    @Test
    void marcadoresDeListaComSimbolosVariadosSaoNormalizadosParaHifen() {
        String textoColado = "Itens da política de férias, que é bem extensa e precisa ser detalhada:\n"
                + "• Trinta dias por ano\n"
                + "* Pode ser dividido em até três períodos\n"
                + "▪ Precisa de aprovação do gestor direto";

        String resultado = conversor.converterSeNecessario(textoColado);

        assertThat(resultado).isEqualTo(
                "Itens da política de férias, que é bem extensa e precisa ser detalhada:\n"
                        + "- Trinta dias por ano\n"
                        + "- Pode ser dividido em até três períodos\n"
                        + "- Precisa de aprovação do gestor direto"
        );
    }

    @Test
    void itensNumeradosSaoPreservadosComFormatoDeListaMarkdown() {
        String textoColado = "Passos do processo de contratação, descritos em detalhe a seguir:\n"
                + "1) Entrevista com o RH\n"
                + "2) Entrevista técnica\n"
                + "3) Proposta final";

        String resultado = conversor.converterSeNecessario(textoColado);

        assertThat(resultado).isEqualTo(
                "Passos do processo de contratação, descritos em detalhe a seguir:\n"
                        + "1. Entrevista com o RH\n"
                        + "2. Entrevista técnica\n"
                        + "3. Proposta final"
        );
    }

    @Test
    void quebrasDeLinhaWindowsSaoNormalizadasAntesDaConversao() {
        String textoColado = "Parágrafo um, longo o suficiente para disparar a conversão prevista.\r\n\r\n"
                + "Parágrafo dois, também detalhado, encerrando o texto colado no chat.";

        String resultado = conversor.converterSeNecessario(textoColado);

        assertThat(resultado).doesNotContain("\r");
        assertThat(resultado).contains(
                "Parágrafo um, longo o suficiente para disparar a conversão prevista.\n\n"
                        + "Parágrafo dois"
        );
    }
}
