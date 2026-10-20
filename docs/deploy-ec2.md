# Demonstração na EC2

A configuração `compose.demo.yml` executa PostgreSQL 17, API Java 21,
frontend Next.js e Caddy. Use uma EC2 Linux x86_64 com Docker e Compose v2,
com pelo menos 2 GiB de RAM e 20 GiB de disco para esta demonstração.
Os builds são feitos no computador de desenvolvimento. Os limites de memória
somam 1536 MiB; acompanhe a utilização com `sudo docker stats --no-stream`.
Esta configuração destina-se a uma demonstração de baixo tráfego, sem alta disponibilidade.

## Pacote de entrega

No computador de desenvolvimento, na revisão que deseja entregar:

```bash
bash scripts/package-demo.sh /tmp/finance-manager-release
```

O script constrói imagens linux/amd64 e compila o frontend com
`NEXT_PUBLIC_API_URL=/api`. Ele inclui as imagens de PostgreSQL e Caddy para
que o servidor não precise construir nem baixar imagens. O pacote contém
manifesto SHA-256, identificador da entrega e configurações, sem `.env` ou
chaves privadas. A revisão curta é acompanhada do horário UTC da construção;
confira `git status` antes de empacotar, pois alterações locais são incluídas.
Não é necessário publicar imagens em um registry para esta modalidade.

Transfira o conteúdo da pasta do pacote por SCP para uma pasta nova na EC2,
por exemplo `~/finance-manager-demo`. A chave privada permanece somente no
computador de desenvolvimento. Verifique a identidade do host SSH e use o IP
público atual da instância.

## Primeira instalação

Na EC2, dentro da pasta do pacote:

```bash
sha256sum -c SHA256SUMS
sudo docker load -i images.tar.gz
```

Crie o arquivo de configuração apenas na primeira instalação. O comando falha
se `.env.demo` já existir para preservar as credenciais do banco:

```bash
(
  set -eu
  umask 077
  set -C
  {
    cat images.env
    printf 'DB_PASSWORD=%s\n' "$(openssl rand -hex 32)"
    printf 'JWT_SECRET=%s\n' "$(openssl rand -hex 64)"
    printf '%s\n' 'APP_ORIGIN=http://localhost:8088'
  } > .env.demo
)
sudo docker compose --env-file .env.demo -f compose.demo.yml config --quiet
sudo docker compose --env-file .env.demo -f compose.demo.yml up -d --wait --wait-timeout 300
sudo docker compose --env-file .env.demo -f compose.demo.yml ps
```

Os segredos são gerados no servidor e não devem ser enviados ao Git ou ao chat.
O volume `finance-manager-demo_postgres_data` preserva o banco entre reinícios
ou recriações de containers. `restart: unless-stopped` reinicia os serviços após
reiniciar a EC2. Os logs têm rotação de 10 MiB, com três arquivos por serviço.
Somente o proxy publica portas; banco, API e frontend ficam na rede Docker.
A documentação Swagger é desativada nesta modalidade.

## Acesso inicial por túnel SSH

Sem domínio, o proxy fica vinculado a `127.0.0.1:8088` na EC2.
No computador de desenvolvimento, mantenha este comando aberto:

```bash
ssh -N -o ExitOnForwardFailure=yes -L 8088:127.0.0.1:8088 \
  -i /caminho/finance-manager-demo.pem ubuntu@IP_PUBLICO
```

Abra `http://localhost:8088`. O tráfego entre computador e EC2 passa pelo SSH.
Use exatamente esse endereço para corresponder à origem configurada.
Não é necessário expor HTTP publicamente nesta etapa. A regra SSH do security
group deve permitir somente o IP atual do administrador. Não abra portas
3000, 5432 ou 8080 no security group.

## HTTPS com domínio

Quando houver um domínio, aponte seu registro DNS A para o IPv4 público atual
da EC2. Se houver um registro AAAA, ele também precisa apontar para um IPv6
configurado e acessível nessa instância. Permita TCP 80 e 443 no security group.
Atualize estas variáveis em `.env.demo`, preservando as credenciais e imagens:

```dotenv
APP_ORIGIN=https://finance.exemplo.com
SITE_ADDRESS=finance.exemplo.com
BIND_ADDRESS=0.0.0.0
HTTP_PORT=80
HTTPS_PORT=443
```

Reaplique `docker compose ... up -d --wait`. O Caddy emite o certificado e
redireciona HTTP para HTTPS. Seus dados e certificados são persistentes.
A API utiliza o prefixo `/api` na mesma origem; o proxy remove esse prefixo
antes de encaminhar à API. A origem permitida pelo CORS deve coincidir com
o endereço utilizado no navegador. Não use HTTP público para inserir senhas.

O IPv4 atribuído automaticamente pode mudar após parar e iniciar a EC2.
Um Elastic IP pode estabilizar o endereço, mas deve ter seu custo avaliado.

## Endereço gratuito para a demonstração

Quando não houver domínio próprio, é possível utilizar o DNS público do
[sslip.io / nip.io](https://nip.io/). Por exemplo, se o IPv4 público da EC2 fosse
`203.0.113.10`, o nome `finance-manager.203-0-113-10.sslip.io` resolveria para
esse IP. Substitua pelo IPv4 real; esse endereço de exemplo não deve ser usado
para publicar o serviço.

Configure as mesmas cinco variáveis da seção HTTPS, usando o nome escolhido
em `SITE_ADDRESS` e sua URL HTTPS em `APP_ORIGIN`. Confirme que o nome resolve
para a EC2 antes de reaplicar o Compose. O Caddy obtém o certificado e a
conexão passa diretamente para a EC2; esse serviço fornece apenas o DNS.
Não é necessário alterar o build do frontend, que continua usando `/api`.

Antes de publicar, faça backup do banco e uma cópia privada de `.env.demo`.
Confirme as regras TCP 80/443 do security group, mantenha o SSH restrito ao IP
do administrador e confira o certificado HTTPS no acesso externo. O banco e
as portas internas da aplicação devem continuar sem publicação.

A publicação permite que visitantes criem contas. Os dados existentes continuam
exigindo autenticação e são filtrados pelo usuário. Cadastros e tráfego podem
consumir recursos da EC2; esta entrega continua sendo uma demonstração de baixo
tráfego.

Esse nome depende da disponibilidade de um DNS de terceiros e do IPv4 embutido.
Se o IP mudar após parar/iniciar a EC2, atualize o nome, `SITE_ADDRESS` e
`APP_ORIGIN`, reaplique o Compose e compartilhe a nova URL. Para um endereço
permanente, use domínio próprio e avalie a necessidade de um IP estável.

## Backup e recuperação

Antes de atualizar, faça um backup fora do volume do banco:

```bash
umask 077
mkdir -p backups
sudo docker compose --env-file .env.demo -f compose.demo.yml \
  exec -T --interactive=false db pg_dump -U finance_manager -d finance_manager -Fc \
  > "backups/finance-manager-$(date -u +%Y%m%d%H%M%S).dump"
```

Copie o backup e `.env.demo` para um local seguro fora da EC2. Um backup no
mesmo disco não protege contra a perda da instância. Os dumps contêm dados
financeiros e devem permanecer privados. Para recuperar, use uma instalação
separada e um banco vazio, importe com `pg_restore -U finance_manager -d
finance_manager --exit-on-error` dentro do container PostgreSQL e confira os
dados antes de substituir a instalação existente. Não restaure sobre o banco
em uso.

## Atualizações e operação

Construa e valide um novo pacote. Faça backup e preserve a entrega anterior.
Carregue as imagens novas com `docker load`; atualize somente `BACKEND_IMAGE`
e `FRONTEND_IMAGE` em `.env.demo` conforme o novo `images.env`. Copie as
configurações novas após revisar as diferenças. Reaplique `up -d --wait`.
Mantenha as credenciais originais do banco. Migrações Flyway executam na partida;
reverter imagens pode exigir recuperar o backup se o esquema não for compatível.

```bash
sudo docker compose --env-file .env.demo -f compose.demo.yml logs --tail 80
sudo docker stats --no-stream
sudo docker compose --env-file .env.demo -f compose.demo.yml stop
sudo docker compose --env-file .env.demo -f compose.demo.yml start
```

Não execute `down --volumes` no servidor: ele apaga o banco e os certificados.
Os testes de `scripts/smoke-test.mjs` criam usuários e registros; execute-os
somente no ambiente isolado de validação, com `SMOKE_API_URL` terminando em
`/api`. Na EC2, prefira verificações de leitura e um cadastro manual de teste.

A EC2, EBS, IPv4 público e eventuais transferências consomem benefícios ou
créditos conforme as condições da conta. Parar a EC2 reduz o consumo de
computação, mas não elimina todos os custos de armazenamento e outros recursos.
Acompanhe o plano, saldo e expiração no Billing. Mantenha backups antes do fim
do plano gratuito. Não habilite serviços pagos adicionais sem revisar seus custos.

Referências: [Caddy handle_path](https://caddyserver.com/docs/caddyfile/directives/handle_path),
[Docker Compose](https://docs.docker.com/reference/compose-file/services/),
[planos AWS](https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/free-tier-plans.html).
