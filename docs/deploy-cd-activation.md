# Acompanhamento da ativação — 20/10/2026

Após autorização do usuário para avançar com a configuração e o primeiro deploy:

- A autenticação local do GitHub foi validada fora do sandbox; a conta possui
  permissão administrativa em `GabrielHDias/Finance_Manager`.
- A main remota, antes da publicação do CD, aponta para `c756f3614c0509976e97bba2009878dfca471d79`.
  Seus checks `backend`, `frontend`, `containers` estão concluídos com sucesso.
- Foi criado o Environment `prod`, com política de deploy que permite somente
  a branch `main`. Ainda não foram configurados reviewers obrigatórios.
- Foi definida a repository variable `PROD_CD_ENABLED=false`.
- Foi definida `PROD_URL` no Environment com a URL HTTPS informada na tarefa.
- Não foram criados secrets ou chaves de deploy; nenhum token foi cadastrado.
- O SSH para `52.91.144.26:22` expirou antes de autenticar. Foram usados
  StrictHostKeyChecking, modo não interativo e a entrada já existente de
  known_hosts; nenhuma chave de host foi aceita automaticamente.
- A consulta HTTPS pública de `/api/health/ready` retornou HTTP 401. A versão
  instalada e sua configuração ainda precisam ser identificadas. Não há evidência
  suficiente para atribuir o resultado a uma causa específica. O bootstrap exige
  saúde válida e não deve ignorar essa resposta para forçar a adoção.
- O usuário autorizou explicitamente criar o commit e enviá-lo à main. A publicação
  do workflow será feita mantendo `PROD_CD_ENABLED=false`, para executar CI sem CD.
- A chave administrativa indicada foi localizada em
  `/home/thed/aws/finance-manager-demo.pem`, com modo 400. Uma nova tentativa com
  ela também expirou antes da autenticação. Seu conteúdo não foi exibido nem
  cadastrado como chave de deploy. A automação deverá usar outra chave exclusiva.
- O IP público observado desta máquina é `45.234.3.40`; foi solicitada confirmação
  do IP/estado da EC2 e acesso TCP 22 restrito a `45.234.3.40/32`, se aplicável.
- Nenhum serviço da EC2 ou recurso AWS foi alterado e nenhum deploy foi executado.

## Informação pendente do usuário

Confirmar no console AWS se a instância está em execução em `52.91.144.26` e
permitir acesso SSH restrito desta máquina, ou informar host/porta corretos.
O timeout ocorre antes da autenticação; a chave ainda não pôde ser validada.
Nunca compartilhar o conteúdo da chave no chat.

Depois de obter acesso, inspecionar a instalação real antes de preparar bootstrap,
reconciliar a saúde/versão, instalar o script, cadastrar uma chave exclusiva,
configurar acesso GHCR de leitura e validar as condições do [runbook](deploy-cd.md).
Somente habilitar `PROD_CD_ENABLED` depois dessas etapas e acompanhar o primeiro
deploy. A autorização para avançar não comprova que a infraestrutura esteja pronta.

## Continuação após ajuste de rede

- O usuário confirmou o IP da instância e ajustou o acesso SSH. A conexão com o
  `.pem` administrativo passou, sem desabilitar verificação da chave do host.
- A instalação real é `/home/ubuntu/finance-manager-demo`, projeto
  `finance-manager-demo`, com os quatro serviços saudáveis pelos checks antigos.
- A revisão instalada foi identificada como
  `2068eb335e4adcb82dd7d0532143ed186a661550`. Migrations V1/V2 estão aplicadas e
  seus arquivos não diferem dos da main inspecionada. A instalação legada usa
  `/accounts` retornando 401 como check de API; não possui os novos endpoints
  de saúde. A adoção precisa tratar explicitamente essa versão legada.
- O banco não publica portas. O volume efetivo é
  `finance-manager-demo_postgres_data`; volumes de certificados Caddy também
  foram identificados. Nenhum container foi recriado ou banco alterado.
- Não há instance profile IAM, SSM está inativo e não há login GHCR de root no
  host. O usuário escolheu AWS OIDC para autorizar temporariamente o IP de cada
  runner, mantendo a porta SSH restrita.
- Foi aberta uma sessão AWS no navegador integrado pelo próprio usuário.
  Consultas de leitura confirmaram a conta e a ausência do provider GitHub e da
  role proposta. Não foram exportadas credenciais AWS.
- A implementação de OIDC está sendo preparada em `codex/deploy-oidc`, com
  gerador de trust/policy, regras temporárias identificadas por execução e
  limpeza de regras expiradas. Ver [guia OIDC](deploy-oidc.md).
- Os 34 testes Python e a validação actionlint dos dois workflows passaram
  localmente. A criação do IAM e a ativação de CD ainda precisam ser concluídas.
