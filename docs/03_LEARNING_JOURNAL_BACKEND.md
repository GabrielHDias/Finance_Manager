# 03_LEARNING_JOURNAL_BACKEND

# Learning Journal — Fundamentos do Spring (Dia 1)

**Data:** 31/07/2026

## Objetivo

Compreender os conceitos fundamentais do ecossistema Spring antes de iniciar a implementação do projeto **Finance Manager**.

---

# Spring Framework

## O que é

O Spring Framework é um framework para desenvolvimento de aplicações Java que assume responsabilidades de infraestrutura da aplicação, permitindo que o desenvolvedor concentre seus esforços na lógica de negócio.

## Problema que resolve

Antes do Spring, o desenvolvedor precisava gerenciar manualmente:

* criação de objetos;
* conexão entre classes;
* configuração HTTP;
* acesso ao banco de dados;
* transações;
* autenticação;
* diversas configurações da aplicação.

Em aplicações grandes, isso gerava muito código repetitivo e de difícil manutenção.

## Ideia principal

O Spring gerencia a infraestrutura da aplicação.

O desenvolvedor passa a declarar quais componentes fazem parte do sistema, enquanto o framework é responsável por criá-los, configurá-los e conectá-los.

---

# Spring Boot

## O que é

O Spring Boot é um projeto construído sobre o Spring Framework.

Ele não substitui o Spring.

Seu objetivo é reduzir configurações repetitivas e facilitar a criação de aplicações Spring.

## Diferença entre Spring e Spring Boot

### Spring Framework

Fornece os recursos e a infraestrutura.

### Spring Boot

Facilita a utilização desses recursos através de convenções e configurações automáticas.

Resumo:

* Spring = infraestrutura.
* Spring Boot = facilidade de configuração.

---

# Starters

## O que são

Starters são dependências que agrupam todas as bibliotecas necessárias para uma funcionalidade específica.

Exemplos:

* `spring-boot-starter-web`
* `spring-boot-starter-data-jpa`
* `spring-boot-starter-security`

## Problema que resolvem

Evitam:

* adicionar dezenas de dependências manualmente;
* incompatibilidade entre versões;
* configuração repetitiva.

---

# Dependências Transitivas

Conceito aprendido durante o estudo dos Starters.

Quando adicionamos um Starter, o Maven baixa automaticamente todas as dependências necessárias para seu funcionamento.

Exemplo:

```text
spring-boot-starter-web

↓

Spring MVC

↓

Jackson

↓

Tomcat

↓

Logging
```

O desenvolvedor adiciona apenas uma dependência.

O Maven resolve todas as demais automaticamente.

---

# Auto Configuration

## O que é

Mecanismo do Spring Boot responsável por configurar automaticamente a aplicação.

Sua principal responsabilidade é registrar os Beans necessários.

## Como funciona

O Spring Boot analisa:

* bibliotecas presentes no classpath;
* propriedades da aplicação;
* Beans existentes;
* condições definidas pelo framework.

Quando uma condição é satisfeita, registra automaticamente os Beans correspondentes.

Fluxo estudado:

```text
Starter

↓

Dependências Transitivas

↓

Classpath

↓

Auto Configuration

↓

Beans

↓

Aplicação pronta
```

## Ideia principal

A Auto Configuration não faz "mágica".

Ela apenas aplica regras previamente definidas com base no contexto da aplicação.

---

# Beans

## Definição

Um Bean é um objeto Java cujo ciclo de vida é gerenciado pelo Container do Spring.

## Principal diferença

Objeto Java comum:

* criado pelo desenvolvedor (`new`);
* gerenciado pelo desenvolvedor.

Bean:

* criado pelo Spring;
* gerenciado pelo Spring;
* armazenado no Container.

## Frase mais importante

> Todo Bean é um objeto Java, mas nem todo objeto Java é um Bean.

---

# Componentes da aplicação x Objetos do domínio

Uma distinção importante aprendida durante o estudo.

## Normalmente são Beans

* Controllers
* Services
* Repositories
* Configurações
* Components
* PasswordEncoder
* JwtService

## Normalmente NÃO são Beans

* Usuario
* Conta
* Categoria
* Receita
* Despesa
* Transacao
* DTOs

Esses objetos representam dados da aplicação e são criados conforme necessário.

---

# Fluxo completo estudado

```text
Java

↓

Spring Framework
(Infraestrutura)

↓

Spring Boot
(Facilita configuração)

↓

Starter
(Quais bibliotecas utilizar?)

↓

Maven resolve dependências transitivas

↓

Classpath

↓

Auto Configuration

↓

Beans registrados

↓

Spring Container

↓

Aplicação pronta
```

---

# Principais aprendizados

* Spring Framework e Spring Boot possuem responsabilidades diferentes.
* Starters apenas agrupam dependências.
* A Auto Configuration configura a aplicação com base nas bibliotecas presentes.
* Beans são objetos gerenciados pelo Spring.
* Nem todo objeto da aplicação deve ser um Bean.
* Componentes da aplicação (Services, Controllers e Repositories) normalmente são Beans.
* Entidades e objetos de domínio normalmente não são Beans.

---

# Próximos estudos

* Component Scan
* `@SpringBootApplication`
* ApplicationContext
* IoC (Inversion of Control)
* Dependency Injection
* Escopos dos Beans
* Ciclo de vida dos Beans
