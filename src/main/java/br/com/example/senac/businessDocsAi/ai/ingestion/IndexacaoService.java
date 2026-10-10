package br.com.example.senac.businessDocsAi.ai.ingestion;

import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusCicloVida;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import br.com.example.senac.businessDocsAi.document.entity.TipoDocumento;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * HTML → seções → chunks → embeddings → pgvector. Roda fora da transação que originou o
 * evento (disparado após o commit, em thread separada) e sempre confere se a versão que
 * está indexando ainda é a vigente, para uma execução atrasada nunca sobrescrever uma
 * versão mais nova.
 */
@Service
@RequiredArgsConstructor
public class IndexacaoService {

    private static final Logger log = LoggerFactory.getLogger(IndexacaoService.class);

    private static final int MAX_TOKENS_POR_CHUNK = 600;
    private static final int SOBREPOSICAO_TOKENS = 80;

    private final IDocumentoRepository documentoRepository;
    private final HtmlSectionSplitter htmlSectionSplitter;
    private final EmbeddingModel embeddingModel;
    private final PgVectorEmbeddingStore embeddingStore;

    @Transactional
    public void indexar(UUID documentoId, int versao) {

        DocumentoEntity documento = documentoRepository.findById(documentoId).orElse(null);

        if (documento == null || documento.isDeletado()) {
            log.info("Indexação ignorada: documento {} não existe mais ou foi excluído", documentoId);
            return;
        }

        if (documento.getVersaoAtual() != versao) {
            log.info(
                    "Indexação da versão {} do documento {} ignorada: a versão vigente já é a {}",
                    versao, documentoId, documento.getVersaoAtual()
            );
            return;
        }

        try {
            removerEmbeddingsDoDocumento(documentoId);

            List<TextSegment> segmentos = gerarSegmentos(documento);

            if (!segmentos.isEmpty()) {
                Response<List<Embedding>> embeddings = embeddingModel.embedAll(segmentos);
                List<String> ids = segmentos.stream().map(s -> UUID.randomUUID().toString()).toList();

                embeddingStore.addAll(ids, embeddings.content(), segmentos);
            }

            documento.setStatusIndexacao(StatusIndexacao.INDEXADO);
            documentoRepository.save(documento);

        } catch (Exception e) {
            log.error("Falha ao indexar o documento {} (versão {})", documentoId, versao, e);

            documento.setStatusIndexacao(StatusIndexacao.ERRO);
            documentoRepository.save(documento);
        }
    }

    @Transactional
    public void removerEmbeddings(UUID documentoId) {
        removerEmbeddingsDoDocumento(documentoId);
    }

    private void removerEmbeddingsDoDocumento(UUID documentoId) {
        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("documento_id").isEqualTo(documentoId));
    }

    private List<TextSegment> gerarSegmentos(DocumentoEntity documento) {

        List<HtmlSectionSplitter.Secao> secoes =
                htmlSectionSplitter.dividir(documento.getTitulo(), documento.getConteudoHtml());

        DocumentSplitter splitter = DocumentSplitters.recursive(MAX_TOKENS_POR_CHUNK, SOBREPOSICAO_TOKENS);

        List<TextSegment> segmentos = new ArrayList<>();

        // Etapa 14 (A1): tipo complementa o prefixo título+seção que já existia — ajuda o
        // retriever a distinguir um PROCESSO de uma POLITICA pelo texto, mesmo sem filtro.
        // NAO_CLASSIFICADO (documento legado/sem metadado real) não teria valor nenhum
        // nesse prefixo — tratado como "sem tipo", igual a null, em vez de poluir o texto
        // (e o metadado, abaixo) com um rótulo que não distingue nada.
        boolean temTipoReal = documento.getTipoDocumento() != null
                && documento.getTipoDocumento() != TipoDocumento.NAO_CLASSIFICADO;
        String tipoLegivel = temTipoReal ? documento.getTipoDocumento().name() + " " : "";

        for (HtmlSectionSplitter.Secao secao : secoes) {
            String textoComContexto = tipoLegivel + documento.getTitulo() + " > " + secao.titulo() + "\n" + secao.texto();

            Document documentoLangchain = Document.from(textoComContexto);

            for (TextSegment segmento : splitter.split(documentoLangchain)) {
                Metadata metadata = segmento.metadata().copy()
                        .put("documento_id", documento.getId())
                        .put("versao", documento.getVersaoAtual())
                        .put("titulo", documento.getTitulo())
                        .put("secao", secao.ancora());

                // Etapa 14/16: categoria_id e status_ciclo_vida são SEMPRE gravados (nunca
                // omitidos) — são os 2 metadados que o pré-filtro da Etapa 16 usa; o
                // langchain4j 1.18.0 não tem filtro IS NULL (B5), então "ausente" não é uma
                // opção segura para eles. status_ciclo_vida ausente no documento (legado/
                // NAO_CLASSIFICADO) grava o valor EFETIVO "VIGENTE" (regra "vigente por
                // padrão" — decisão 7/C4), nunca omite a chave. Os outros 3 metadados
                // continuam opcionais (omitidos quando ausentes) — não entram no pré-filtro.
                metadata.put("categoria_id", documento.getCategoriaId());
                metadata.put("status_ciclo_vida", documento.isVigente()
                        ? StatusCicloVida.VIGENTE.name()
                        : documento.getStatusCicloVida().name());
                if (temTipoReal) {
                    metadata.put("tipo_documento", documento.getTipoDocumento().name());
                }
                if (documento.getMacroprocessoId() != null) {
                    metadata.put("macroprocesso_id", documento.getMacroprocessoId());
                }
                if (documento.getConfidencialidade() != null) {
                    metadata.put("confidencialidade", documento.getConfidencialidade().name());
                }

                segmentos.add(TextSegment.from(segmento.text(), metadata));
            }
        }

        return segmentos;
    }
}
