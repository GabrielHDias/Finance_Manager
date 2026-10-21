# Finance Manager

Aplicação de gerenciamento financeiro pessoal e projeto de aprendizado Full Stack e DevOps, com Java 21, Spring Boot, Next.js, TypeScript e PostgreSQL 17.

O MVP inclui cadastro e autenticação, contas financeiras, categorias, receitas e despesas, edição e exclusão, filtros de transações e dashboard por período. Os saldos são calculados pelas transações, e os dados pertencem ao usuário autenticado. A importação CSV oferece prévia, validação por linha e prevenção de duplicados. A exportação CSV gera relatórios com os filtros aplicados.

## Preparar as variáveis

Pré-requisitos: Node.js 24 e Docker com Docker Compose v2.20 ou superior. Java 21 é necessário para executar o backend fora do Docker.

Se usar nvm, execute `nvm install` e `nvm use` na raiz do projeto. A versão está registrada em `.nvmrc`.

Na raiz:

```bash
node scripts/init-env.mjs
```

O comando cria `.env` e `frontend/.env.local` a partir dos exemplos. Gera senha do banco e segredo JWT; valores existentes são preservados. Se `.env` já tiver apenas `DB_PASSWORD`, o comando acrescenta `JWT_SECRET` sem trocar a senha do banco. Variáveis vazias também são preenchidas. Os valores não são impressos.

O segredo JWT precisa ser Base64 de pelo menos 32 bytes. O inicializador já produz esse formato. Os arquivos com valores locais são ignorados pelo Git; os exemplos são versionados.

## Executar a aplicação completa com Docker

```bash
docker compose -f compose.app.yml up --build --detach --wait --wait-timeout 180
```

Acesse:

| Serviço | Endereço padrão |
|---|---|
| Interface | http://localhost:3000 |
| API | http://localhost:8080 |
| Documentação da API | http://localhost:8080/swagger-ui.html |

Crie um usuário na interface e cadastre suas contas, categorias e transações.

O Compose aguarda o banco ficar saudável antes de iniciar o backend e aguarda o backend antes da interface. O esquema é criado por Flyway e validado pelo Hibernate. O PostgreSQL desse modo é interno à rede Docker e usa o volume `app_postgres_data`.

Este modo tem banco próprio, separado do PostgreSQL do desenvolvimento local. Os dados existentes do modo local não são migrados automaticamente.

Para consultar estado, logs e encerrar os serviços preservando o volume:

```bash
docker compose -f compose.app.yml ps
docker compose -f compose.app.yml logs --follow
docker compose -f compose.app.yml down
```

Se as portas já estiverem ocupadas, altere `BACKEND_PORT` e `FRONTEND_PORT` em `.env` e repita o comando com `--build`. A URL da API usada pelo navegador e a origem CORS acompanham essas portas por padrão. `NEXT_PUBLIC_API_URL` e `CORS_ALLOWED_ORIGIN` podem sobrescrever os valores derivados.

`NEXT_PUBLIC_API_URL` é incorporada ao JavaScript durante o build do Next.js. Ao mudar a URL ou a porta da API, reconstrua a interface com `--build`. O endereço deve ser acessível pelo navegador, por exemplo `http://localhost:8080`; o nome interno do serviço Docker não é um endereço do navegador.

## Desenvolvimento local

Use o Compose original para subir apenas o PostgreSQL:

```bash
docker compose up --detach --wait
```

Esse modo mantém o serviço `postgres`, o container `finance-manager-postgres` e o volume `postgres_data`. A porta externa pode ser alterada com `DB_PORT`; se fizer isso, ajuste também `DB_URL` no `.env`.

Em um terminal, na raiz do projeto, carregue as variáveis e inicie o backend:

```bash
set -a
. ./.env
set +a
cd backend
./mvnw spring-boot:run
```

O Spring Boot não carrega o `.env` da raiz automaticamente. Também é possível configurar `DB_URL`, `DB_PASSWORD` e `JWT_SECRET` nas variáveis de ambiente da IDE. No Windows, use `mvnw.cmd` com essas variáveis configuradas no terminal ou na IDE.

Em outro terminal:

```bash
cd frontend
npm ci
npm run dev
```

A interface usa `frontend/.env.local`, criado pelo inicializador. O backend local usa a porta 8080 e permite a origem `http://localhost:3000` por padrão; `SERVER_PORT` e `CORS_ALLOWED_ORIGIN` podem alterar esses valores.

## Verificações locais

Backend, com Java 21 e Docker em execução:

```bash
cd backend
./mvnw --batch-mode --no-transfer-progress verify
```

Os testes de integração criam seu próprio PostgreSQL 17 via Testcontainers. Eles não precisam do banco de desenvolvimento em execução. Se o Testcontainers não localizar o Docker Desktop, configure o endpoint do contexto atual antes de executar:

```bash
export DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')"
```

Frontend, com Node.js 24:

```bash
cd frontend
npm ci
npm run check
```

`check` executa lint, gera os tipos de rotas do Next.js, verifica TypeScript e constrói a aplicação. Para verificar apenas tipos, use `npm run typecheck`.

O inicializador de variáveis tem testes próprios, executados na raiz:

```bash
node --test scripts/init-env.test.mjs
```

O fluxo integrado automatizado usa `scripts/smoke-test.mjs`. Execute-o contra um ambiente de teste descartável, com os endereços explícitos:

```bash
SMOKE_API_URL=http://localhost:8080 \
SMOKE_FRONTEND_URL=http://localhost:3000 \
node scripts/smoke-test.mjs
```

Esse fluxo cria usuários de teste, exercita autenticação, recursos, transações, filtros, saldos e dashboard, e confirma que um usuário não consegue consultar ou alterar os dados do outro. Também valida duplicidades, intervalos invertidos, acesso sem autenticação e importação CSV, incluindo prévia sem gravação, erros sem importação parcial, repetição e confirmações simultâneas. Ao final remove os recursos financeiros que criou; os usuários de teste permanecem até a remoção do banco descartável.

## Importar transações CSV

Acesse **Importar CSV** na navegação. Escolha uma conta e uma categoria já cadastradas; ambas serão usadas em todas as linhas do arquivo. O modelo pode ser baixado na própria tela.

```csv
data;descricao;valor;tipo
2026-10-01;Salário;3000,00;RECEITA
02/10/2026;Supermercado;150,50;DESPESA
```

- UTF-8, com ou sem BOM; até 1 MiB e 1000 registros.
- Cabeçalhos `data`, `descricao`, `valor`, `tipo`, em qualquer ordem; sem colunas extras. Nomes dos cabeçalhos não diferenciam maiúsculas de minúsculas.
- Separadores `;` ou `,`. Campos com separador ou quebra de linha devem estar entre aspas duplas; uma aspa no conteúdo deve ser duplicada.
- Datas `AAAA-MM-DD` ou `DD/MM/AAAA`, validadas sem corrigir dias inexistentes.
- Valores positivos, com ponto ou vírgula decimal e até duas casas; sem símbolo de moeda nem separador de milhar. Com CSV separado por vírgulas, coloque valores com vírgula decimal entre aspas.
- Tipos `RECEITA` / `DESPESA` ou `INCOME` / `EXPENSE`.

**Gerar prévia** não grava transações. A tela informa as novas, duplicadas e inválidas, com erros e números de linha. Corrija todas as inválidas e envie o arquivo novamente antes de confirmar. Alterar arquivo, conta ou categoria descarta a prévia anterior.

**Confirmar importação** grava as linhas novas em uma única transação de banco. Duplicados são ignorados, tanto dentro do arquivo quanto na comparação com transações existentes, inclusive criadas manualmente. A correspondência exige a mesma conta, categoria, data, descrição após remover espaços externos, valor e tipo. Movimentações diferentes com esses mesmos dados também serão tratadas como duplicadas; confira essa regra antes de importar.

A API verifica novamente os duplicados na confirmação e serializa importações na mesma conta. Confirmar novamente um arquivo sem alterações nos dados persistidos não acrescenta registros repetidos. Se um registro for editado ou removido depois, uma nova importação compara o arquivo com o estado atual do banco.

Endpoints autenticados, em `multipart/form-data`:

- `POST /transactions/import/preview`: `file`, `accountId`, `categoryId`.
- `POST /transactions/import/confirm`: os mesmos campos e `previewHash` retornado na prévia. O hash vincula o arquivo e a seleção; ele não substitui a autenticação e a validação da API.

A prévia não cria rascunhos persistentes. O arquivo é enviado novamente na confirmação e não é armazenado no servidor.

## Orçamentos mensais por categoria

Em **Orçamentos**, selecione mês e ano, escolha uma categoria e informe um limite positivo. **Salvar limite** cria o orçamento ou atualiza o limite da mesma categoria e mês. Os limites de meses diferentes são independentes e não são repetidos automaticamente.

Os indicadores mostram limite, despesas registradas no mês, valor disponível ou excedido e percentual utilizado. Entram despesas de todas as contas do usuário, inclusive datas futuras dentro do mês; receitas não compensam gastos. Edição, exclusão e importação de despesas se refletem ao consultar o mês novamente. A barra para no limite, mas o percentual e o valor excedido continuam mostrando o consumo total.

Excluir orçamento mantém as transações. Categorias com orçamentos precisam ter esses vínculos removidos antes de serem excluídas.

API autenticada:

- `GET /budgets?year=2026&month=10`: orçamentos e consumo do mês.
- `PUT /budgets`: corpo com `categoryId`, `year`, `month`, `limitAmount`; cria ou atualiza atomicamente o limite e retorna o acompanhamento da categoria.
- `DELETE /budgets/{id}`: remove somente o orçamento pertencente ao usuário.

Cada resposta inclui `id`, `categoryId`, `categoryName`, `periodStart`, `limitAmount`, `spent` e `remaining`. O ano deve estar entre 1 e 9999, o mês entre 1 e 12 e o limite deve ser positivo, com até duas casas decimais. `remaining` negativo indica excesso. A migração Flyway V2 cria a tabela de limites e a restrição de um orçamento por categoria/mês, preservando os dados existentes.

## Paginação de transações

A lista carrega 10 transações por página, com opções de 25 ou 50. **Página anterior** e **Próxima página** mantêm os filtros aplicados; a tela informa o intervalo de registros e o total de páginas. Aplicar ou limpar filtros e trocar o tamanho retornam à primeira página.

Após criar ou editar, a lista consulta novamente a primeira página para manter a ordem por data e identificador decrescentes. Após excluir, permanece na página atual ou retorna à última página existente quando necessário. Se a atualização da lista falhar após uma gravação bem-sucedida, a confirmação da gravação permanece visível junto do erro da consulta.

Endpoint autenticado: `GET /transactions/page?page=0&size=10`, com os mesmos filtros `startDate`, `endDate`, `type`, `accountId` e `categoryId`. A resposta contém `content`, `page`, `size`, `totalElements` e `totalPages`. A página da API começa em zero; `size` aceita de 1 a 100. Os registros e a contagem são filtrados pelo usuário autenticado e pela mesma seleção. Páginas além da última retornam conteúdo vazio e preservam a contagem.

`GET /transactions` mantém o contrato anterior e a consulta completa. **Exportar CSV** continua gerando todas as transações dos filtros aplicados, independentemente da página visível.

## Comparação mensal no dashboard

O **Resumo mensal** compara o mês de referência com o mês anterior, mostrando receitas, despesas, saldo do mês e a diferença em reais de cada indicador. Ao abrir o dashboard, usa o mês atual do navegador; para consultar outro mês, selecione mês e ano e clique em **Comparar meses**.

Essa seleção é independente do filtro geral de período. Cada consulta usa os limites completos do mês, exibidos em DD/MM/YYYY; inclui todas as transações datadas nesse intervalo, inclusive datas futuras registradas. Janeiro compara com dezembro do ano anterior, e fevereiro considera anos bissextos.

O saldo mensal é receitas menos despesas, não o saldo acumulado das contas. Meses sem movimentação exibem valores zero e uma mensagem; falhas têm opção de tentar novamente. As consultas reutilizam a API autenticada existente e seus filtros por data.

Testes da comparação:

```bash
node --test scripts/monthly-summary.test.mjs
```

## Exportar transações CSV

Na tela **Transações**, aplique os filtros desejados e clique em **Exportar CSV** na seção de transações cadastradas. Sem filtros, o arquivo inclui todas as transações do usuário autenticado. Alterações nos campos de filtro só entram na exportação depois de clicar em **Aplicar filtros**.

A exportação consulta novamente a listagem autenticada com os filtros aplicados e gera `transacoes.csv` no navegador. O botão fica indisponível durante outras operações, quando a lista falha ou quando está vazia. Falhas de autenticação seguem o fluxo de login existente; falhas de consulta não geram um arquivo parcial.

O relatório usa UTF-8 com BOM, separador `;`, datas `DD/MM/YYYY`, valores com vírgula e duas casas decimais, tipos `RECEITA` e `DESPESA`, e colunas `data;descricao;valor;tipo;conta;categoria`. Aspas e quebras de linha são preservadas dentro de campos delimitados. Textos que podem ser interpretados como fórmulas recebem um apóstrofo inicial para leitura como texto nas planilhas.

Este relatório possui colunas adicionais de conta e categoria e não é o modelo de importação de quatro colunas. Para importar, use **Baixar modelo CSV** na página de importação.

Teste do serializador (após instalar as dependências do frontend):

```bash
node --test scripts/transaction-csv.test.mjs
```

## Integração contínua

O workflow `.github/workflows/ci.yml` é acionado em pushes, pull requests e execução manual:

1. **Backend:** Java 21, Maven Wrapper, testes e empacotamento; relatórios de testes ficam disponíveis como artefato.
2. **Frontend:** Node.js 24, instalação pelo lockfile, testes do inicializador, da exportação CSV e da comparação mensal, lint, TypeScript e build.
3. **Segredos:** Gitleaks com versão/checksum fixados, autoteste sintético e varredura do histórico alcançável; veja [prevenção e histórico](docs/public-exposure.md).
4. **Containers:** construção dos dois serviços, espera por saúde, validação integrada e remoção do banco temporário.

Os jobs de backend, frontend e segredos executam em paralelo; o teste dos containers começa após os três passarem. O CI gera credenciais apenas para seu ambiente temporário e não exige segredos de produção. Após os checks, o mesmo workflow pode publicar imagens no GHCR e atualizar a EC2. O CD fica desabilitado até autorização e configuração explícita; veja o [runbook de CD](docs/deploy-cd.md).

## Demonstração na EC2

A atualização automatizada por GitHub Actions/GHCR está descrita no [runbook de CD](docs/deploy-cd.md), com [diagnóstico da instalação documentada](docs/deploy-cd-diagnosis.md), preparação, ativação, saúde e rollback.

O [guia de deploy](docs/deploy-ec2.md) descreve a entrega de imagens pré-construídas para uma EC2 x86_64, acesso inicial por túnel SSH, HTTPS com domínio, volumes persistentes e backup. Use `compose.demo.yml` para essa modalidade; `compose.app.yml` continua sendo o ambiente local.

## Estrutura

- `backend/`: API, regras de negócio, migrações, testes e Dockerfile.
- `frontend/`: interface e Dockerfile com saída standalone.
- `docker-compose.yml`: banco para desenvolvimento local.
- `compose.app.yml`: aplicação completa com banco próprio.
- `scripts/`: preparação de variáveis e validação integrada.

Referências: [ordem de inicialização no Docker Compose](https://docs.docker.com/compose/how-tos/startup-order/), [configuração de Node no GitHub Actions](https://github.com/actions/setup-node) e [configuração de Java no GitHub Actions](https://github.com/actions/setup-java).

Autor: Gabriel Heinrick Dias.

## Contratos e runbooks atuais

Consulte [arquitetura](docs/01_ARCHITECTURE.md), [decisões F01–F11](docs/02_DECISION_LOG.md), [segurança e contratos](docs/security-and-contracts.md), [backup/restauração](docs/backup-and-restore.md) e [evidências de validação](docs/validation-f01-f11.md). Dinheiro em respostas JSON é string decimal de duas casas; atualize clientes coordenadamente. Exportações recusam mais de 10000 registros. Saúde pública usa `/health/live` e `/health/ready`.

Testes Node incluem dinheiro, CSV, comparação mensal, tema, inicialização e empacotamento. `python3 scripts/backup_test.py` verifica ferramentas de backup; em ambiente local descartável já iniciado, `cd frontend && E2E_BASE_URL=http://localhost:3000 npm run test:e2e` executa Playwright/axe. Instale antes o Chromium com `npx playwright install --with-deps chromium`. Não executar essas suítes contra uma instalação com dados de uso.

As referências imutáveis verificadas estão em [dependency-pins.json](docs/dependency-pins.json). O mantenedor deve revisá-las mensalmente e após avisos de segurança, repetindo os checks antes da promoção. Configuração existente e checks definidos não comprovam disponibilidade nem controles em produção.
