# 04_LEARNING_JOURNAL_FRONTEND

# Learning Journal — Fundamentos da Web (Bloco 1)

**Data:** 02/08/2026

## Objetivo

Compreender como uma aplicação Web funciona antes de iniciar o desenvolvimento Frontend do projeto **Finance Manager**.

O foco deste bloco foi entender toda a comunicação entre usuário, navegador, servidor e backend, bem como os protocolos e padrões utilizados nessa comunicação.

---

# Internet

## O que é

A Internet é uma rede de redes, composta por milhares de redes independentes interligadas através de protocolos comuns.

Ela permite que dispositivos localizados em diferentes partes do mundo consigam se comunicar.

## Problema que resolve

Permitir a comunicação entre computadores pertencentes a diferentes redes.

Antes da Internet, muitas redes eram isoladas e não existia uma forma padronizada de comunicação global.

## Ideia principal

A Internet conecta redes.

Ela não é um único computador nem uma única rede.

---

# Cliente e Servidor

## O que são

### Cliente

É o dispositivo ou software responsável por solicitar um serviço.

Exemplos:

- Navegador
- Aplicativo mobile
- Postman

### Servidor

É o computador ou software responsável por fornecer um serviço ao cliente.

## Ideia principal

Toda aplicação Web funciona através da comunicação entre Cliente e Servidor.

---

# Endereço IP

## O que é

É um identificador utilizado para localizar um dispositivo conectado à rede.

## Problema que resolve

Permite que uma requisição encontre exatamente o computador de destino.

## Ideia principal

O IP funciona como o endereço de uma residência.

Sem ele, não seria possível localizar o servidor.

---

# DNS

## O que é

DNS (Domain Name System) é o sistema responsável por converter nomes de domínio em endereços IP.

## Problema que resolve

Evita que usuários precisem memorizar endereços IP.

## Exemplo

```text
google.com

↓

DNS

↓

142.xxx.xxx.xxx
```

## Ideia principal

Usuários trabalham com nomes.

Computadores trabalham com IPs.

O DNS realiza essa tradução.

---

# HTTP

## O que é

HTTP (HyperText Transfer Protocol) é o protocolo utilizado para comunicação entre clientes e servidores na Web.

## Problema que resolve

Padroniza a troca de informações entre diferentes aplicações.

## Estrutura

Toda comunicação HTTP é composta por:

- Request (Requisição)
- Response (Resposta)

## Stateless

HTTP é stateless.

Cada requisição é independente das anteriores.

O servidor não assume que conhece o cliente apenas porque ele realizou outra requisição anteriormente.

---

# Métodos HTTP

## O que são

São operações padronizadas que indicam a intenção da requisição.

## Métodos estudados

### GET

Consulta recursos.

### POST

Cria novos recursos.

### PUT

Atualiza completamente um recurso existente.

### DELETE

Remove recursos.

## Ideia principal

O recurso permanece o mesmo.

O método HTTP representa a operação desejada.

Exemplo:

```text
GET    /transacoes
POST   /transacoes
PUT    /transacoes/15
DELETE /transacoes/15
```

---

# Status Codes

## O que são

Códigos padronizados utilizados pelo servidor para informar o resultado de uma requisição HTTP.

## Principais códigos estudados

### 200 OK

Requisição executada com sucesso.

### 201 Created

Novo recurso criado.

### 400 Bad Request

Erro causado pela requisição do cliente.

### 401 Unauthorized

Usuário não autenticado.

### 403 Forbidden

Usuário autenticado, porém sem permissão.

### 404 Not Found

Recurso não encontrado.

### 500 Internal Server Error

Erro interno do servidor.

## Ideia principal

Os Status Codes permitem que cliente e servidor interpretem o resultado de uma operação de forma padronizada.

---

# HTTPS

## O que é

Versão segura do HTTP.

Utiliza criptografia para proteger os dados durante o transporte.

## Problema que resolve

Evita que informações sensíveis sejam lidas durante sua transmissão pela Internet.

## Certificado Digital

O certificado digital permite que o navegador valide a identidade do servidor antes de estabelecer a conexão segura.

## Ideia principal

HTTPS protege a comunicação.

Ele não protege os dados armazenados no banco de dados.

---

# REST

## O que é

REST (Representational State Transfer) é um estilo arquitetural utilizado para construção de APIs.

## Problema que resolve

Padroniza a organização de APIs, facilitando integração, manutenção e entendimento.

## Características

- baseado em recursos;
- utiliza HTTP;
- utiliza Métodos HTTP;
- utiliza Status Codes;
- é stateless.

## Ideia principal

As URLs representam recursos.

As operações são representadas pelos métodos HTTP.

---

# JSON

## O que é

JSON (JavaScript Object Notation) é um formato de representação de dados.

## Problema que resolve

Padroniza a troca de informações entre diferentes sistemas.

## Estrutura

JSON é composto por pares:

```text
chave

↓

valor
```

Exemplo:

```json
{
  "descricao": "Salário",
  "valor": 1500,
  "categoria": "Receita"
}
```

## Ideia principal

HTTP transporta os dados.

JSON representa esses dados.

---

# Fluxo completo estudado

```text
Usuário

↓

Navegador (Cliente)

↓

DNS

↓

Servidor

↓

HTTPS

↓

HTTP Request

↓

Método HTTP

↓

Backend

↓

Banco de Dados

↓

Status Code + JSON

↓

HTTP Response

↓

Navegador

↓

Usuário
```

---

# Principais aprendizados

- A Internet é uma rede de redes.
- Cliente e Servidor possuem responsabilidades diferentes.
- O DNS traduz domínios em endereços IP.
- HTTP padroniza a comunicação entre cliente e servidor.
- Métodos HTTP representam operações sobre recursos.
- Status Codes informam o resultado das requisições.
- HTTPS protege os dados durante o transporte através de criptografia.
- REST organiza APIs utilizando recursos e métodos HTTP.
- JSON é o principal formato de troca de dados entre Frontend e Backend.
- Uma aplicação Web moderna é construída pela integração de todos esses conceitos.

---

# Próximos estudos

- HTML
- Estrutura de uma página
- Elementos HTML
- Tags semânticas
- Formulários
- Inputs
- Acessibilidade
- SEO básico

# Learning Journal — HTML (Bloco 2)

**Data:** 02/08/2026

## Objetivo

Compreender como o navegador interpreta um documento HTML e como estruturar páginas Web de forma semântica, acessível e preparada para futuras estilizações com CSS e comportamentos com JavaScript.

O foco deste bloco foi entender que HTML não é responsável pela aparência da página, mas sim por representar corretamente o significado de cada informação.

---

# HTML

## O que é

HTML (HyperText Markup Language) é uma linguagem de marcação utilizada para estruturar documentos Web.

Seu objetivo é descrever o significado dos elementos da página para que navegadores, mecanismos de busca e tecnologias assistivas consigam interpretá-los corretamente.

## Problema que resolve

Transformar conteúdo textual em uma estrutura compreensível para o navegador.

Sem HTML, o navegador receberia apenas texto, sem conseguir identificar títulos, parágrafos, imagens, formulários e demais elementos da interface.

## Ideia principal

HTML descreve a estrutura e o significado da informação.

Ele não define aparência nem comportamento.

---

# Estrutura de um documento HTML

## Elementos estudados

- `<!DOCTYPE html>`
- `<html>`
- `<head>`
- `<body>`

## Principais aprendizados

### `<!DOCTYPE html>`

Informa ao navegador que o documento deve ser interpretado utilizando o padrão HTML5.

### `<html>`

Elemento raiz que contém todo o documento.

### `<head>`

Contém metadados e configurações da página.

Exemplos:

- título da aba;
- favicon;
- configuração de responsividade;
- arquivos CSS;
- metadados.

### `<body>`

Contém todo o conteúdo visível da página.

---

# Headings

## Elementos estudados

- `<h1>` até `<h6>`

## Problema que resolvem

Representar a hierarquia do conteúdo.

## Principais aprendizados

- Heading não representa tamanho de fonte.
- Heading representa importância e hierarquia.
- Auxilia SEO.
- Auxilia leitores de tela.
- Deve seguir uma hierarquia lógica.

---

# Parágrafos

## Elemento estudado

- `<p>`

## Problema que resolve

Representar textos corridos dentro do documento.

## Principais aprendizados

- Não deve ser utilizado apenas para criar espaçamento.
- Representa um parágrafo semântico.

---

# Links

## Elemento estudado

- `<a>`

## Atributo estudado

- `href`

## Problema que resolve

Permitir navegação entre documentos.

## Principais aprendizados

- HyperText representa documentos conectados.
- `<a>` representa um hyperlink.
- `href` informa o destino.
- Existem links absolutos e relativos.
- Link representa navegação.

---

# Listas

## Elementos estudados

- `<ul>`
- `<ol>`
- `<li>`

## Problema que resolvem

Representar coleções de informações.

## Principais aprendizados

### `<ul>`

Lista cuja ordem não possui significado.

### `<ol>`

Lista cuja ordem possui significado.

### `<li>`

Representa um item pertencente à lista.

---

# Imagens

## Elemento estudado

- `<img>`

## Atributos estudados

- `src`
- `alt`

## Problema que resolve

Permitir que páginas referenciem imagens externas.

## Principais aprendizados

### `src`

Informa onde o navegador deve buscar a imagem.

### `alt`

Descreve a imagem.

Melhora:

- acessibilidade;
- SEO;
- experiência do usuário quando a imagem não é carregada.

Também foi compreendido que uma página HTML realiza múltiplas requisições HTTP para carregar recursos externos.

---

# Tabelas

## Elementos estudados

- `<table>`
- `<tr>`
- `<th>`
- `<td>`

## Problema que resolvem

Representar dados tabulares.

## Principais aprendizados

### `<table>`

Representa uma tabela.

### `<tr>`

Representa uma linha.

### `<th>`

Representa uma célula de cabeçalho.

### `<td>`

Representa uma célula de dados.

Também foi aprendido que tabelas não devem ser utilizadas para construção de layouts.

---

# HTML Semântico

## Elementos estudados

- `<header>`
- `<nav>`
- `<main>`
- `<section>`
- `<article>`
- `<aside>`
- `<footer>`
- `<div>`

## Problema que resolve

Dar significado estrutural às diferentes áreas da página.

## Principais aprendizados

### `<header>`

Representa o cabeçalho.

### `<nav>`

Representa a navegação.

### `<main>`

Representa o conteúdo principal da página.

Existe apenas um `<main>` por documento.

### `<section>`

Representa uma seção temática.

### `<article>`

Representa um conteúdo independente.

### `<aside>`

Representa conteúdo complementar.

### `<footer>`

Representa o rodapé.

### `<div>`

Representa apenas uma divisão genérica.

Deve ser utilizada apenas quando não existir um elemento semântico mais adequado.

---

# Formulários

## Elementos estudados

- `<form>`
- `<label>`
- `<input>`
- `<textarea>`
- `<select>`
- `<option>`
- `<button>`

## Problema que resolvem

Permitir entrada de dados pelo usuário.

## Principais aprendizados

### `<form>`

Agrupa campos relacionados.

### `<label>`

Identifica semanticamente um campo de entrada.

### `<input>`

Recebe informações curtas.

Tipos estudados:

- `text`
- `email`
- `password`
- `number`
- `date`

### `<textarea>`

Recebe textos longos.

### `<select>`

Permite selecionar uma opção previamente definida.

### `<option>`

Representa uma opção de um `<select>`.

### `<button>`

Representa uma ação.

Foi reforçada a diferença entre:

- `<a>` → navegação.
- `<button>` → execução de ações.

---

# Acessibilidade

## O que é

Desenvolver aplicações utilizáveis pelo maior número possível de pessoas, incluindo usuários com diferentes tipos de deficiência.

## Principais aprendizados

HTML semântico melhora significativamente a acessibilidade.

Elementos importantes:

- headings;
- `<label>`;
- `alt`;
- `<nav>`;
- `<main>`;
- `<header>`;
- `<footer>`.

Também foi estudado o funcionamento básico dos leitores de tela e como eles utilizam a estrutura HTML.

---

# SEO Básico

## O que é

SEO (Search Engine Optimization) consiste em técnicas que facilitam a compreensão da página por mecanismos de busca.

## Principais aprendizados

Elementos semânticos auxiliam buscadores a compreender:

- tema principal;
- organização do conteúdo;
- hierarquia da página.

Também foi estudada a importância de:

- `<title>`;
- headings;
- `alt`;
- estrutura semântica.

---

# Conceitos Fundamentais

## HTML descreve significado

A principal característica do HTML é representar o significado da informação.

Ele não define aparência nem comportamento.

---

## Separação de responsabilidades

HTML

→ Estrutura.

CSS

→ Aparência.

JavaScript

→ Comportamento.

---

## Semântica

Sempre escolher o elemento que melhor representa o significado da informação.

Antes de utilizar uma `<div>`, verificar se existe um elemento semântico mais adequado.

---

## Acessibilidade

Uma estrutura HTML correta melhora significativamente a experiência de usuários que utilizam tecnologias assistivas.

---

## SEO

Mecanismos de busca utilizam a estrutura HTML para compreender melhor o conteúdo da página.

---

# Aplicação no Finance Manager

Os conhecimentos deste bloco serão utilizados em praticamente todas as telas do projeto.

Exemplos:

- Tela de Login;
- Dashboard;
- Cadastro de Transações;
- Cadastro de Categorias;
- Perfil do Usuário;
- Relatórios.

Durante o desenvolvimento serão utilizados:

- HTML semântico;
- formulários estruturados;
- tabelas para dados financeiros;
- imagens acessíveis;
- navegação organizada;
- estrutura preparada para CSS e React.

---

# Principais Aprendizados

- HTML representa significado, não aparência.
- O navegador interpreta a estrutura HTML antes de renderizar a interface.
- Cada elemento HTML possui uma responsabilidade específica.
- Semântica melhora legibilidade, manutenção, acessibilidade e SEO.
- Formulários representam a principal forma de entrada de dados em aplicações Web.
- A escolha correta das tags faz parte da Engenharia de Software e não apenas da aparência da página.

---

# Objetivos Alcançados

- Compreender o funcionamento do HTML.
- Entender a estrutura de um documento HTML.
- Utilizar corretamente headings e parágrafos.
- Compreender links e atributos.
- Representar listas, imagens e tabelas.
- Entender HTML semântico.
- Construir formulários utilizando os elementos adequados.
- Compreender acessibilidade.
- Compreender SEO básico.
- Diferenciar claramente estrutura, aparência e comportamento.

---

# Próximo Bloco

## Bloco 3 — CSS

### Objetivo

Aprender como transformar a estrutura HTML em interfaces modernas, organizadas e responsivas.

Ao final do próximo bloco, serei capaz de:

- compreender a cascata;
- entender especificidade;
- dominar Box Model;
- utilizar Flexbox;
- utilizar CSS Grid;
- criar layouts responsivos;
- compreender Media Queries;
- construir interfaces profissionais para o Finance Manager.