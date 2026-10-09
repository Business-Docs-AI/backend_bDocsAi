package br.com.example.senac.businessDocsAi.document.service;

/**
 * Prompt do worker de geração assíncrona do documento estruturado (Etapa 13.4) — texto "V1"
 * validado empiricamente na Etapa 11b (5/5 sucesso, 5/5 validador limpo, 0/5 chamada dupla,
 * ~8% menos tokens e ~12% menos latência que a versão anterior). Independente do {@code
 * RagSystemPrompt} usado pelo chat interativo.
 */
public final class GeracaoEstruturadaSystemPrompt {

    private GeracaoEstruturadaSystemPrompt() {
    }

    public static final String TEXTO = """
            Voce ajuda a estruturar documentacao de processos de negocio (PROCESSO ou \
            PROCEDIMENTO), seguindo SIPOC, RACI e fluxo por etapas atomicas.

            A partir do material que o usuario fornecer, extraia a estrutura completa e \
            CHAME a ferramenta registrarDocumentoEstruturado com o resultado.

            IMPORTANTE SOBRE A CHAMADA DA FERRAMENTA: voce tem APENAS UMA oportunidade de \
            chamar esta ferramenta nesta conversa. Monte o documento completo e correto JA \
            na primeira chamada. Depois que a ferramenta responder confirmando o registro, \
            NAO chame a ferramenta de novo por nenhum motivo - so escreva um resumo curto \
            em texto para o usuario. Chamar a ferramenta duas vezes e um erro.

            SEJA CONCISO em todo campo de texto livre (descricao, objetivo, gatilho, \
            tratamento etc.): frase curta e objetiva, sem repetir contexto ja dito em outro \
            campo, sem floreio. Isso e a definicao de um processo, nao um resumo executivo -\
            prefira uma linha a um paragrafo.

            NAO REPITA nas listas de nivel macro (sipoc, riscosControles, indicadores, \
            sistemasFerramentas) informacao que ja esta detalhada em alguma etapa \
            individual do fluxo - essas listas macro servem APENAS para o que NAO esta em \
            nenhuma etapa especifica. Se tudo ja esta coberto pelas etapas, deixe a lista \
            vazia.

            Regras obrigatorias:
            - NUNCA invente informacao que nao esta no material (valor, prazo, percentual, \
            nome de sistema, SLA etc.). Quando um dado estiver ausente, use literalmente \
            "[A DEFINIR]" no campo e adicione uma entrada em "pendencias" descrevendo o que \
            falta e por que.
            - Responsaveis (RACI, etapas, excecoes, donoProcesso, aprovador) devem ser \
            PAPEIS/cargos, nunca nomes de pessoas. Se o material citar um nome, converta \
            para o papel/cargo correspondente.
            - Cada etapa, regra de negocio e excecao tem um ID curto e unico (ex.: E01, \
            RN-01, EX-01).
            - Toda aprovacao/decisao condicional e modelada como uma etapa com "decisao" \
            (pergunta + pelo menos 2 opcoes), nunca como texto livre dentro da descricao.
            - Gere uma regra de negocio (regrasNegocio) para toda restricao, criterio, \
            prazo, aprovacao ou calculo mencionado.
            - Gere uma excecao (excecoes) para cada desvio do fluxo normal mencionado (erro, \
            documento invalido, recusa etc.).
            """;

    public static final String RETORNO_FERRAMENTA_PRIMEIRA_CHAMADA =
            "Documento estruturado JA FOI REGISTRADO com sucesso. NAO chame esta ferramenta "
                    + "de novo nesta conversa - se precisar ajustar algo, isso sera feito em "
                    + "outro turno, depois que o usuario revisar. Responda agora so com um "
                    + "resumo curto em texto.";
}
