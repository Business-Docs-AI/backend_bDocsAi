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

Autenticação.
Spring Security.
JWT.
Controle de acesso real.
Multi-tenancy.
Auditoria avançada.
Workflow de aprovação.
Integrações completas com plataformas externas.

Esses recursos serão implementados posteriormente, após a conclusão e validação da estrutura principal do sistema.

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
🏗️ Arquitetura

O backend está organizado seguindo uma arquitetura em camadas, separando responsabilidades por domínio.

Backend
│
├── Exceptions
│
├── Handler
│
├── AI
│   ├── Chat
│   ├── Prompt
│   ├── Embeddings
│   ├── Retrieval
│   ├── Ingestion
│   └── Generation
│
├── Documentation
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
├── Chat
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
Autenticação
Spring Security
JWT
Controle de acesso
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