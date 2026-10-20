# CD na EC2 com GHCR

Status: automação preparada; ativação inicial exige autorização explícita.
Nenhuma configuração da AWS, credencial ou Environment é criada por estes arquivos.
O [diagnóstico anterior à implementação](deploy-cd-diagnosis.md) registra a evidência local e as incertezas sobre a EC2. O [acompanhamento da ativação](deploy-cd-activation.md) registra as configurações externas já realizadas e os bloqueios encontrados após autorização para avançar.

## Fluxo e limites

O único workflow continua sendo `.github/workflows/ci.yml` (`CI`). Backend,
frontend e containers mantêm os checks existentes. Apenas depois de todos passarem,
`publish` constrói e publica os dois Dockerfiles em GHCR, com tag de SHA completo.
`deploy` depende dessa publicação e usa os digests devolvidos pelo mesmo build,
sem resolver `latest` ou buscar outro checkout. O frontend é construído com `/api`.
Pull requests e branches diferentes de `main` nunca publicam ou implantam.
`workflow_dispatch` na `main` repete todos os checks e o mesmo caminho de CD.

A variável **de repositório** `PROD_CD_ENABLED` deve permanecer ausente ou `false`
até autorização para ativar. Somente o valor literal `true` libera publicação/CD.
A variável precisa estar no repositório porque `publish` não usa o Environment.
Os jobs de CI não cancelam execuções da main; `deploy-prod` serializa os deploys.
Depois de esperar aprovação, o workflow confere que seu SHA ainda é o HEAD de main;
se estiver obsoleto, falha antes de acessar a EC2. Uma atualização que chegue após
essa conferência será tratada pela execução seguinte. GitHub pode substituir
execuções pendentes no grupo de concorrência; consulte as execuções na interface.

Não há promessa de zero downtime. Uma EC2 com um container por componente pode
apresentar breve indisponibilidade, inclusive durante rollback. Não são alterados
Compose, Caddy, CORS, autenticação, variáveis de produção, portas ou volumes.
Mudanças nesses arquivos precisam de revisão e aplicação operacional separadas;
o CD entrega somente as imagens. Os checks da CI exercitam a aplicação local;
a validação HTTPS do CD cobre a configuração efetivamente instalada.

## Preparação da EC2 (manual, somente após autorização)

Antes de executar qualquer comando de adoção, conferir na instalação existente:

- Linux x86_64, Python 3.10+ e Docker com Compose v2.20+ (`--wait`, `--wait-timeout`
  e `config --format json` disponíveis), espaço para guardar imagens atual/anterior.
- Caminho real da instalação, Compose `compose.demo.yml`, `.env.demo` existente,
  `deploy/Caddyfile`, projeto efetivo `finance-manager-demo`, serviços `db`,
  `backend`, `frontend`, `proxy` e volumes reais. Se diferirem, adaptar/revisar o
  procedimento antes de usá-lo; não renomear projeto ou criar outro banco.
- Nenhuma porta publicada pelo banco; só proxy expõe HTTP/HTTPS. HTTPS e
  `APP_ORIGIN` precisam já funcionar. `APP_ORIGIN` deve ser origem HTTPS sem `/`
  final, com DNS acessível também da própria EC2.
- Todos os serviços precisam estar saudáveis antes da adoção/atualização.
  Readiness pública: `/api/health/ready`; liveness: `/api/health/live`; frontend:
  `/login`. Certificados TLS são verificados; redirects não contam como sucesso.
- O Compose instalado deve estar revisado e corresponder à configuração esperada
  pela versão entregue. Preserve os volumes PostgreSQL/Caddy e os valores de
  `.env.demo`. Não imprima `docker compose config` sem `--quiet` nos logs.

Criar usuário/chave **exclusivos para deploy**, sem autenticação por senha no
pipeline; restringir a chave (sem forwarding/PTY) e permissões aos arquivos usados.
Acesso ao socket Docker equivale a privilégio administrativo sobre o host: um
usuário no grupo Docker não é uma fronteira de isolamento. Proteja a chave e,
quando viável, use runner com acesso de rede restrito e wrapper/forced-command
revisado. O script usa Docker sem `sudo`; não exige `sudo` irrestrito no workflow.
Garanta acesso SSH a partir do runner sem abrir 22 indiscriminadamente. Um runner
hospedado pelo GitHub tem endereços variáveis; adote uma estratégia de acesso de
rede aprovada antes de ativar. Não altere Security Groups como parte deste código.

Obtenha a chave pública do host por canal confiável (por exemplo, console já
verificado) e confira seu fingerprint. Configure `known_hosts` com o hostname/IP
usado em `DEPLOY_HOST`; para porta não padrão, use `[host]:porta`. Um resultado de
`ssh-keyscan` recebido pela mesma rede, sem conferir fingerprint, não comprova a
identidade do host. O pipeline mantém `StrictHostKeyChecking=yes`, `BatchMode=yes`,
`IdentitiesOnly=yes` e autenticação por senha/interativa desabilitada.

Instale uma cópia revisada de `scripts/deploy-ec2.py` como `deploy-cd.py` na raiz
**existente** de instalação. Não é enviado código/configuração automaticamente à
EC2. Atualizações desse script são uma operação manual revisada. O usuário de
deploy precisa ler `.env.demo` e escrever `.deploy/`; mantenha ambos privados.
O `.deploy/` será criado com permissões restritas. Não copie segredos ao checkout.

## Registry

As imagens são `ghcr.io/<owner>/<repo>-backend:<SHA>` e
`ghcr.io/<owner>/<repo>-frontend:<SHA>` (owner/repo em minúsculas).
Publicação usa `GITHUB_TOKEN`, com `packages: write` somente no job `publish`;
checkout usa `contents: read` e não persiste credenciais. Configure as permissões
do pacote/organização para permitir esse repositório publicar e preservar os
pacotes necessários à recuperação. As imagens podem permanecer privadas.

Na EC2, configurar uma vez login Docker como o mesmo usuário de deploy, usando
credencial de leitura GHCR com `read:packages` e acesso aos dois pacotes (e SSO,
se aplicável). Use credential helper ou arquivo Docker protegido; `docker login`
pode armazenar token codificado, sem criptografia. Informe token por stdin e não
por argumento. O token de publicação não é copiado à EC2. Imagens públicas podem
ser baixadas anonimamente. Não efetue login ou cadastre tokens sem autorização.

Não remova imagens em uso, a versão anterior ou pacotes GHCR referenciados pelo
histórico. A primeira versão vinda de `docker load` pode existir apenas no host;
o bootstrap guarda IDs locais imutáveis para permitir restaurá-la. Preserve também
o pacote original fora da instância. O script não executa prune.

## GitHub Environment e configuração

Criar `prod`, limitar deployment branches a `main` e configurar reviewers quando
apropriado ao plano/política do repositório. Proteger `main` exigindo os checks
existentes `backend`, `frontend`, `containers`, revisão e bloqueio de force push.
Confirmar os nomes exatos dos checks após a primeira execução. Nenhuma proteção
é configurada automaticamente. Aprovação do Environment ocorre antes do job
`deploy`; não substitui a autorização inicial para ativar a automação.

| Escopo | Nome | Conteúdo |
| --- | --- | --- |
| Repository variable | `PROD_CD_ENABLED` | Ausente/`false` inicialmente; `true` somente após autorização |
| `prod` variable | `DEPLOY_HOST` | Host/IP da EC2 verificado |
| `prod` variable | `DEPLOY_USER` | Usuário exclusivo com acesso ao Docker |
| `prod` variable | `DEPLOY_PORT` | Porta SSH; padrão `22` |
| `prod` variable | `DEPLOY_PATH` | Caminho absoluto da instalação existente, sem espaços |
| `prod` variable | `PROD_URL` | URL HTTPS, para o link do Environment |
| `prod` secret | `DEPLOY_SSH_KEY` | Chave privada exclusiva, compatível com autenticação não interativa |
| `prod` secret | `DEPLOY_KNOWN_HOSTS` | Entrada(s) verificadas da chave do host |
| Automático | `GITHUB_TOKEN` | Fornecido pelo GitHub; não cadastrar PAT para publicar |
| Somente EC2 | Credencial GHCR de leitura | Login Docker privado do usuário de deploy |
| Somente EC2 | `DB_PASSWORD`, `JWT_SECRET`, demais configurações | Permanecem em `.env.demo` |

## Adoção da versão atual

Identifique o SHA **realmente instalado** pelo manifesto do pacote anterior, não
pelo HEAD atual do repositório. No checkout local que contém essa revisão, obtenha
a identidade do diretório de migrations daquela versão:

```bash
git rev-parse <SHA_INSTALADO>:backend/src/main/resources/db/migration
```

Confirme as migrations aplicadas no banco conforme o runbook operacional. Se não
for possível provar qual revisão está instalada, investigue antes do bootstrap;
o script confia nessa identificação informada pelo operador.

Na EC2, como usuário de deploy, substituindo os placeholders:

```bash
python3 /CAMINHO/EXISTENTE/deploy-cd.py bootstrap /CAMINHO/EXISTENTE \
  --revision <SHA_INSTALADO_COMPLETO> --migrations <HASH_MIGRATIONS_INSTALADAS>
python3 /CAMINHO/EXISTENTE/deploy-cd.py status /CAMINHO/EXISTENTE
```

O bootstrap captura os IDs Docker das imagens de backend/frontend em execução,
valida saúde interna/HTTPS e grava `.deploy/current.json`. Recusa sobrescrever
um estado existente. Não recria serviços nem altera `.env.demo`.

## Ativação e execução manual

Após preparar credenciais, validar a instalação, revisar o diff e obter
**autorização explícita para ativar**, definir a repository variable
`PROD_CD_ENABLED=true`. Essa alteração permite CD tanto em pushes futuros à main
quanto em execuções manuais. Não existe bypass dos testes.

Em **Actions → CI → Run workflow**, selecionar `main`. Os jobs publicam exatamente
o SHA dessa execução, passam pela proteção de `prod` e verificam novamente o HEAD.
A primeira execução real precisa ser acompanhada e confirmada; testes simulados
não comprovam que SSH, registry, rede ou HTTPS da EC2 estejam funcionais.
Para suspender novas publicações, definir a variável como `false`. Isso não cancela
uma operação que já começou na EC2; acompanhe seu resultado antes de intervir.

## Atualização e migrations

O processo adquire `flock` local, confere divergências entre imagens em execução e
estado registrado, valida a versão atual, baixa imagens novas e grava journal
`.deploy/pending.json` e trava `.deploy/BLOCKED` antes de alterar containers.
Executa `up -d --no-deps --pull never --wait --wait-timeout 300 backend frontend`;
PostgreSQL e Caddy não são recriados. Repetir uma entrega idêntica apenas valida a saúde e registra `already-deployed`, preservando a versão anterior para rollback. Checks de containers incluem identidade da
imagem, processo e estado `healthy`. Depois verifica HTTPS para os três endpoints,
com TLS, timeout de 10 s por requisição e até seis tentativas por rota. Somente
então troca atomicamente `current.json` e guarda `previous.json`.

O identificador de migrations é o hash Git da árvore de migrations, incluindo
nomes/conteúdo. Se mudar em relação à versão registrada, o deploy falha **antes**
de trocar containers, até que haja backup verificado e revisão de compatibilidade.
Seguir [backup e restauração](backup-and-restore.md), aprovar retenção/destino e
conferir possibilidade de restauração. Após autorização para aquela revisão,
criar manualmente na EC2 o arquivo de aprovação de uso único:

```bash
install -d -m 700 /CAMINHO/EXISTENTE/.deploy/approved-migrations
touch /CAMINHO/EXISTENTE/.deploy/approved-migrations/<SHA_ALVO_COMPLETO>
```

Esse arquivo representa a revisão de migrations e backup pelo operador. Não o
crie automaticamente no CI. Flyway continua executando na partida do backend.
Se houver falha após iniciar atualização com migrations diferentes, **não** há
rollback automático de imagens/schema: a trava fica ativa para recuperação
assistida. A comparação não detecta alterações manuais no banco ou migrations
fora do diretório monitorado; conferir esses casos operacionalmente. Mesmo
migrations apenas aditivas são tratadas conservadoramente como mudança de schema.

## Rollback e interrupções

Quando migrations são idênticas, falha no `up` ou em saúde restaura as imagens
anteriores, repete os checks e mantém o job vermelho para registrar a falha.
Falha na restauração mantém `BLOCKED`; não confirma a versão nova. Falha no pull
não altera containers ou estado. Falhas de conexão antes de executar não iniciam
atualização. A trava local impede operações concorrentes mesmo fora do GitHub.

Rollback manual, como usuário de deploy:

```bash
python3 /CAMINHO/EXISTENTE/deploy-cd.py rollback /CAMINHO/EXISTENTE
```

Usa `previous.json`, exige migrations iguais, imagens disponíveis localmente e
valida saúde antes/depois. Não reconstrói imagens nem modifica dados. O estado
anterior à reversão passa a ser `previous.json`, mantendo rastreabilidade.

Se houver cancelamento, queda SSH, processo morto ou reinício no meio da operação,
`.deploy/pending.json` registra origem/destino, e `BLOCKED` impede outro deploy.
Inspecione containers, logs e estado do banco antes de liberar. A operação remota
tem timeout global de 20 minutos no transporte; uma interrupção pode exigir esse
procedimento mesmo se o host tiver continuado trabalhando após perder SSH.

Para recuperar, primeiro desabilitar novas entregas (`PROD_CD_ENABLED=false`) e
confirmar que nenhum processo de deploy ainda está ativo. Obter lock exclusivo
antes de intervir, preservar cópia privada do journal/histórico e comparar os IDs
em execução com `current.json` e `pending.json`. Se o schema permitir, restaurar
as duas imagens imutáveis de `pending.json.from` usando o Compose existente e
variáveis **somente para aquele processo**; reaplicar os checks internos e HTTPS.
Se o schema tiver mudado, preparar versão corretiva compatível ou seguir o runbook
de restauração em ambiente separado, com autorização específica e avaliação de
perda de dados. Não reverter schema ou restaurar dumps automaticamente.

Depois de provar a versão saudável e o estado de migrations, atualizar o estado
sob lock para essa versão, guardar o journal no histórico e remover a trava apenas
como etapa final. Se a recuperação devolveu exatamente `current.json`, preserve-o;
se foi uma versão corretiva, registre SHA, hash de migrations e digests reais. Esse
procedimento é assistido porque não há decisão segura universal após mudança de
schema/interrupção. Nunca use `down -v`, volume prune ou reset de checkout.

## Logs, versão e investigação

- GitHub Actions: logs de `publish`/`deploy`, resultado do job e Step Summary com
  SHA e digests. As dependências `needs` impedem CD se algum check obrigatório falhar.
- EC2: `python3 deploy-cd.py status /CAMINHO/EXISTENTE` mostra versão atual,
  anterior e trava; `.deploy/events.jsonl` registra tentativas/resultados com UTC.
- Logs locais de serviços: `docker compose --env-file .env.demo -f compose.demo.yml
  logs --tail 80 backend frontend proxy`. Logs podem conter dados operacionais;
  revisar/sanitizar antes de compartilhar. O script não envia o conteúdo de `.env`
  ou o resultado completo de Compose aos logs do Actions.
- SSH: conferir host, porta, fingerprint, usuário, permissão da chave e rede do runner.
- Pull: conferir login Docker do usuário remoto, permissão dos pacotes, digests,
  arquitetura, disco e conectividade GHCR. Não podar imagens de recuperação.
- Saúde: conferir readiness/banco/Flyway, memória, frontend, DNS, certificado e Caddy.
  O script recusa atualizar se a versão atual já estiver sem saúde ou houver drift;
  nesse caso, a recuperação precisa ser operacional, antes de retomar o CD.

## Validação e ativação pendente

Antes de ativar: validar YAML/workflow, scripts, Compose com valores sintéticos,
permissões/secrets, testes de falha e comandos remotos. Executar
`python3 -m unittest discover -s scripts -p '*_test.py'` para testes isolados de
sucesso, pull/health/rollback, schema, drift e proteção do transporte SSH. Eles
substituem Docker/HTTPS/SSH por simuladores; não alteram infraestrutura.
A CI continua rodando Maven/Testcontainers e as verificações do frontend.

É necessário conferir a instalação real, preparar usuário/rede/login GHCR,
instalar o script, configurar GitHub/branch protections e fazer bootstrap antes
de autorizar a ativação e acompanhar o primeiro deploy. Um deploy funcional só
pode ser afirmado após essa validação real. Nenhum commit é feito automaticamente.

Referências: [dependências entre jobs](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-jobs),
[publicação GHCR](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images),
[Compose up e opções de espera/dependências](https://docs.docker.com/reference/cli/docker/compose/up/).


## Resultado da validação local — 20/10/2026

- `actionlint` 1.7.7: workflow válido (checksum do binário conferido com release
  oficial); verificação ShellCheck desabilitada por ausência da ferramenta.
  Todos os blocos `run` passaram separadamente em `bash -n`.
- Compose existente: `config --quiet` passou com ambiente sintético e sem carregar
  arquivos de produção. Revisados gates, dependências, SHA, permissões e nomes
  de secrets. Não há comando de remoção de volumes nos jobs novos ou scripts.
  O `down --volumes` preexistente continua restrito ao ambiente descartável de CI.
- Python: 24 testes passaram, incluindo os testes existentes e os novos testes
  de deploy/SSH. Simulações cobrem sucesso, idempotência, bootstrap, lock local,
  falha de pull, falha HTTPS, rollback/falha de rollback, drift, schema e injeção
  em parâmetros SSH. `py_compile` passou.
- Backend: `./mvnw --batch-mode --no-transfer-progress verify` passou com 177
  testes, sem falhas ou skips, usando PostgreSQL descartável via Testcontainers.
- Node: `node --test scripts/*.test.mjs` passou com 23 testes.
- Frontend: `npm ci` e `npm run check` passaram (lint, tipos e build). A instalação
  anterior estava incompleta; foi corrigida usando o lockfile existente, sem
  alterar dependências versionadas. `npm ci` reportou cinco vulnerabilidades de
  severidade alta nas dependências atuais; atualização requer trabalho separado.
- `git diff --check` passou. Não foi feita nova execução da suíte completa de
  containers/Playwright, publicação GHCR, conexão SSH ou validação da EC2.
  Essas validações externas permanecem necessárias antes de declarar CD funcional.
- Nenhum deploy, alteração AWS/GitHub, cadastro de credencial ou commit foi feito.
