# Referência para ativação do CD

Este documento público preserva as lições técnicas da ativação inicial. Não é
inventário nem atestado do estado atual de produção. Hosts, IPs do operador,
caminhos de chaves, diretórios/volumes efetivos e evidências de acesso devem ficar
em registro operacional privado. Consulte o [runbook de CD](deploy-cd.md) e o
[guia OIDC](deploy-oidc.md) para os procedimentos completos.

## Preparação e acesso

- Conferir a revisão alvo e os checks `backend`, `frontend`, `secrets` e `containers`.
  Manter `PROD_CD_ENABLED=false` durante a preparação; habilitar somente após
  validar adoção, acesso e saúde.
- Usar o Environment `prod` restrito à `main`, com política de revisão definida
  pelo responsável. Cadastrar URL e variáveis operacionais no GitHub, sem
  reproduzi-las em documentação pública.
- Um timeout SSH anterior à autenticação pede conferir host, porta e regra de
  rede. Não comprova chave inválida. Preservar `StrictHostKeyChecking`, modo não
  interativo e known_hosts verificado por canal confiável.
- Exemplo fictício: servidor `192.0.2.10:22`, operador `198.51.100.20/32` e chave
  administrativa em `/caminho/privado/admin.pem` (modo 0400). Esses endereços são
  reservados para documentação; substitua-os somente na configuração privada.
  Nunca compartilhar o conteúdo da chave em chat, logs ou commits.
- A chave administrativa não deve ser cadastrada como chave de CD. Usar uma
  identidade exclusiva para a automação e acesso GHCR temporário por job.

## Adoção de uma instalação legada

1. Inspecionar a instalação antes de preparar bootstrap. Registrar privadamente
   diretório, projeto Compose, volumes, revisão, digests e fingerprint das
   migrations. Exemplos genéricos: `/srv/app`, projeto `app-demo` e volume
   `app-demo_postgres_data`; não são recursos reais a operar.
2. Comparar migrations e identificar o perfil de saúde da revisão instalada.
   Uma revisão legada pode usar `/accounts` com HTTP 401 como check e não ter
   `/health/ready`. Não ignorar falhas de saúde para forçar adoção; usar o perfil
   legado explícito previsto no runbook até a atualização validada.
3. Preservar `.env.demo`, Compose e volumes. Capturar baseline com imagens
   imutáveis e registro privado `.deploy/adoption.json` para conferir a
   preservação depois da primeira entrega.
4. Verificar acesso IAM/SSM/registry efetivo antes de escolher o mecanismo.
   Para OIDC, revisar trust/policy, subject e grupo de segurança conforme o guia.
   A regra SSH temporária deve identificar a execução e ter revogação e limpeza
   de regras expiradas.
5. Instalar scripts/gateway sob a custódia definida no runbook. O gateway deve
   aceitar somente status e deploy permitido por digest; testar a rejeição de
   comandos arbitrários. Cadastrar chave exclusiva e known_hosts nos Secrets do
   Environment sem imprimir seus valores.
6. Validar abertura/revogação a partir do runner, saúde e rollback protegido.
   Acompanhar o primeiro CD e guardar evidências sanitizadas. Aprovação para
   avançar não substitui a verificação de prontidão da infraestrutura.

## Evidências públicas e privadas

Publicar somente resultados sanitizados de testes e explicações reproduzíveis.
Manter fora do Git o diário de acessos, identificação da estação, inventário do
servidor e detalhes da sessão do operador. Notas antigas removidas da versão
atual continuam no histórico Git; veja [exposição pública e histórico](public-exposure.md).
