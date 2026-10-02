Business Docs AI (bDocsAi)

Plataforma inteligente de Documentação Viva utilizando Inteligência Artificial, LLMs e RAG (Retrieval-Augmented Generation) para geração, gerenciamento e consulta de conhecimento corporativo.

📖 Sobre o Projeto

O Business Docs AI é uma plataforma desenvolvida para automatizar a criação, organização, atualização e consulta de documentações empresariais utilizando Inteligência Artificial.

A proposta é substituir documentações estáticas por uma Documentação Viva, permitindo que colaboradores interajam com uma IA por meio de texto ou áudio, transformando informações fornecidas pelos usuários em documentações estruturadas e padronizadas.

As documentações seguem um template único definido pela empresa, garantindo padronização na forma como o conhecimento é registrado.

O sistema também utiliza RAG (Retrieval-Augmented Generation) para permitir que a IA consulte o conhecimento interno armazenado e utilize essas informações para responder perguntas e auxiliar na criação e atualização das documentações.

A arquitetura também será preparada para integração com plataformas externas de documentação, como Notion, Google Docs e outras ferramentas.

🎯 Objetivos
Automatizar a criação de documentações.
Centralizar o conhecimento corporativo.
Padronizar a estrutura das documentações.
Facilitar a atualização e manutenção do conhecimento.
Permitir consultas ao conhecimento da empresa através de IA.
Reduzir o tempo gasto na criação e busca por informações.
Utilizar RAG para fornecer contexto baseado no conhecimento interno.
Preparar a plataforma para integração com ferramentas externas de documentação.
🚀 MVP

O MVP tem como foco principal a construção do backend funcional, priorizando a estrutura principal da aplicação antes da implementação de recursos complementares.

Funcionalidades principais
Cadastro de usuários.
Gerenciamento dos três perfis padrão:
ADMIN
EDITOR
USUARIO
Cadastro e gerenciamento de categorias.
Associação de categorias aos usuários.
Criação e gerenciamento de documentações.
Versionamento das documentações.
Template único de documentação.
Chat com IA.
Geração de documentação utilizando IA.
Entrada de informações por texto.
Entrada de informações por áudio.
RAG utilizando conhecimento interno.
Embeddings das documentações.
Estrutura para integrações externas.
Fora do escopo inicial do MVP

Neste primeiro momento, não serão implementados:

Multi-tenancy.
Auditoria avançada.
Workflow de aprovação.
Integrações completas com plataformas externas.

Esses recursos serão implementados posteriormente, após a conclusão e validação da estrutura principal do sistema.

> **Atualização:** autenticação, Spring Security, JWT e controle de acesso real (perfis
> ADMIN/EDITOR/USUARIO aplicados em todos os endpoints e serviços) já foram implementados
> e deixaram de ser "fora do escopo" — ver a seção de Perfis de Usuário abaixo.

👥 Perfis de Usuário

O sistema possui três perfis padrão, que não podem ser criados ou alterados pelos usuários.

ADMIN

Responsável pela administração da plataforma.

Principais responsabilidades:

Gerenciar usuários.
Associar permissões aos usuários.
Gerenciar categorias.
Gerenciar documentações.
Configurar recursos da plataforma.
Gerenciar configurações de integração.
EDITOR

Responsável pela criação e manutenção das documentações.

Principais responsabilidades:

Criar documentações.
Atualizar documentações.
Gerar documentações utilizando IA.
Enviar informações por texto ou áudio.
Consultar documentações.
Utilizar o chat com IA.
USUARIO

Consumidor do conhecimento disponibilizado pela plataforma.

Principais responsabilidades:

Consultar documentações.
Utilizar o chat com IA.
Fazer perguntas sobre o conhecimento disponível.

Observação: os perfis são fixos e fazem parte da regra de negócio da plataforma. Não existe cadastro de novos tipos de permissão no sistema.

Autenticação é feita via login (`POST /auth/login`) com e-mail e senha, que retorna um token JWT. O perfil de cada usuário fica salvo no próprio cadastro (`role`) e é recarregado do banco a cada requisição — nunca fica só guardado no token. As permissões são checadas no backend, tanto no controller quanto no service (`@PreAuthorize`), então uma tentativa de acesso direto ao endpoint sem o perfil certo retorna 403, mesmo sem nenhuma tela por trás.

📂 Categorias

As documentações são organizadas através de categorias.

As categorias possuem duas funções principais:

Organizar o conhecimento da empresa.
Auxiliar a IA na separação e recuperação das informações.

Exemplo:

Categorias

├── Recursos Humanos
├── Desenvolvimento
├── Financeiro
├── Comercial
├── Suporte
└── Operações

As categorias também podem ser associadas aos usuários para determinar o contexto de conhecimento que estará relacionado a cada usuário.

📚 Documentação Viva

A documentação é o principal recurso da plataforma.

O usuário poderá fornecer informações por:

Texto
│
└──────────┐
│
Áudio        ├──→ IA ──→ Documentação
│
Documento ───┘

A IA interpreta as informações recebidas e gera uma documentação seguindo o template padrão definido pela empresa.

O objetivo é permitir que a documentação evolua continuamente conforme novas informações são fornecidas.

🧠 Inteligência Artificial

A camada de Inteligência Artificial será implementada utilizando LangChain4j.

Ela será responsável por integrar a aplicação com modelos de linguagem (LLMs) e pelos processos relacionados ao conhecimento da plataforma.

Principais responsabilidades:

Interpretação das informações fornecidas pelos usuários.
Geração de documentações.
Atualização de documentações existentes.
Consulta ao conhecimento interno.
Recuperação de contexto.
Geração de respostas.
Geração e processamento de embeddings.
Processamento das informações utilizadas pelo RAG.
🔍 RAG

O sistema utilizará RAG (Retrieval-Augmented Generation) para permitir que a IA utilize o conhecimento armazenado na plataforma como contexto para suas respostas.

Fluxo simplificado:

Usuário
│
▼
Chat / Entrada de informação
│
▼
Processamento
│
▼
Retrieval
│
▼
Recuperação de documentos relevantes
│
▼
Construção do contexto
│
▼
Prompt
│
▼
LLM
│
▼
Resposta / Documentação

As documentações serão processadas e transformadas em embeddings, permitindo sua recuperação através de busca semântica.

🗄️ Banco de Dados

O sistema utiliza:

PostgreSQL
pgvector

O pgvector será utilizado para armazenamento e recuperação dos vetores utilizados pelo mecanismo de RAG.

Estrutura simplificada:

PostgreSQL
│
├── Dados da aplicação
│   ├── Usuários
│   ├── Categorias
│   ├── Documentações
│   ├── Versões
│   └── Integrações
│
└── Dados vetoriais
└── Embeddings

📑 Módulo de Documentação Versionada (implementado)

Documentos têm título e conteúdo em HTML (sanitizado com Jsoup antes de salvar). Toda
criação, edição ou restauração gera uma nova versão; o histórico é imutável e a versão
vigente é sempre a mais recente. Só a versão vigente fica indexada no pgvector — a
indexação roda de forma assíncrona depois do commit e confere se a versão ainda é a vigente
antes de gravar embeddings, para uma indexação atrasada nunca sobrescrever uma versão mais
nova. Um job agendado reprocessa documentos cuja indexação ficou pendente ou deu erro.

Permissões (aplicadas no backend, em endpoints e serviços):

| Ação                                         | ADMIN | EDITOR | USUARIO |
|-----------------------------------------------|-------|--------|---------|
| Visualizar documento (versão vigente)          | sim   | sim    | sim     |
| Busca semântica                                | sim   | sim    | sim     |
| Usar o próprio chat com IA                     | sim   | sim    | sim     |
| Criar e editar documentos                      | sim   | sim    | não     |
| Ver histórico de versões                       | sim   | sim    | não     |
| Restaurar versão anterior                      | sim   | não    | não     |
| Excluir documentos                             | sim   | não    | não     |
| Forçar reindexação                             | sim   | não    | não     |

Endpoints principais:

POST /documentos · PUT /documentos/{id} · DELETE /documentos/{id} · GET /documentos/{id}
GET /documentos/{id}/versoes · GET /documentos/{id}/versoes/{numero}
POST /documentos/{id}/versoes/{numero}/restaurar · POST /documentos/{id}/reindexar
GET /documentos/busca?q=...
POST /chat/conversas · GET /chat/conversas · GET /chat/conversas/{id}
POST /chat/conversas/{id}/mensagens · DELETE /chat/conversas/{id}

O chat é isolado por usuário (uma conversa de outro usuário nunca é acessível) e o
assistente de IA responde só com base nos trechos recuperados da documentação, sempre
devolvendo as fontes (documento e seção) usadas na resposta — ele não tem nenhuma
ferramenta capaz de alterar documentos, só de ler.

🏗️ Arquitetura

O backend está organizado seguindo uma arquitetura em camadas, separando responsabilidades por domínio.

Backend
│
├── Exceptions
│
├── Handler
│
├── Security (JWT, filtros, autorização)
│
├── Auth (login)
│
├── Infra (async/scheduling)
│
├── AI
│   ├── Prompt
│   ├── Embeddings
│   ├── Retrieval
│   └── Generation
│
├── Document (documento versionado + RAG)
│   ├── Controller
│   ├── Service
│   ├── Entity
│   ├── Repository
│   ├── Event
│   ├── Job
│   └── DTO
│
├── Documentation (CRUD simples legado, sem versionamento)
│   ├── Controller
│   ├── Service
│   ├── Entity
│   ├── Repository
│   └── DTO
│
├── Categories
│   ├── Controller
│   ├── Service
│   ├── Entity
│   ├── Repository
│   └── DTO
│
├── Users
│   ├── Controller
│   ├── Service
│   ├── Entity
│   ├── Repository
│   └── DTO
│
├── Chat (conversas por usuário + RAG)
│   ├── Controller
│   ├── Service
│   ├── Entity
│   ├── Repository
│   └── DTO
│
└── Integrations
├── Controller
├── Service
├── Entity
├── Repository
└── DTO
📁 Estrutura do Projeto
src
├── main
│   ├── java
│   │   └── br.com.businessdocsai
│   │       ├── Exceptions
│   │       ├── Handler
│   │       ├── AI
│   │       ├── Documentation
│   │       ├── Categories
│   │       ├── Users
│   │       ├── Chat
│   │       └── Integrations
│   │
│   └── resources
│       ├── application.yaml
│       ├── application-dev.yaml
│       ├── application-test.yaml
│       └── application-prod.yaml
│
└── test
└── java
🛠️ Tecnologias
Backend
Java 25
Spring Boot
Spring Data JPA
Spring Validation
Spring Security
JWT
Flyway
Gradle
Lombok
Inteligência Artificial
LangChain4j
LLM
RAG
Embeddings
Banco de Dados
PostgreSQL
pgvector
Documentação da API
Swagger / OpenAPI
Infraestrutura
Docker
Docker Compose
🐳 Ambiente de Desenvolvimento

O ambiente de desenvolvimento utiliza Docker para padronizar a execução do projeto.

Os principais serviços são:

Docker
│
├── Backend
│   └── Spring Boot
│
├── PostgreSQL
│   └── pgvector
│
└── pgAdmin

O ambiente atual utiliza o profile:

dev

Para iniciar o projeto pela primeira vez:

docker compose up --build

Nas execuções seguintes:

docker compose up

Caso o código ou as dependências sejam alterados:

docker compose up --build

O backend estará disponível em:

http://localhost:8080

O pgAdmin estará disponível em:

http://localhost:5050

O projeto atualmente não utiliza hot reload. Alterações no código exigem a reconstrução da imagem do backend.

Para mais detalhes sobre a configuração do ambiente, consulte a documentação de setup do projeto.

⚠️ Migrations (Flyway)

O schema do banco passou a ser controlado por migrations versionadas (Flyway), em vez do
`ddl-auto` do Hibernate criando/alterando tabelas automaticamente. Se você já tinha o
ambiente de desenvolvimento rodando antes dessa mudança, o volume do Postgres já contém as
tabelas antigas (criadas pelo `ddl-auto`) e o Flyway vai falhar ao tentar recriá-las. Rode
uma vez, para recomeçar do zero:

docker compose down -v
docker compose up --build

🔑 Variáveis de Ambiente

Copie `.env.example` para `.env` e preencha antes de subir o projeto. Além das variáveis de
banco já existentes, agora também são necessárias:

JWT_SECRET — segredo usado para assinar os tokens (HS256 exige pelo menos 256 bits/32
bytes; gere um com `openssl rand -base64 48`).
JWT_EXPIRATION_MINUTES — validade do token em minutos (padrão: 60).
AI_CHAT_PROVIDER — qual provedor de LLM usar no chat: `gemini` (padrão, tem camada
gratuita), `anthropic` ou `openai`. A troca é só configuração, sem mexer em código (ver
`ai/generation/ChatModelConfig`).
AI_EMBEDDING_PROVIDER — qual provedor usar para embeddings (indexação/busca semântica):
`local` (padrão — roda o modelo all-MiniLM-L6-v2 quantizado em processo via ONNX, sem
nenhuma chave/chamada externa) ou `openai` (ver `ai/embeddings/EmbeddingConfig`).
GEMINI_API_KEY / GEMINI_CHAT_MODEL — chave (gerada no Google AI Studio) e modelo do Gemini
usados no chat quando `AI_CHAT_PROVIDER=gemini` (padrão do modelo: `gemini-3.8-flash`).
ANTHROPIC_API_KEY / ANTHROPIC_CHAT_MODEL — chave e modelo da Anthropic usados no chat quando
`AI_CHAT_PROVIDER=anthropic` (padrão do modelo: `claude-sonnet-5`).
OPENAI_API_KEY / OPENAI_CHAT_MODEL / OPENAI_EMBEDDING_MODEL / OPENAI_EMBEDDING_DIMENSION —
só necessários se `AI_CHAT_PROVIDER=openai` e/ou `AI_EMBEDDING_PROVIDER=openai` (padrões:
`gpt-4o-mini`, `text-embedding-3-small`, `1536`). Com tudo no padrão (`gemini` + `local`),
o projeto funciona sem nenhuma chave da OpenAI.

Em ambiente de desenvolvimento (`dev`) e teste (`test`), um usuário ADMIN inicial é criado
automaticamente via migration (`admin@businessdocs.ai` / `admin123`), só para conseguir
fazer login e criar os demais usuários — essa migration não existe no perfil `prod`.

🌎 Ambientes

A aplicação está estruturada para trabalhar com três profiles:

DEV
TEST
PROD
DEV

Utilizado durante o desenvolvimento do projeto.

Desenvolvedor
↓
DEV
↓
Docker
↓
Backend + PostgreSQL + pgvector
TEST

Será utilizado para testes automatizados e testes de integração.

PROD

Será utilizado posteriormente no ambiente de produção.

A configuração dos ambientes é separada através dos arquivos:

application.yaml
application-dev.yaml
application-test.yaml
application-prod.yaml
🔗 Integrações

A arquitetura da aplicação será preparada para integração com plataformas externas de documentação.

Integrações planejadas:

Notion
Google Docs
Confluence
Microsoft SharePoint
GitHub Wiki
APIs REST

As integrações completas serão implementadas após a consolidação das funcionalidades principais do MVP.

🗺️ Roadmap
MVP
Estrutura base do backend
Configuração Docker
Configuração PostgreSQL + pgvector
Gestão de usuários
Perfis padrão
Gestão de categorias
Gestão de documentações
Versionamento
Template único de documentação
Entrada de informações por texto
Entrada de informações por áudio
Integração com LangChain4j
Integração com LLM
Geração de embeddings
Implementação do RAG
Chat com IA
Estrutura de integrações
🔮 Futuras versões
Multiempresa (Multi-tenant)
Dashboard
Auditoria
Notificações
Busca semântica avançada
Workflow de aprovação
Comentários em documentações
Sincronização automática com plataformas externas
Suporte a múltiplos provedores de IA
CI/CD
Ambiente de produção automatizado
🔄 Fluxo Principal

O fluxo conceitual da plataforma é:

Usuário
│
▼
Seleciona categoria
│
▼
Envia texto ou áudio
│
▼
Processamento da informação
│
▼
IA interpreta a solicitação
│
├───────────────┐
│               │
▼               ▼
Retrieval        Prompt
│               │
▼               │
Contexto ──────────┘
│
▼
LLM
│
▼
Documentação
│
▼
Persistência
│
▼
Embeddings
│
▼
Base de conhecimento
│
▼
Consultas futuras
🎯 Visão do Produto

O Business Docs AI tem como objetivo criar uma plataforma de Documentação Viva, onde o conhecimento corporativo deixa de ser apenas um conjunto de documentos estáticos e passa a ser uma fonte de conhecimento continuamente atualizada e acessível através de Inteligência Artificial.

A combinação de:

Documentação
+
LLMs
+
RAG
+
Embeddings
+
Integrações
↓
Documentação Viva

permite centralizar o conhecimento da organização, facilitar sua manutenção e possibilitar que colaboradores encontrem informações de forma rápida e contextualizada.

📄 Status do Projeto

Status: Em desenvolvimento — MVP

O desenvolvimento atual está concentrado no backend, priorizando a implementação e validação da estrutura principal do sistema.

Após a conclusão, estabilização e testes do backend, será iniciada a implementação do frontend.

📄 Licença

Projeto desenvolvido para fins acadêmicos e de pesquisa, com foco em:

Arquitetura de software
Inteligência Artificial
LLMs
RAG
Engenharia de software
Gestão de conhecimento