# Validação e pendências — F01–F11

Data: 20/10/2026, America/Sao_Paulo. Revisão-base: `4f5d781e116091d42fb14d6392ec700452634191`. Implementação local ainda sem commit; não se atribuem os novos checks ao conteúdo original desse SHA. O manifesto de fontes nesta pasta identifica o conteúdo efetivamente validado. Não houve commit, push, deploy, publicação, acesso AWS/SSH ou operação em produção.

## Alterações e critérios

| Tarefa | Implementação/aceite local | Pendências e limites |
|---|---|---|
| F01 | Strings decimais nas respostas e novos clientes; BigInt nos centavos; DTOs, OpenAPI, formulários, formatos, gráfico, CSV, orçamento e diferenças coerentes. Casos 0.10+0.20, máximo de entrada, totais superiores ao máximo, diferença negativa, JSON/CSV e fluxo real de UI. | Respostas alteram o contrato de clientes antigos; migração deve ser coordenada. Entrada JSON numérica permanece por compatibilidade, sem garantia de precisão em clientes antigos. |
| F02 | Filtro configurável, 429/Retry-After, memória limitada, recuperação de janela, clientes distintos e rejeição de identidade por header arbitrário. | Controles externos não inspecionados. Limitador por processo e peer; gateway/NAT compartilha quota. Ajustar limites/proxy e projetar cota global antes de múltiplas instâncias. |
| F03 | Decoder/filtro reais testados para assinatura, expiração, algoritmo e claims inválidas/ausentes. Política stateless de uma hora documentada. CTA expirado e 401 testados no navegador. | Logout não revoga tokens. CSP avaliada e mantida sem alteração por exigir tratamento do inicializador de tema. Issuer/audience não adotados para o único emissor/consumidor atual. |
| F04 | Testes Node incluem tema no CI, regressão de tema deliberadamente detectada em cópia temporária; Playwright real em dois temas e duas larguras, CRUD, CSV, dashboard/orçamento, carregamento/erro/vazio, teclado, primeira preferência e persistência. Axe mediu contraste, sem violações nos estados ensaiados. | Execução local comprovada; execução remota do GitHub Actions não realizada. Artefatos CI usam lista permitida de metadados, sem logs, propriedades, mensagens de erro, anexos ou traces brutos. |
| F05 | Liveness sem acesso ao banco, readiness com timeouts e JSON sanitizado; Compose atualizado. Pausa de PostgreSQL descartável produziu live 200/ready 503 e recuperação 200/200. | Não houve teste de falha em ambiente operacional. Checks de dependência não comprovam capacidade sob carga. |
| F06 | Scripts/runbook, criptografia age, dump custom validado, checksum, idade, retenção, restauração separada. Ensaio local restaurou e conferiu contagens, somas e Flyway. | Cópia externa apenas simulada localmente. Aprovar destino real, custódia, responsável, RPO/RTO e retenção; instalar/monitorar agendamento somente com autorização operacional. Não se declara recuperação operacional comprovada. |
| F07 | Checkout alterado rejeitado antes de Docker; build de git archive imutável, SHA completo, IDs/digests, plataforma, checks da revisão e checksum. Testes sem build real por unidade confirmam rejeição, manifesto, integridade e preservação de pacote. | Release real não produzida: fontes ainda sem commit e gate deve rejeitá-las. Promoção e deploy exigem aprovação própria. |
| F08 | Saldos em lote, exportação limitada no SQL a 10001 linhas para detectar teto de 10000, recusa clara sem arquivo truncado. Filtros/isolamento testados. Planos e tempos antes/depois registrados com massa sintética. | Sem índices adicionados: não houve justificativa suficiente para custo de escrita. Medições locais não são SLA; reavaliar distribuição/carga reais e memória do navegador. |
| F09 | JSON/tipos/enum/data/parâmetros padronizados, constraints conhecidas distintas, falha desconhecida 500 sanitizada e timestamp UTC. Corridas determinísticas reais de cadastro e renomeação retornam 201/409 e 200/409. | Nenhuma migração de schema necessária. Não se mapeia toda falha de integridade como duplicidade. |
| F10 | Actions por SHAs verificados; bases, PostgreSQL, Caddy e frontend Dockerfile por digests oficiais compatíveis com linux/amd64; empacotador alinhado, tipos Node 24 e builds de imagem locais. | Mantenedor do repositório deve revisar referências mensalmente e após avisos. apt/npm ainda podem exigir avaliações de segurança próprias; não se alega build bit a bit nem auditoria de CVEs. |
| F11 | Arquitetura, decisões, segurança/contratos, backup/deploy e README atualizados; contexto inicial marcado como histórico. | Disponibilidade/controles de produção não foram verificados. |

## Checks executados

- `mvn -o -q -f backend/pom.xml verify`: 177 testes aprovados, incluindo unitários/MVC e integração PostgreSQL/Testcontainers; resumo final em `validation/backend-summary-2026-10-20.json`.
- `NEXT_PUBLIC_API_URL=http://127.0.0.1:18379 npm run check`: lint, geração de tipos, TypeScript e build de produção.
- `node --test scripts/*.test.mjs`: dinheiro, CSV, calendário mensal, tema, inicialização e empacotador; 23 testes aprovados.
- `python3 -m unittest discover -s scripts -p '*_test.py'`: 5 testes aprovados de backup e sanitização de relatórios.
- `E2E_BASE_URL=http://127.0.0.1:13379 npm run test:e2e`: 5 cenários aprovados; resumo sanitizado em `validation/browser-summary-2026-10-20.json`.
- `scripts/smoke-test.mjs`: 133 verificações HTTP aprovadas contra serviços descartáveis exclusivos em localhost; cobre autorização, CRUD, dashboard, orçamento e importação CSV.
- `bash -n scripts/package-demo.sh`; `docker compose -f compose.app.yml config --quiet`; configuração demo com valores sintéticos e `config --quiet`: aprovados, sem subir ambiente operacional.
- Builds locais linux/amd64 de backend e frontend com os Dockerfiles fixados: aprovados. Imagens usadas apenas para validação, sem publicação.
- Ensaio de regressão: remover o inicializador de tema em uma cópia temporária faz o mesmo teste Node do CI falhar; fontes finais preservadas.

## Medições e ensaios

Docker Desktop Linux, x86_64, memória disponível ao engine aproximadamente 5,6 GiB, Node 24.21.0 e PostgreSQL 17. Os planos usam tabelas temporárias transacionais com 500 contas (100 por usuário) e 100000 transações sintéticas, ANALYZE e rollback. A comparação equivalente das 100 contas levou 305,695 ms individualmente e 7,085 ms em lote, excluindo HTTP. Exportação sem limite retornou 20000 linhas (17,132 ms); com sentinela, 10001 linhas (18,018 ms). O teto controla materialização; este ensaio não demonstrou ganho de latência na exportação. Não se adicionou índice.

CSV de 10000 linhas com campos extensos e Unicode: 9650046 bytes, aproximadamente 49,65 MB adicionais de heap e 79,84 ms no Node, abaixo da proposta de 64 MiB. É medição de heap após execução em Node, não pico instrumentado de navegador/JVM nem garantia para todos os datasets. Repetir `node --expose-gc scripts/benchmark-csv.mjs` e o SQL em banco explicitamente descartável para comparação.

O ensaio age/pg_restore levou 0,886 s com dados sintéticos pequenos. Não mede RTO operacional. A cópia externa foi simulada por diretório local; nenhuma conta ou infraestrutura foi acessada. As evidências sanitizadas estão em `validation/`.

## Ocorrências de validação e preservação

A primeira escolha da porta 18080 colidiu com um serviço local preexistente, de outro checkout. O smoke inicial atingiu esse serviço e criou registros sintéticos antes de detectar o contrato antigo; essa execução foi invalidada. Os testes foram interrompidos, mudaram para portas exclusivas 18379/13379 e passaram contra banco criado nesta tarefa. O serviço preexistente não foi parado, sua configuração não foi lida e não foi feita limpeza indiscriminada de registros que poderiam pertencer a outra validação. Os registros sintéticos daquela execução permanecem como pendência de limpeza do ambiente local correspondente.

A primeira tentativa de recuperação com stop/start do container descartável mudou a porta publicada aleatoriamente e invalidou o ensaio. O teste válido usou pause/unpause, preservando endereço e dados; o resultado está em `validation/health-exercise-2026-10-20.json`. Nenhum volume ou serviço preexistente foi removido. Falhas iniciais de sandbox em subprocessos/Mockito foram superadas executando checks locais autorizados; não foram declaradas falhas do produto.

## Alerta adicional de dependências

`npm audit` registrou 5 ocorrências altas na cadeia de desenvolvimento ESLint/fast-glob/micromatch/braces. `npm audit --omit=dev` não registrou vulnerabilidades nas dependências de produção nesta consulta; isso não substitui análise de imagem/JVM ou comprova ausência de vulnerabilidades. O [aviso oficial GHSA-vfj7-8cjw-p6xm](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm) afeta braces até 3.0.3 e não informa versão corrigida. A sugestão automática do npm envolve downgrade incompatível de eslint-config-next; não foi aplicada. Acompanhar correção upstream ou avaliar migração compatível da ferramenta em revisão própria. Evidência: `validation/npm-audit-summary-2026-10-20.json`.
