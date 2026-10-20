# Acesso temporário do GitHub à EC2 por AWS OIDC

A alternativa OIDC foi escolhida pelo usuário. O CD permanece desabilitado até a
configuração IAM, adoção da instalação e validação real do primeiro deploy.

## Evidência e desenho

A inspeção SSH identificou a região `us-east-1` e um único Security Group ligado à
instância. Não há instance profile IAM; o SSM não está ativo. O GitHub informou
`use_immutable_subject=true`, portanto a trust policy precisa incluir IDs imutáveis
do proprietário/repositório, além do Environment `prod`. Não use o subject antigo
baseado somente nos nomes. Consulte a identidade efetiva com:

```bash
gh api repos/GabrielHDias/Finance_Manager/actions/oidc/customization/sub
```

O job `deploy` recebe `id-token: write` e assume a role por uma hora usando a action
oficial AWS, fixada por SHA. Confere a conta esperada e não recebe access keys
persistentes. O Environment já está restrito à branch `main`.

`scripts/deploy-network.py open` descobre o IPv4 público do próprio runner e abre
**somente TCP 22 para aquele IPv4 /32**. A regra é marcada com `ManagedBy`, `Owner`,
`GithubRun` e `ExpiresAt`. Uma etapa `always()` revoga apenas regras pertencentes à
mesma execução. Regras de administrador e de outras execuções são preservadas;
uma regra equivalente já existente não é adotada nem apagada.

Falhas de limpeza deixam o job vermelho. Um encerramento abrupto pode impedir a
etapa final. O próximo deploy remove regras gerenciadas expiradas; o workflow
`deploy-network-cleanup.yml` também executa essa limpeza uma vez por hora e por
execução manual na main. Expiração é de uma hora, maior que o timeout de 25 min do
deploy. Security Groups não possuem TTL automático: as tags são interpretadas
pelo script, e o agendamento GitHub pode atrasar ou ser suspenso. Inspecione o
Security Group/CloudTrail e remova operacionalmente regras órfãs se a limpeza
falhar. A limpeza agendada usa `PROD_CD_ENABLED=true`; a manual também funciona
com CD desabilitado, desde que o IAM e as variáveis estejam configurados.

## Revisão do escopo IAM

O gerador `scripts/prepare-aws-oidc.py` produz `trust.json` e `permissions.json`;
ele não acessa AWS e não modifica recursos. Recebe conta, região, Security Group e
subject imutável exato. Não aceita wildcards no subject nem outro Environment.

A permissão de alteração de ingress é limitada ao **ARN do único Security Group**
aprovado. A role também pode consultar regras na região e marcar regras no ato de
criação. Ela não pode criar IAM, iniciar/parar instâncias, alterar volumes, modificar
egress ou administrar outros grupos. A autorização de regras marcadas usa também
o recurso `security-group-rule`, requerido no fluxo com TagSpecifications.

Limite importante: as ações EC2 de ingress não oferecem condições IAM para impor
porta 22 e CIDR /32. Esses limites são aplicados pelo código revisado do workflow,
não pelo IAM. A role pode gerenciar outras regras de entrada **nesse grupo** se
seu código confiável for alterado ou comprometido. Proteger main, o Environment e
a revisão do workflow é essencial. Não alegue que a policy limita o CIDR/porta ou
que restringe a revogação por tag; a API autoriza revogação no recurso do grupo.

Não altere políticas/trust de roles já existentes sem revisar diferenças. O nome
proposto da nova role é `FinanceManagerGitHubDeploy`, duração máxima 3600 segundos.
O provider é `https://token.actions.githubusercontent.com`, com audience
`sts.amazonaws.com`. Se o provider já existir, confira suas audiences antes de
reutilizar; não o substitua. Não crie access keys para esta integração.

Exemplo de preparação, substituindo dados pelos valores inspecionados:

```bash
python3 scripts/prepare-aws-oidc.py \
  --account <CONTA_12_DIGITOS> --region <REGIAO> --group <SECURITY_GROUP_ID> \
  --subject 'repo:GabrielHDias@<OWNER_ID>/Finance_Manager@<REPO_ID>:environment:prod' \
  --output /tmp/finance-manager-oidc
```

Depois de revisar/aprovar os documentos, cadastrar o provider/role e a policy no
IAM usando a sessão administrativa AWS. Validar conta e permissões antes de
executar qualquer criação. O script gerador não instala ou modifica IAM.

## Variáveis adicionais no Environment prod

| Nome | Valor |
| --- | --- |
| `AWS_ROLE_ARN` | ARN da role criada/revisada |
| `AWS_REGION` | Região real da instância |
| `AWS_ACCOUNT_ID` | Conta esperada, 12 dígitos |
| `DEPLOY_SECURITY_GROUP_ID` | Grupo associado à instância e autorizado pela policy |

`DEPLOY_PORT` deve ser 22 para este procedimento. O check de configuração recusa
outras portas antes de abrir a regra. Variáveis SSH/registry e bootstrap continuam
conforme o [runbook principal](deploy-cd.md). OIDC resolve o acesso AWS à regra de
rede; ele não substitui a chave SSH exclusiva. A leitura GHCR usa o token temporário do job, encaminhado por stdin SSH e removido ao término.

## Validação e recuperação

Antes de habilitar CD: validar trust/policy, testar a assunção da role, confirmar
que a regra criada usa /32/TCP22, executar SSH com known_hosts verificado e provar
que a regra foi removida ao final. Ensaiar falha após abertura e executar a limpeza
manual para confirmar sua identificação por tags. Conferir que regras manuais
permaneceram intactas. Nenhum desses testes exige reiniciar a aplicação.

Os testes isolados em `scripts/network_test.py` cobrem limites de porta/CIDR,
preservação de regras manuais/de outras execuções, regras expiradas, duplicidade,
intenção registrada antes da API, revogação idempotente e escopo/subject da policy.
São simulações e não comprovam a autorização IAM real. Validar workflows com
`actionlint`, blocos shell com `bash -n` e testes Python antes da integração.

Referências: [GitHub OIDC na AWS](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws),
[ações e recursos EC2 para IAM](https://docs.aws.amazon.com/service-authorization/latest/reference/list_ec2.html),
[API AuthorizeSecurityGroupIngress](https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_AuthorizeSecurityGroupIngress.html).
