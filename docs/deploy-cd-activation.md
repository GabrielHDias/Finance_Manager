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
