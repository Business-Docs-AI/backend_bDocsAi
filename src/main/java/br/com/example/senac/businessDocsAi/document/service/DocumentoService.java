package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.categories.entity.CategoryEntity;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoEstruturadoMetadadosDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoRequestDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoVersaoResponseDTO;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoAreaParticipanteEntity;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoVersaoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusCicloVida;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import br.com.example.senac.businessDocsAi.document.entity.TipoDocumento;
import br.com.example.senac.businessDocsAi.document.event.DocumentoAlteradoEvent;
import br.com.example.senac.businessDocsAi.document.event.DocumentoExcluidoEvent;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoAreaParticipanteRepository;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoVersaoRepository;
import br.com.example.senac.businessDocsAi.exception.BadRequestException;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import br.com.example.senac.businessDocsAi.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private static final int PROFUNDIDADE_MAXIMA_HIERARQUIA = 50;

    private final IDocumentoRepository documentoRepository;
    private final IDocumentoVersaoRepository documentoVersaoRepository;
    private final ICategoryRepository categoryRepository;
    private final HtmlSanitizerService htmlSanitizerService;
    private final CurrentUserProvider currentUserProvider;
    private final CategoriaAccessService categoriaAccessService;
    private final ApplicationEventPublisher eventPublisher;
    private final IDocumentoAreaParticipanteRepository documentoAreaParticipanteRepository;

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @Transactional
    public DocumentoResponseDTO criar(DocumentoRequestDTO dto) {

        categoriaAccessService.validarAcessoCategoria(dto.categoriaId());

        String autor = currentUserProvider.getCurrentUserName();
        String htmlSanitizado = htmlSanitizerService.sanitize(dto.conteudoHtml());
        String hash = calcularHash(dto.titulo(), htmlSanitizado);

        DocumentoEntity documento = new DocumentoEntity();
        documento.setTitulo(dto.titulo());
        documento.setConteudoHtml(htmlSanitizado);
        documento.setCategoriaId(dto.categoriaId());
        documento.setHashConteudo(hash);
        documento.setVersaoAtual(1);
        documento.setStatusIndexacao(StatusIndexacao.PENDENTE);
        documento.setCriadoPor(autor);
        documento.setCriadoEm(LocalDateTime.now());
        documento.setDeletado(false);
        // Explícito em Java porque o Hibernate manda NULL pra coluna não setada no objeto —
        // isso bypassaria o DEFAULT do banco (migration V9). Preserva "documento confirmado
        // pelo fluxo atual = vigente", o comportamento de hoje.
        documento.setTipoDocumento(TipoDocumento.NAO_CLASSIFICADO);
        documento.setStatusCicloVida(StatusCicloVida.VIGENTE);

        DocumentoEntity salvo = documentoRepository.save(documento);

        // Criação via DocumentoRequestDTO (fluxo legado) nunca carrega estruturado — fica
        // null, como o campo já nasce (decisão C3/B3).
        registrarNovaVersao(salvo, htmlSanitizado, autor, dto.comentarioAlteracao(), 1, null, null);

        eventPublisher.publishEvent(new DocumentoAlteradoEvent(salvo.getId(), salvo.getVersaoAtual()));

        return toResponseDTO(salvo);
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @Transactional
    public DocumentoResponseDTO atualizar(UUID id, DocumentoRequestDTO dto) {

        DocumentoEntity documento = buscarAtivoOrElseThrow(id);

        categoriaAccessService.validarAcessoCategoria(documento.getCategoriaId());
        categoriaAccessService.validarAcessoCategoria(dto.categoriaId());

        String autor = currentUserProvider.getCurrentUserName();
        String htmlSanitizado = htmlSanitizerService.sanitize(dto.conteudoHtml());
        String novoHash = calcularHash(dto.titulo(), htmlSanitizado);

        documento.setCategoriaId(dto.categoriaId());

        if (novoHash.equals(documento.getHashConteudo())) {
            documentoRepository.save(documento);
            return toResponseDTO(documento);
        }

        // Decisão C3: atualizar pelo fluxo LEGADO de HTML sempre invalida o conteúdo
        // estruturado que o documento tivesse (null explícito) — o JSON antigo continua só
        // na versão anterior, nunca é copiado pra frente por engano. DocumentoRequestDTO
        // nunca carrega estruturado, então não há como preservar aqui mesmo que quisesse.
        aplicarNovaVersao(documento, dto.titulo(), htmlSanitizado, novoHash, autor, dto.comentarioAlteracao(), null, null);

        return toResponseDTO(documento);
    }

    // Etapa 13.3 (decisão B3, confirmação da proposta estruturada gerada pelo worker
    // assíncrono): equivalente a criar(), mas grava conteudoEstruturado/versaoSchema na
    // versão 1 e aplica o bloco de metadados nas colunas do documento — nunca dentro do
    // JSON de conteúdo salvo. O conteúdo HTML já vem renderizado/sanitizado pelo worker
    // (EstruturaDocumentoHtmlRenderer já sanitiza internamente) — sanitiza de novo aqui
    // mesmo assim, pela mesma razão de sempre (nunca confiar em HTML vindo de fora deste
    // método, mesmo que a origem pareça seguir as mesmas regras).
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @Transactional
    public DocumentoResponseDTO criarComEstrutura(
            DocumentoRequestDTO dto, String conteudoEstruturadoJson, String versaoSchema,
            DocumentoEstruturadoMetadadosDTO metadados
    ) {
        categoriaAccessService.validarAcessoCategoria(dto.categoriaId());

        String autor = currentUserProvider.getCurrentUserName();
        String htmlSanitizado = htmlSanitizerService.sanitize(dto.conteudoHtml());
        String hash = calcularHash(dto.titulo(), htmlSanitizado);

        DocumentoEntity documento = new DocumentoEntity();
        documento.setTitulo(dto.titulo());
        documento.setConteudoHtml(htmlSanitizado);
        documento.setCategoriaId(dto.categoriaId());
        documento.setHashConteudo(hash);
        documento.setVersaoAtual(1);
        documento.setStatusIndexacao(StatusIndexacao.PENDENTE);
        documento.setCriadoPor(autor);
        documento.setCriadoEm(LocalDateTime.now());
        documento.setDeletado(false);
        documento.setStatusCicloVida(StatusCicloVida.VIGENTE);
        // Mesma regra de aplicarNovaVersao (usada por atualizarComEstrutura/restaurarVersao):
        // o documento espelha o conteudoEstruturado/versaoSchema da sua própria versão
        // atual, não só a linha em documento_versao — sem isto, documento.conteudo_estruturado
        // ficava sempre nulo na CRIAÇÃO estruturada (só documento_versao tinha o valor),
        // divergindo de atualizarComEstrutura/restaurarVersao (achado no teste manual E2E
        // da Etapa 13.6, modo modelo-fake).
        documento.setConteudoEstruturado(conteudoEstruturadoJson);
        documento.setVersaoSchema(versaoSchema);
        aplicarMetadadosEstruturados(documento, null, metadados);

        DocumentoEntity salvo = documentoRepository.save(documento);

        registrarNovaVersao(
                salvo, htmlSanitizado, autor, dto.comentarioAlteracao(), 1, conteudoEstruturadoJson, versaoSchema
        );
        aplicarAreasParticipantes(salvo.getId(), metadados);

        eventPublisher.publishEvent(new DocumentoAlteradoEvent(salvo.getId(), salvo.getVersaoAtual()));

        return toResponseDTO(salvo);
    }

    // Ver criarComEstrutura — mesma ideia para atualização. Diferente de atualizar() (fluxo
    // legado), aqui SEMPRE versiona (nunca pula por hash igual): confirmar uma proposta
    // estruturada é uma ação deliberada e explícita do usuário, não uma edição de rotina.
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @Transactional
    public DocumentoResponseDTO atualizarComEstrutura(
            UUID id, DocumentoRequestDTO dto, String conteudoEstruturadoJson, String versaoSchema,
            DocumentoEstruturadoMetadadosDTO metadados
    ) {
        DocumentoEntity documento = buscarAtivoOrElseThrow(id);

        categoriaAccessService.validarAcessoCategoria(documento.getCategoriaId());
        categoriaAccessService.validarAcessoCategoria(dto.categoriaId());

        String autor = currentUserProvider.getCurrentUserName();
        String htmlSanitizado = htmlSanitizerService.sanitize(dto.conteudoHtml());
        String novoHash = calcularHash(dto.titulo(), htmlSanitizado);

        documento.setCategoriaId(dto.categoriaId());
        aplicarMetadadosEstruturados(documento, id, metadados);

        aplicarNovaVersao(
                documento, dto.titulo(), htmlSanitizado, novoHash, autor, dto.comentarioAlteracao(),
                conteudoEstruturadoJson, versaoSchema
        );
        aplicarAreasParticipantes(id, metadados);

        return toResponseDTO(documento);
    }

    private void aplicarMetadadosEstruturados(
            DocumentoEntity documento, UUID documentoIdParaChecarCiclo, DocumentoEstruturadoMetadadosDTO metadados
    ) {
        if (metadados == null) {
            documento.setTipoDocumento(TipoDocumento.NAO_CLASSIFICADO);
            return;
        }

        documento.setTipoDocumento(
                metadados.tipoDocumento() != null ? metadados.tipoDocumento() : TipoDocumento.NAO_CLASSIFICADO
        );
        documento.setMacroprocessoId(metadados.macroprocessoId());

        if (metadados.processoPaiId() != null) {
            // documentoIdParaChecarCiclo é null na criação (o ID ainda não existe — não há
            // como um documento inexistente já ser ancestral de ninguém, então o
            // autorreferência/ciclo é trivialmente impossível nesse caso).
            validarHierarquiaProcesso(documentoIdParaChecarCiclo, metadados.processoPaiId());
        }
        documento.setProcessoPaiId(metadados.processoPaiId());

        documento.setDonoProcesso(metadados.donoProcesso());
        documento.setAprovador(metadados.aprovador());
        documento.setPeriodicidadeRevisaoMeses(metadados.periodicidadeRevisaoMeses());
        documento.setConfidencialidade(metadados.confidencialidade());
        documento.setTags(metadados.tags());
    }

    // Substitui por completo o conjunto de áreas participantes pelo que a proposta trouxe —
    // nunca usado para controle de acesso (ver DocumentoAreaParticipanteEntity).
    private void aplicarAreasParticipantes(UUID documentoId, DocumentoEstruturadoMetadadosDTO metadados) {
        documentoAreaParticipanteRepository.deleteByDocumentoId(documentoId);

        if (metadados == null || metadados.areasParticipantes() == null) {
            return;
        }

        for (Long categoriaId : metadados.areasParticipantes()) {
            documentoAreaParticipanteRepository.save(new DocumentoAreaParticipanteEntity(documentoId, categoriaId));
        }
    }

    @PreAuthorize("isAuthenticated()")
    public List<DocumentoResponseDTO> listar(Long categoriaId) {

        if (categoriaId != null) {
            categoriaAccessService.validarAcessoCategoria(categoriaId);
        }

        List<DocumentoEntity> documentos = categoriaId != null
                ? documentoRepository.findByDeletadoFalseAndCategoriaIdOrderByTituloAsc(categoriaId)
                : documentoRepository.findByDeletadoFalseOrderByTituloAsc();

        List<DocumentoEntity> acessiveis = documentos.stream()
                .filter(documento -> categoriaAccessService.podeAcessarCategoria(documento.getCategoriaId()))
                .toList();

        Map<Long, String> nomesCategorias = carregarNomesCategorias(acessiveis);
        Set<UUID> idsDeProcessoPaiVisiveis = carregarIdsDeProcessoPaiVisiveis(acessiveis);

        return acessiveis.stream()
                .map(documento -> {
                    UUID processoPaiVisivel = documento.getProcessoPaiId() != null
                            && idsDeProcessoPaiVisiveis.contains(documento.getProcessoPaiId())
                            ? documento.getProcessoPaiId()
                            : null;

                    return montarResponseDTO(
                            documento, nomesCategorias.get(documento.getCategoriaId()), processoPaiVisivel
                    );
                })
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public DocumentoResponseDTO restaurarVersao(UUID id, int numeroVersao) {

        DocumentoEntity documento = buscarAtivoOrElseThrow(id);

        DocumentoVersaoEntity versaoAlvo = documentoVersaoRepository
                .findByDocumentoIdAndNumeroVersao(id, numeroVersao)
                .orElseThrow(() -> new NotFoundException(
                        "Versão " + numeroVersao + " não encontrada para o documento " + id));

        String autor = currentUserProvider.getCurrentUserName();
        String htmlSanitizado = htmlSanitizerService.sanitize(versaoAlvo.getConteudoHtml());
        String novoHash = calcularHash(versaoAlvo.getTitulo(), htmlSanitizado);

        if (novoHash.equals(documento.getHashConteudo())) {
            return toResponseDTO(documento);
        }

        String comentario = "Restauração da versão " + numeroVersao;
        // Restaurar traz de volta o conteúdo estruturado DAQUELA versão (se ela tinha) —
        // diferente de atualizar() pelo fluxo legado, aqui não é uma edição às cegas, é
        // voltar pra um estado que já existiu, íntegro.
        aplicarNovaVersao(
                documento, versaoAlvo.getTitulo(), htmlSanitizado, novoHash, autor, comentario,
                versaoAlvo.getConteudoEstruturado(), versaoAlvo.getVersaoSchema()
        );

        return toResponseDTO(documento);
    }

    // Sem fluxo de aprovação (EM_ELABORACAO→EM_REVISAO→VIGENTE→OBSOLETO) — adiado de
    // propósito, ver docs/plano-documentacao-estruturada.md. Troca direta, sem validação de
    // transição. É uma mudança só de metadado: não versiona conteúdo, só atualiza
    // atualizado_por/atualizado_em. O gatilho de reindexação por essa mudança é da Etapa 17
    // — não disparado ainda aqui, de propósito (esta etapa só introduz o caminho em si).
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public DocumentoResponseDTO atualizarStatusCicloVida(UUID id, StatusCicloVida novoStatus) {

        DocumentoEntity documento = buscarAtivoOrElseThrow(id);
        categoriaAccessService.validarAcessoCategoria(documento.getCategoriaId());

        documento.setStatusCicloVida(novoStatus);
        documento.setAtualizadoPor(currentUserProvider.getCurrentUserName());
        documento.setAtualizadoEm(LocalDateTime.now());
        documentoRepository.save(documento);

        // Etapa 17 (B4, decisão 7): status_ciclo_vida é metadado do CHUNK (Etapa 14) e do
        // pré-filtro do RAG (Etapa 16) — essa troca não versiona o documento (decisão
        // original do endpoint, Etapa 5), mas precisa reindexar pra sincronizar o metadado
        // nos chunks já existentes. Sempre ligado (B4) — não atrás de nenhuma flag de RAG.
        // Mesma versaoAtual de sempre: IndexacaoService.indexar() só confere que ainda é a
        // vigente, nunca exige que tenha mudado.
        eventPublisher.publishEvent(new DocumentoAlteradoEvent(documento.getId(), documento.getVersaoAtual()));

        return toResponseDTO(documento);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void excluir(UUID id) {

        DocumentoEntity documento = buscarAtivoOrElseThrow(id);

        documento.setDeletado(true);
        documento.setExcluidoEm(LocalDateTime.now());
        documentoRepository.save(documento);

        eventPublisher.publishEvent(new DocumentoExcluidoEvent(documento.getId()));
    }

    @PreAuthorize("isAuthenticated()")
    public DocumentoResponseDTO buscarPorId(UUID id) {
        DocumentoEntity documento = buscarAtivoOrElseThrow(id);
        categoriaAccessService.validarAcessoCategoria(documento.getCategoriaId());
        return toResponseDTO(documento);
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public List<DocumentoVersaoResponseDTO> listarVersoes(UUID id) {

        DocumentoEntity documento = buscarAtivoOrElseThrow(id);
        categoriaAccessService.validarAcessoCategoria(documento.getCategoriaId());

        return documentoVersaoRepository.findByDocumentoIdOrderByNumeroVersaoDesc(id)
                .stream()
                .map(this::toVersaoResponseDTO)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public DocumentoVersaoResponseDTO buscarVersao(UUID id, int numeroVersao) {

        DocumentoEntity documento = buscarAtivoOrElseThrow(id);
        categoriaAccessService.validarAcessoCategoria(documento.getCategoriaId());

        DocumentoVersaoEntity versao = documentoVersaoRepository
                .findByDocumentoIdAndNumeroVersao(id, numeroVersao)
                .orElseThrow(() -> new NotFoundException(
                        "Versão " + numeroVersao + " não encontrada para o documento " + id));

        return toVersaoResponseDTO(versao);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void reindexar(UUID id) {

        DocumentoEntity documento = buscarAtivoOrElseThrow(id);

        documento.setStatusIndexacao(StatusIndexacao.PENDENTE);
        documentoRepository.save(documento);

        eventPublisher.publishEvent(new DocumentoAlteradoEvent(documento.getId(), documento.getVersaoAtual()));
    }

    // Valida um vínculo de hierarquia de processo ANTES de aplicá-lo a um documento —
    // rejeita auto-referência e qualquer ciclo (A→B→A). Reaproveitada pela Etapa 13 ao
    // aplicar metadados estruturados; nenhum caminho hoje seta processoPaiId ainda, então
    // não há chamador em produção nesta etapa.
    public void validarHierarquiaProcesso(UUID documentoId, UUID novoProcessoPaiId) {
        if (novoProcessoPaiId == null) {
            return;
        }

        if (novoProcessoPaiId.equals(documentoId)) {
            throw new BadRequestException("Um documento não pode ser pai de si mesmo.");
        }

        UUID atual = novoProcessoPaiId;
        for (int i = 0; i < PROFUNDIDADE_MAXIMA_HIERARQUIA; i++) {
            DocumentoEntity pai = documentoRepository.findById(atual).orElse(null);
            if (pai == null || pai.getProcessoPaiId() == null) {
                return;
            }
            if (pai.getProcessoPaiId().equals(documentoId)) {
                throw new BadRequestException("Esse vínculo de hierarquia formaria um ciclo.");
            }
            atual = pai.getProcessoPaiId();
        }

        throw new BadRequestException("Hierarquia de processo excede a profundidade máxima permitida.");
    }

    // Compartilhado por atualizar/restaurar: incrementa a versão vigente, grava o histórico
    // e dispara a reindexação. Criação não passa por aqui pois a versão 1 não "incrementa" nada.
    // conteudoEstruturado/versaoSchema são decididos pelo CHAMADOR (decisão B3/C3) — este
    // método só grava o que recebe, nunca decide sozinho se deve preservar ou invalidar.
    private void aplicarNovaVersao(
            DocumentoEntity documento,
            String titulo,
            String htmlSanitizado,
            String hash,
            String autor,
            String comentario,
            String conteudoEstruturado,
            String versaoSchema
    ) {
        int novaVersao = documento.getVersaoAtual() + 1;

        documento.setTitulo(titulo);
        documento.setConteudoHtml(htmlSanitizado);
        documento.setHashConteudo(hash);
        documento.setVersaoAtual(novaVersao);
        documento.setStatusIndexacao(StatusIndexacao.PENDENTE);
        documento.setAtualizadoPor(autor);
        documento.setAtualizadoEm(LocalDateTime.now());
        documento.setConteudoEstruturado(conteudoEstruturado);
        documento.setVersaoSchema(versaoSchema);

        documentoRepository.save(documento);

        registrarNovaVersao(documento, htmlSanitizado, autor, comentario, novaVersao, conteudoEstruturado, versaoSchema);

        eventPublisher.publishEvent(new DocumentoAlteradoEvent(documento.getId(), novaVersao));
    }

    private void registrarNovaVersao(
            DocumentoEntity documento,
            String htmlSanitizado,
            String autor,
            String comentario,
            int numeroVersao,
            String conteudoEstruturado,
            String versaoSchema
    ) {
        DocumentoVersaoEntity versao = new DocumentoVersaoEntity();
        versao.setDocumentoId(documento.getId());
        versao.setNumeroVersao(numeroVersao);
        versao.setTitulo(documento.getTitulo());
        versao.setConteudoHtml(htmlSanitizado);
        versao.setHashConteudo(documento.getHashConteudo());
        versao.setAutor(autor);
        versao.setCriadoEm(LocalDateTime.now());
        versao.setComentarioAlteracao(comentario);
        versao.setConteudoEstruturado(conteudoEstruturado);
        versao.setVersaoSchema(versaoSchema);

        documentoVersaoRepository.save(versao);
    }

    private DocumentoEntity buscarAtivoOrElseThrow(UUID id) {

        DocumentoEntity documento = documentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Documento não encontrado com o ID: " + id));

        if (documento.isDeletado()) {
            throw new NotFoundException("Documento não encontrado com o ID: " + id);
        }

        return documento;
    }

    private String calcularHash(String titulo, String htmlSanitizado) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(titulo.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(htmlSanitizado.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível", e);
        }
    }

    private DocumentoResponseDTO toResponseDTO(DocumentoEntity documento) {
        String categoriaNome = documento.getCategoriaId() == null
                ? null
                : categoryRepository.findById(documento.getCategoriaId())
                        .map(CategoryEntity::getName)
                        .orElse(null);

        return toResponseDTO(documento, categoriaNome);
    }

    private DocumentoResponseDTO toResponseDTO(DocumentoEntity documento, String categoriaNome) {
        UUID processoPaiVisivel = resolverProcessoPaiVisivel(documento.getProcessoPaiId());
        return montarResponseDTO(documento, categoriaNome, processoPaiVisivel);
    }

    private DocumentoResponseDTO montarResponseDTO(
            DocumentoEntity documento, String categoriaNome, UUID processoPaiVisivel
    ) {
        return new DocumentoResponseDTO(
                documento.getId(),
                documento.getTitulo(),
                documento.getConteudoHtml(),
                documento.getVersaoAtual(),
                documento.getStatusIndexacao(),
                documento.getCriadoPor(),
                documento.getCriadoEm(),
                documento.getAtualizadoPor(),
                documento.getAtualizadoEm(),
                documento.getCategoriaId(),
                categoriaNome,
                documento.getTipoDocumento(),
                documento.getStatusCicloVida(),
                documento.getDonoProcesso(),
                documento.getAprovador(),
                documento.getDataVigencia(),
                documento.getProximaRevisao(),
                documento.getPeriodicidadeRevisaoMeses(),
                documento.getConfidencialidade(),
                documento.getTags(),
                documento.getMacroprocessoId(),
                processoPaiVisivel
        );
    }

    // Um pai soft-deletado nunca é exposto como vínculo válido — ver DocumentoEntity.processoPaiId.
    private UUID resolverProcessoPaiVisivel(UUID processoPaiId) {
        if (processoPaiId == null) {
            return null;
        }
        return documentoRepository.findById(processoPaiId)
                .filter(pai -> !pai.isDeletado())
                .map(DocumentoEntity::getId)
                .orElse(null);
    }

    // Mesma regra de resolverProcessoPaiVisivel, só que em lote — evita N+1 ao listar.
    private Set<UUID> carregarIdsDeProcessoPaiVisiveis(List<DocumentoEntity> documentos) {
        List<UUID> paisReferenciados = documentos.stream()
                .map(DocumentoEntity::getProcessoPaiId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (paisReferenciados.isEmpty()) {
            return Set.of();
        }

        return documentoRepository.findAllById(paisReferenciados).stream()
                .filter(pai -> !pai.isDeletado())
                .map(DocumentoEntity::getId)
                .collect(Collectors.toSet());
    }

    private Map<Long, String> carregarNomesCategorias(List<DocumentoEntity> documentos) {
        List<Long> categoriaIds = documentos.stream()
                .map(DocumentoEntity::getCategoriaId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Long, String> nomes = new HashMap<>();
        categoryRepository.findAllById(categoriaIds)
                .forEach(categoria -> nomes.put(categoria.getId(), categoria.getName()));

        return nomes;
    }

    private DocumentoVersaoResponseDTO toVersaoResponseDTO(DocumentoVersaoEntity versao) {
        return new DocumentoVersaoResponseDTO(
                versao.getId(),
                versao.getNumeroVersao(),
                versao.getTitulo(),
                versao.getConteudoHtml(),
                versao.getAutor(),
                versao.getCriadoEm(),
                versao.getComentarioAlteracao()
        );
    }
}
