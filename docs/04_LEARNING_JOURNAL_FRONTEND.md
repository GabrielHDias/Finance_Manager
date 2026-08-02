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