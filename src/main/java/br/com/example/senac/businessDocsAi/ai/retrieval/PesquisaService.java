package br.com.example.senac.businessDocsAi.ai.retrieval;

import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.document.dto.ResultadoBuscaDTO;
import br.com.example.senac.businessDocsAi.document.dto.TrechoDTO;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Busca semântica sobre a versão vigente dos documentos, agrupando vários trechos do
 * mesmo documento em um único resultado (ordenado pelo melhor score encontrado).
 */
@Service
@RequiredArgsConstructor
public class PesquisaService {

    private static final int MAX_RESULTADOS_BRUTOS = 20;
    private static final double SCORE_MINIMO = 0.5;
    private static final int MAX_TRECHOS_POR_DOCUMENTO = 3;

    private final EmbeddingModel embeddingModel;
    private final PgVectorEmbeddingStore embeddingStore;
    private final IDocumentoRepository documentoRepository;
    private final CategoriaAccessService categoriaAccessService;

    @PreAuthorize("isAuthenticated()")
    public List<ResultadoBuscaDTO> buscar(String query) {

        Embedding embeddingConsulta = embeddingModel.embed(query).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingConsulta)
                .maxResults(MAX_RESULTADOS_BRUTOS)
                .minScore(SCORE_MINIMO)
                .build();

        EmbeddingSearchResult<TextSegment> resultado = embeddingStore.search(request);

        return agrupar(resultado.matches());
    }

    private List<ResultadoBuscaDTO> agrupar(List<EmbeddingMatch<TextSegment>> matches) {

        Map<UUID, List<EmbeddingMatch<TextSegment>>> porDocumento = matches.stream()
                .collect(Collectors.groupingBy(
                        match -> match.embedded().metadata().getUUID("documento_id"),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ResultadoBuscaDTO> resultados = new ArrayList<>();

        for (Map.Entry<UUID, List<EmbeddingMatch<TextSegment>>> entry : porDocumento.entrySet()) {
            UUID documentoId = entry.getKey();

            if (!documentoAcessivel(documentoId)) {
                continue;
            }

            List<EmbeddingMatch<TextSegment>> matchesDoDocumento = entry.getValue().stream()
                    .sorted(Comparator.comparingDouble((EmbeddingMatch<TextSegment> m) -> m.score()).reversed())
                    .toList();

            EmbeddingMatch<TextSegment> melhor = matchesDoDocumento.get(0);
            String titulo = melhor.embedded().metadata().getString("titulo");

            List<TrechoDTO> trechos = matchesDoDocumento.stream()
                    .limit(MAX_TRECHOS_POR_DOCUMENTO)
                    .map(match -> {
                        String secao = match.embedded().metadata().getString("secao");
                        return new TrechoDTO(
                                secao,
                                match.embedded().text(),
                                match.score(),
                                "/documentos/" + documentoId + "#" + secao
                        );
                    })
                    .toList();

            resultados.add(new ResultadoBuscaDTO(documentoId, titulo, melhor.score(), trechos));
        }

        resultados.sort(Comparator.comparingDouble(ResultadoBuscaDTO::melhorScore).reversed());

        return resultados;
    }

    private boolean documentoAcessivel(UUID documentoId) {
        return documentoRepository.findById(documentoId)
                .filter(documento -> !documento.isDeletado())
                .map(documento -> categoriaAccessService.podeAcessarCategoria(documento.getCategoriaId()))
                .orElse(false);
    }
}
