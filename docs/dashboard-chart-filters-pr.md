# feat(dashboard): add configurable financial charts

Base: `main`
Compare: `feat/dashboard-chart-filters`

O dashboard passa a permitir receitas ou despesas por dia, mês ou categoria, com gráficos de pizza/rosca, linha e coluna. Os filtros preservam o período e a movimentação ao trocar de visualização; combinações sem significado temporal ou com excesso de fatias são ajustadas ou desabilitadas com orientação na tela.

## Alterações

- Novo `GET /dashboard/transactions-summary`, autenticado e isolado por usuário, com agregações no banco, valores decimais exatos e períodos vazios preenchidos com zero.
- Reutilização da agregação por ID da categoria e dos contratos anteriores, sem alterar autenticação, CORS ou infraestrutura.
- Componentes separados para controles, gráfico e total; Recharts e temas existentes; atalho dos últimos doze meses.
- Loading, erro com retry, ausência de dados, cancelamento e proteção contra respostas antigas.
- Testes unitários, MVC, PostgreSQL/Testcontainers e Playwright.

## Evidências

- Backend: 197 testes aprovados, zero falhas/erros/ignorados.
- Frontend: instalação pelo lockfile, lint, tipagem e build aprovados.
- Playwright: oito cenários novos e cinco cenários existentes de regressão aprovados, com API real local, temas claro/escuro e larguras 390/1280.
- Capturas e relatórios: `docs/validation/dashboard-chart-filters/`.

## Validação manual

Use a conta fictícia na instância local descrita em `docs/dashboard-chart-filters.md`. Aplique outubro de 2026, alterne movimentação, agrupamento e os três gráficos; confira zeros nas séries, percentuais, BRL e preservação dos filtros. Teste intervalos inválidos, ausência de dados, teclado e os dois temas.

## Limites e aprovação

Intervalo máximo de dez anos; diário até 366 dias; pizza temporal até oito períodos. As marcas dos eixos são aproximações visuais, enquanto totais, tooltips e lista acessível mantêm centavos exatos. Sem dependências novas ou alterações de deploy. O usuário autorizou commit, push e merge após a validação local. O deploy automático em produção foi autorizado explicitamente somente para esta tarefa, pois `PROD_CD_ENABLED=true`; a CI remota deve ser aprovada antes do merge. Em tarefas futuras, solicitar nova permissão de deploy.
