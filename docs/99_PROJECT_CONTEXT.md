# Finance Manager - Project Context

> Documento central de contexto do projeto.
>
> Este arquivo representa o estado atual do projeto e deve ser atualizado ao final de cada Sprint ou decisão arquitetural importante.

---

# Visão Geral

## Objetivo

Desenvolver uma aplicação Full Stack para gerenciamento financeiro pessoal com foco em:

- aprendizado de tecnologias modernas;
- boas práticas de engenharia de software;
- arquitetura de software;
- testes automatizados;
- construção de portfólio profissional.

---

# Escopo do Produto (MVP)

O sistema permitirá:

- Cadastro de usuários
- Autenticação via JWT
- Cadastro de contas financeiras
- Cadastro de categorias
- Controle de receitas
- Controle de despesas
- Dashboard financeiro
- Importação de extratos CSV

---

# Stack Tecnológica

## Backend

- Java 21
- Spring Boot 3
- Maven
- Spring Data JPA
- Spring Security
- PostgreSQL
- JUnit 5
- Mockito
- Lombok

## Frontend

- React
- Next.js
- TypeScript

## Infraestrutura

- Docker
- Docker Compose

## Ferramentas

- Git
- GitHub
- Jira
- IntelliJ IDEA
- DBeaver

---

# Arquitetura

Arquitetura Monolítica Modular.

Estrutura prevista:

backend/

    user/

    account/

    category/

    transaction/

    auth/

    security/

    common/

Cada módulo possui:

- Controller
- Service
- Repository
- DTO
- Entity
- Mapper
- Validation

---

# Princípios Arquiteturais

Durante todo o projeto serão adotados:

- SOLID
- Clean Code
- Separation of Concerns
- Dependency Injection
- Inversão de Controle (IoC)
- Programação orientada a interfaces
- RESTful APIs

---

# Convenções

## Git

Branch principal

main

Branches de desenvolvimento

feature/nome-feature

fix/nome-fix

hotfix/nome-hotfix

Commits

Conventional Commits

Exemplo:

feat(user): create user endpoint

fix(auth): correct jwt validation

chore: initialize project structure

---

# Jira

Fluxo:

Epic

↓

Story

↓

Task

Todo

↓

In Progress

↓

Code Review

↓

Done

---

# Padrões de Código

Backend

- Injeção por construtor
- Nunca utilizar field injection
- Nunca utilizar @Data em entidades JPA
- Explicar toda anotação nova antes do uso
- Toda regra de negócio deve possuir testes
- Controller não contém regra de negócio

Frontend

- Componentes pequenos
- Componentização
- Tipagem com TypeScript
- Consumo da API através de camada de serviços

---

# Estratégia de Aprendizado

Toda tecnologia será apresentada na seguinte ordem:

1. Problema

2. Alternativas

3. Trade-offs

4. Decisão

5. Implementação

6. Revisão

Nunca implementar antes da explicação.

---

# Sprint Atual

Sprint 0

## Objetivo

Preparação do ambiente e infraestrutura do projeto.

---

# Status Atual

## Concluído

- Repositório GitHub criado

- Projeto Jira criado

- Sprint 0 criada

- EPIC-01 Project Setup criado

- User Stories iniciais criadas

- Estrutura inicial do repositório criada

- README criado

- .gitignore criado

---

## Em andamento

Configuração inicial do Backend.

---

# Próxima Story

US-002

Configurar Backend Spring Boot

---

# Próximas Tasks

- Gerar projeto Spring Boot
- Explicar Spring Framework
- Explicar Spring Boot
- Explicar Spring Initializr
- Definir dependências iniciais
- Importar projeto no IntelliJ
- Executar primeira aplicação
- Primeiro build Maven

---

# Decisões Arquiteturais

## DA-001

Arquitetura

Decisão:

Monólito Modular.

Motivo:

Menor complexidade.

Maior foco em aprendizado.

---

## DA-002

Banco de Dados

PostgreSQL.

---

## DA-003

Containerização

Docker + Docker Compose.

---

## DA-004

Gerenciamento de Dependências

Maven.

---

## DA-005

Lombok

Será utilizado.

Toda anotação será explicada antes do uso.

Não utilizar @Data em entidades.

---

# Convenções de Ensino

O ChatGPT atuará em cinco papéis distintos:

Tech Lead

Responsável por:

- arquitetura
- decisões técnicas
- planejamento
- roadmap

Backend Engineer

Responsável por:

- Spring Boot
- JPA
- Security
- testes

Frontend Tech Lead

Responsável por:

- React
- Next.js
- TypeScript

Mentor Técnico

Responsável por:

- explicações conceituais
- nivelamento técnico

Code Reviewer

Responsável por:

- revisão de código
- sugestões
- refatorações

---

# Objetivo Final

Ao término do projeto o desenvolvedor deverá ser capaz de explicar detalhadamente:

- Spring Framework
- Spring Boot
- IoC
- Dependency Injection
- Beans
- REST
- HTTP
- JPA
- Hibernate
- Spring Security
- JWT
- Docker
- Docker Compose
- Testes Unitários
- Mockito
- SOLID
- Design Patterns
- Arquitetura da aplicação

Não apenas utilizar essas tecnologias.

Mas compreender seu funcionamento interno.

---

# Histórico de Atualizações

## Sprint 0

Projeto iniciado.

Estrutura inicial criada.

Organização do Jira concluída.

README criado.

.gitignore criado.