package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.document.dto.estruturado.DecisaoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DecisaoOpcaoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.EtapaDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.ExcecaoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.RaciEntryDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.RegraNegocioDTO;
import br.com.example.senac.businessDocsAi.document.entity.TipoDocumento;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validação SEMÂNTICA do {@link DocumentoEstruturadoDTO} — além do Bean Validation (campos
 * obrigatórios, tamanhos mínimos), confere integridade referencial entre os itens atômicos:
 * IDs únicos, referências que existem de fato, decisões bem formadas, sem etapa órfã. Nunca
 * lança exceção — devolve a lista de erros (vazia = válido) para a tool estruturada (Etapa
 * 13) devolver ao LLM corrigir e chamar de novo (decisão A2).
 */
@Component
public class DocumentoEstruturadoValidator {

    public List<String> validar(DocumentoEstruturadoDTO dto) {
        List<String> erros = new ArrayList<>();

        validarTipoDocumentoSuportado(dto, erros);

        Set<String> idsEtapas = coletarIdsDuplicados(dto.fluxo().stream().map(EtapaDTO::id).toList(), "etapa", erros);
        Set<String> idsRegras = coletarIdsDuplicados(
                dto.regrasNegocio().stream().map(RegraNegocioDTO::id).toList(), "regra de negócio", erros
        );
        coletarIdsDuplicados(dto.excecoes().stream().map(ExcecaoDTO::id).toList(), "exceção", erros);

        validarReferenciasDasEtapas(dto, idsEtapas, idsRegras, erros);
        validarRaci(dto, idsEtapas, erros);
        validarEtapaOrfa(dto, erros);

        return erros;
    }

    private void validarTipoDocumentoSuportado(DocumentoEstruturadoDTO dto, List<String> erros) {
        if (dto.tipoDocumento() != TipoDocumento.PROCESSO && dto.tipoDocumento() != TipoDocumento.PROCEDIMENTO) {
            erros.add(
                    "tipoDocumento " + dto.tipoDocumento() + " não é suportado pela tool estruturada nesta "
                            + "entrega — só PROCESSO e PROCEDIMENTO. Use a criação legada (HTML) para os demais tipos."
            );
        }
    }

    private Set<String> coletarIdsDuplicados(List<String> ids, String rotulo, List<String> erros) {
        Set<String> vistos = new HashSet<>();
        Set<String> duplicados = new HashSet<>();

        for (String id : ids) {
            if (!vistos.add(id)) {
                duplicados.add(id);
            }
        }

        for (String duplicado : duplicados) {
            erros.add("ID de " + rotulo + " duplicado: " + duplicado);
        }

        return vistos;
    }

    private void validarReferenciasDasEtapas(
            DocumentoEstruturadoDTO dto, Set<String> idsEtapas, Set<String> idsRegras, List<String> erros
    ) {
        for (EtapaDTO etapa : dto.fluxo()) {
            for (String regraId : etapa.regrasAplicaveis()) {
                if (!idsRegras.contains(regraId)) {
                    erros.add(etapa.id() + " referencia " + regraId + ", que não existe em regrasNegocio");
                }
            }

            boolean temProximaDireta = etapa.proximaEtapaId() != null;
            boolean temDecisao = etapa.decisao() != null;

            if (temProximaDireta && temDecisao) {
                erros.add(etapa.id() + " tem proximaEtapaId E decisao ao mesmo tempo — escolha só um dos dois");
            }

            if (temProximaDireta && !idsEtapas.contains(etapa.proximaEtapaId())) {
                erros.add(etapa.id() + " referencia proximaEtapaId " + etapa.proximaEtapaId() + ", que não existe no fluxo");
            }

            if (temDecisao) {
                validarDecisao(etapa, etapa.decisao(), idsEtapas, erros);
            }
        }
    }

    private void validarDecisao(EtapaDTO etapa, DecisaoDTO decisao, Set<String> idsEtapas, List<String> erros) {
        if (decisao.opcoes() == null || decisao.opcoes().size() < 2) {
            erros.add(etapa.id() + ": a decisão precisa de pelo menos 2 opções");
            return;
        }

        for (DecisaoOpcaoDTO opcao : decisao.opcoes()) {
            if (!idsEtapas.contains(opcao.proximaEtapaId())) {
                erros.add(
                        etapa.id() + ": a opção \"" + opcao.resposta() + "\" referencia " + opcao.proximaEtapaId()
                                + ", que não existe no fluxo"
                );
            }
        }
    }

    private void validarRaci(DocumentoEstruturadoDTO dto, Set<String> idsEtapas, List<String> erros) {
        for (RaciEntryDTO linha : dto.raci()) {
            if (!idsEtapas.contains(linha.etapaId())) {
                erros.add("A linha RACI de " + linha.etapaId() + " referencia uma etapa que não existe no fluxo");
            }
        }
    }

    // Toda etapa (exceto a primeira) precisa ser alcançável a partir da primeira, seguindo
    // proximaEtapaId/decisao.opcoes — senão ela é "órfã" (nunca seria chegada no fluxo real).
    private void validarEtapaOrfa(DocumentoEstruturadoDTO dto, List<String> erros) {
        List<EtapaDTO> fluxo = dto.fluxo();
        if (fluxo.isEmpty()) {
            return;
        }

        Map<String, EtapaDTO> porId = new HashMap<>();
        for (EtapaDTO etapa : fluxo) {
            porId.put(etapa.id(), etapa);
        }

        Set<String> alcancadas = new HashSet<>();
        Deque<String> pendentes = new ArrayDeque<>();
        pendentes.add(fluxo.get(0).id());
        alcancadas.add(fluxo.get(0).id());

        while (!pendentes.isEmpty()) {
            EtapaDTO atual = porId.get(pendentes.poll());
            if (atual == null) {
                continue;
            }

            if (atual.proximaEtapaId() != null && alcancadas.add(atual.proximaEtapaId())) {
                pendentes.add(atual.proximaEtapaId());
            }

            if (atual.decisao() != null && atual.decisao().opcoes() != null) {
                for (DecisaoOpcaoDTO opcao : atual.decisao().opcoes()) {
                    if (opcao.proximaEtapaId() != null && alcancadas.add(opcao.proximaEtapaId())) {
                        pendentes.add(opcao.proximaEtapaId());
                    }
                }
            }
        }

        for (EtapaDTO etapa : fluxo) {
            if (!alcancadas.contains(etapa.id())) {
                erros.add(etapa.id() + " é uma etapa órfã — nenhuma outra etapa leva até ela");
            }
        }
    }
}
