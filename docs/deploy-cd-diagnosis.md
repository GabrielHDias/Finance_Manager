# Diagnóstico anterior à implementação do CD — 20/10/2026

Inspeção local do commit `c756f36`, com checkout inicialmente limpo.

- A referência `origin/HEAD` identifica `main` como branch principal; o remoto é `GabrielHDias/Finance_Manager`.
- `.github/workflows/ci.yml` já executa Maven verify com Java 21/Testcontainers PostgreSQL, lint/tipos/build Next.js com Node 24 e testes integrados Docker/Playwright. Não há CD ou registry configurado no repositório.
- Os Dockerfiles existentes são multi-stage, têm bases fixadas por digest e usuários sem root. O frontend recebe a URL pública no build; produção exige `/api`.
- `docker-compose.yml` atende desenvolvimento (PostgreSQL com porta loopback); `compose.app.yml` atende validação local. `compose.demo.yml` é a modalidade documentada para EC2: serviços `db`, `backend`, `frontend`, `proxy`, projeto `finance-manager-demo` e volumes persistentes para PostgreSQL e Caddy. Somente o proxy publica portas.
- `deploy/Caddyfile` encaminha `/api/*` ao backend e o restante ao frontend. HTTPS depende das variáveis existentes em `.env.demo`; certificados ficam em volume persistente.
- O processo anterior, descrito em `docs/deploy-ec2.md` e `scripts/package-demo.sh`, constrói localmente imagens linux/amd64, envia um pacote por SCP e usa `docker load`. A atualização troca referências de imagens em `.env.demo` e reaplica Compose manualmente.
- Já existem health checks Compose para os quatro serviços. `/health/ready` verifica conexão com PostgreSQL, `/health/live` indica liveness; via proxy usam prefixo `/api`. Não é necessário adicionar Actuator.
- Flyway executa na partida; Hibernate valida o schema. Há migrations V1/V2. Reversão de imagem não reverte schema.
- `.env.demo` contém segredos gerados no servidor; `.env*` reais são ignorados. Não será substituído pelo CD.

## Decisão

Acrescentar publicação GHCR e deploy SSH à CI existente, sem duplicar suas verificações. GHCR aproveita o token do GitHub para publicação e dispensa introduzir IAM/ECR/SSM sem evidência de uso atual. Imagens terão tags SHA e serão implantadas por digest. O Compose, Caddy e arquivos de ambiente instalados permanecerão sob controle do operador; o CD substitui somente imagens de backend/frontend.

A ativação fica desabilitada por variável explícita. A adoção inicial deve verificar os nomes/projeto/volumes reais e capturar as imagens atualmente em execução para rollback. Mudanças de migrations exigem aprovação local específica; falhas após essas mudanças exigem recuperação assistida.

## Limites da evidência

Não foi acessada a EC2, a configuração privada do GitHub, credenciais, Security Groups ou AWS. A documentação do repositório não comprova o estado real da instância. Não foram realizados deploy, instalação ou alterações externas. A compatibilidade com a instalação efetiva deve ser conferida antes da adoção.
