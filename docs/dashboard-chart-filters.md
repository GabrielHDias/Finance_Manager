# Dashboard: gráficos dinâmicos e filtros financeiros

Implementação na branch `feat/dashboard-chart-filters`, criada de `main` após `git fetch origin main` e confirmação de divergência `0/0`. A árvore de trabalho estava limpa. Este relatório registra a validação local anterior à publicação da branch. Após essa validação, o usuário autorizou commit, push e merge. O workflow foi consultado e `PROD_CD_ENABLED=true`: o merge acionará deploy automático. O usuário autorizou explicitamente esse deploy somente para esta tarefa; em tarefas futuras a permissão deve ser solicitada novamente.

## Funcionalidade

O dashboard mostra despesas ou receitas por categoria, dia ou mês, em pizza/rosca, linha ou coluna. Os filtros do gráfico permanecem disponíveis durante o carregamento. Trocar a visualização não refaz a consulta nem perde movimentação, agrupamento ou período.

O padrão é despesas por categoria em rosca, no mês atual do navegador. O período possui os atalhos “Mês atual” e “Últimos 12 meses”; a troca de agrupamento mantém as datas. É necessário aplicar as duas datas válidas, em ordem, com intervalo máximo de dez anos. O agrupamento diário aceita até 366 dias, inclusive. Os endpoints anteriores continuam aceitando seus parâmetros originais.

Categorias permitem pizza e coluna. A opção linha fica desabilitada; ao trocar de uma série temporal em linha para categorias, o controle seleciona coluna. Ao sair de pizza por categoria para uma série temporal, seleciona linha. Pizza temporal permite até oito períodos, contando os intervalos sem movimentação; se um período maior for aplicado a uma pizza temporal, a tela mostra coluna e informa o ajuste.

## Arquitetura e contrato

- `DashboardController` obtém o proprietário exclusivamente de `AuthenticatedUser`, utilizando o JWT existente. Não há parâmetro para selecionar outro usuário.
- `DashboardService` valida o intervalo, chama agregações do repository e preenche lacunas nas séries temporais. Nenhuma lista de transações é carregada para gerar o gráfico.
- `TransactionRepository` agrega por data ou por ano/mês no PostgreSQL por meio de JPQL. Categorias reutilizam `sumExpensesByCategory`, que já recebe o tipo financeiro e agrupa pelo ID da categoria.
- `dashboard-service.ts` monta a URL e reutiliza `apiRequest`, inclusive autenticação e tratamento de erros. Aceita `AbortSignal`.
- `FinancialChartPanel` coordena filtros e requisições; `ChartFilters`, `FinancialChart` e `ChartSummary` separam controles, visualização e total. AbortController e a checagem de requisição ativa impedem resultados antigos de substituir os novos.
- Os gráficos usam Recharts, CSS convencional e as variáveis de cor existentes. Loading, erro com retry e vazio são distintos. `401` encaminha ao login e `403` apresenta o erro da API.
- Valores monetários usam `BigDecimal` e o `MoneySerializer` existente, chegando ao navegador como strings decimais. Totais, tooltips e lista acessível usam o formatador BRL existente, preservando centavos mesmo acima do limite seguro de `number`. Somente a geometria e as marcas aproximadas do eixo usam números/razões. Percentuais e marcas do eixo usam `Intl.NumberFormat` em `pt-BR`.
- `Transaction.date` permanece `LocalDate`, sem conversão de fuso. Séries temporais recebem zeros do backend; total zero gera mensagem de ausência de movimentação.

Novo endpoint:

```http
GET /dashboard/transactions-summary?type=EXPENSE&groupBy=DAY&startDate=2026-10-01&endDate=2026-10-03
Authorization: Bearer <JWT>
```

Todos os parâmetros são obrigatórios. `type`: `INCOME | EXPENSE`; `groupBy`: `DAY | MONTH | CATEGORY`. Datas inválidas, parâmetros ausentes, enums desconhecidos e intervalos fora dos limites retornam `400` no contrato de erro atual. Anos aceitos: 0001 a 9999.

```json
{
  "type": "EXPENSE",
  "groupBy": "DAY",
  "total": "0.30",
  "items": [
    { "key": "2026-10-01", "label": "01/10/2026", "total": "0.10" },
    { "key": "2026-10-02", "label": "02/10/2026", "total": "0.00" },
    { "key": "2026-10-03", "label": "03/10/2026", "total": "0.20" }
  ]
}
```

Chaves mensais usam `AAAA-MM`, rótulos `mês/AAAA`; categorias usam o ID convertido para string e o nome atual como rótulo, ordenadas pelo total decrescente. Categoria sem dados retorna `items: []`; séries temporais sem dados retornam os períodos com zeros.

Preservados: `GET /dashboard/summary`, `GET /dashboard/expenses-by-category`, JWT, CORS, rotas, tema e resumo comparativo mensal. Nenhuma dependência nova, migração ou alteração de infraestrutura/pipeline.

## Validações executadas

- `cd backend && ./mvnw test`: 197 testes, zero falhas/erros/ignorados, com PostgreSQL real em Testcontainers.
- `cd frontend && npm ci --no-audit --no-fund`: concluído com o lockfile existente.
- `npm run lint`: aprovado.
- `npm run typecheck`: aprovado.
- `npm run build`: aprovado.
- `./node_modules/.bin/playwright test e2e/dashboard-charts.spec.ts`: oito cenários; filtros, três gráficos, BRL, estados, combinações incompatíveis, preservação de filtros, períodos, sessão, dois temas, larguras 390/1280 e acessibilidade do painel. Inclui um cenário contra a API real e banco descartável.
- `./node_modules/.bin/playwright test e2e/finance.spec.ts`: cinco cenários de regressão aprovados, incluindo cadastro/login, CRUD, CSV, teclado, contraste e expiração de sessão.
- `git diff --check`: aprovado.

As primeiras tentativas de backend e build no sandbox falharam por restrições de instrumentação/Docker e subprocessos. As execuções com permissões adequadas passaram. Uma tentativa de aprovação automática do Playwright expirou; a repetição autorizada executou normalmente. Uma asserção nova precisou limitar o seletor de alerta ao conteúdo da página para excluir o anunciador de rotas do Next.js; foi corrigida. Não foi identificada falha preexistente na suíte executada.

Evidências: [resumo técnico](validation/dashboard-chart-filters/checks.json), [regressão](validation/dashboard-chart-filters/regression-summary.json), [cenários dos gráficos](validation/dashboard-chart-filters/charts-summary.json), [desktop claro](validation/dashboard-chart-filters/light-desktop.png), [celular escuro](validation/dashboard-chart-filters/dark-mobile.png), [API real](validation/dashboard-chart-filters/real-api.png).

## Validação manual

Para reproduzir a revisão, use frontend e API locais com PostgreSQL descartável e dados sintéticos. Crie uma conta de teste no próprio ambiente; não publique credenciais nem o caminho de arquivos de acesso da sessão. As evidências abaixo descrevem o ensaio realizado, sem prometer uma instância de revisão permanente.

1. Faça login e selecione 01/10/2026 a 31/10/2026, aplicando o período. A conta de revisão possui despesas de **R$ 2.401,25** e receitas de **R$ 4.500,00** em outubro.
2. Em categoria, alterne despesas/receitas e pizza/coluna; confira os percentuais e os tooltips. Linha deve permanecer indisponível.
3. Troque para dia e teste linha/coluna; confira a ordenação e os dias com zero. Para testar pizza diária, reduza para até oito dias. Aumente novamente o intervalo e confira o ajuste informado para coluna.
4. Troque para mês e use “Últimos 12 meses”. Confira meses vazios e identificação de ano; pizza fica indisponível para os doze períodos.
5. Alterne rapidamente movimentação e agrupamento durante uma consulta. O resultado final deve corresponder aos controles atuais.
6. Consulte um período sem dados, por exemplo 01/01/2020 a 31/01/2020. Confira a mensagem de ausência de movimentação.
7. Teste um intervalo invertido e mais de 366 dias no agrupamento diário; a mensagem deve orientar a correção sem apagar os demais filtros.
8. Confira celular e desktop, temas claro e escuro e navegação por teclado.

## Publicação e integração

O usuário autorizou commit, push e merge após a entrega da validação local. O texto do PR está em [dashboard-chart-filters-pr.md](dashboard-chart-filters-pr.md), com base `main` e compare `feat/dashboard-chart-filters`. As verificações remotas devem ser aprovadas antes da integração. Como `PROD_CD_ENABLED=true`, o usuário também autorizou explicitamente o deploy automático em produção somente para esta tarefa, com nova solicitação de permissão obrigatória em tarefas futuras. Os relatórios JSON de validação registram o estado anterior à publicação; o estado atualizado do PR e da CI deve ser consultado no GitHub.

## Arquivos modificados

- `backend/src/main/java/io/github/gabrielhdias/financeManager/api/dashboard/DashboardController.java`
- `backend/src/main/java/io/github/gabrielhdias/financeManager/domain/dashboard/DashboardService.java`
- `backend/src/main/java/io/github/gabrielhdias/financeManager/domain/transaction/TransactionRepository.java`
- `backend/src/test/java/io/github/gabrielhdias/financeManager/api/dashboard/DashboardControllerMvcTest.java`
- `backend/src/test/java/io/github/gabrielhdias/financeManager/domain/transaction/TransactionRepositoryTest.java`
- `frontend/src/app/(protected)/dashboard/dashboard.module.css`
- `frontend/src/app/(protected)/dashboard/page.tsx`
- `frontend/src/services/dashboard-service.ts`
- `frontend/src/types/dashboard.ts`

## Arquivos criados

- `backend/src/main/java/io/github/gabrielhdias/financeManager/api/dashboard/dto/TransactionChartResponse.java`
- `backend/src/main/java/io/github/gabrielhdias/financeManager/domain/dashboard/ChartGrouping.java`
- `backend/src/main/java/io/github/gabrielhdias/financeManager/domain/dashboard/MonthTotal.java`
- `backend/src/main/java/io/github/gabrielhdias/financeManager/domain/dashboard/PeriodTotal.java`
- `backend/src/main/java/io/github/gabrielhdias/financeManager/domain/dashboard/TransactionChartSummary.java`
- `backend/src/test/java/io/github/gabrielhdias/financeManager/domain/dashboard/TransactionChartSummaryTest.java`
- `docs/dashboard-chart-filters-pr.md`
- `docs/dashboard-chart-filters.md`
- `docs/validation/dashboard-chart-filters/charts-summary.json`
- `docs/validation/dashboard-chart-filters/checks.json`
- `docs/validation/dashboard-chart-filters/dark-mobile.png`
- `docs/validation/dashboard-chart-filters/light-desktop.png`
- `docs/validation/dashboard-chart-filters/real-api.png`
- `docs/validation/dashboard-chart-filters/regression-summary.json`
- `frontend/e2e/dashboard-charts.spec.ts`
- `frontend/src/components/dashboard/chart-filters.tsx`
- `frontend/src/components/dashboard/chart-summary.tsx`
- `frontend/src/components/dashboard/financial-chart-panel.tsx`
- `frontend/src/components/dashboard/financial-chart.module.css`
- `frontend/src/components/dashboard/financial-chart.tsx`
