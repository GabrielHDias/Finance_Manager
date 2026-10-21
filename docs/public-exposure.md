# Prevenção de exposição pública e tratamento do histórico

## Varredura de segredos

A CI executa Gitleaks **8.30.1**, com binário Linux x64 verificado por SHA-256
fixado no workflow a partir dos [checksums da release oficial](https://github.com/gitleaks/gitleaks/releases/tag/v8.30.1).
Atualizações exigem rever versão, checksum e regras em conjunto. O job `secrets`
tem somente `contents: read`, checkout sem credencial persistida e histórico
completo (`fetch-depth: 0`). Executa em push, pull request e execução manual;
`containers` depende dele, bloqueando a publicação de imagens em caso de falha.
Não usa secrets do Environment, licença, comentários em PR ou upload de relatório.

O comando varre os commits alcançáveis pelas referências disponíveis no checkout,
incluindo o histórico anterior à mudança. Não cobre referências inacessíveis,
forks, caches, logs, arquivos ignorados/não commitados ou metadados pessoais de
autores. Em PR, a revisão de merge também é examinada. Para exigir o check antes
do merge, o administrador precisa adicionar `secrets` à proteção/ruleset da
branch; a alteração do workflow sozinha não configura essa proteção.

Para repetir localmente, instale a mesma release verificando o SHA-256 mostrado
no workflow e coloque `gitleaks` no PATH. Na raiz do checkout:

```bash
python3 scripts/security-scan-selftest.py
gitleaks git --config .gitleaks.toml --redact=100 --no-banner --log-opts=--all .
```

O autoteste cria dados sintéticos em diretório temporário e os remove ao terminar.
Confere detecção, segredo apagado ainda no histórico e limites das exceções.
Saída/relatórios do scanner não são publicados; a CI usa redação integral.
Para conferir alterações locais ainda não commitadas, exporte somente os arquivos
que deseja revisar para diretório descartável e execute `gitleaks dir` nele com
a configuração do projeto (caminhos relativos à raiz e `--redact=100`).

As duas exceções em `.gitleaks.toml` combinam **arquivo exato e conteúdo exato**,
somente para `generic-api-key`:

- A chave JWT determinística de `JwtRealMvcTest.java`, incluindo a representação
  Base64 e a decodificada, é fixture do teste MVC; nunca usar em configuração real.
- Uma linha de comentário de `scripts/deploy-ec2.py` explica tokens opacos; não
  contém credencial. Acrescentar conteúdo à linha invalida a exceção.

Não ignorar diretórios de testes, commits inteiros ou todas as ocorrências de uma
regra. Novos achados precisam de triagem; uma credencial real exige revogação/
rotação antes de considerar limpeza. O scanner não substitui revisão humana.

O `.gitignore` bloqueia chaves, keystores e dumps/pares de backup usados pelo
projeto. Arquivos SQL de migrations e CSVs sintéticos continuam permitidos.
Não há fixture de chave ou dump versionada que exija exceção no momento. Uma
fixture futura requer exceção por arquivo e revisão de conteúdo; evitar `git add
-f` como rotina. Arquivos já rastreados não passam a ser ignorados retroativamente.

## O que continua acessível

A sanitização atual remove do documento de ativação os IPs reais, caminho da
chave administrativa, inventário da instalação e diário operacional. Remove
uma anotação de acesso temporário da documentação dos gráficos. A autoria pública
intencional do projeto e os namespaces de código permanecem.

Isso **não apaga versões anteriores**. Os conteúdos removidos continuam em
commits antigos, branches/tags que os alcançam e possivelmente referências de
PR, forks, clones e caches. Email pessoal em autores/committers também permanece;
os commits `a0b4b01` e `64ecbe6` contêm identidade automática com hostname/domínio
local. Esses metadados foram confirmados no checkout, sem reproduzi-los aqui.
Gitleaks não é um detector de dados pessoais e não remove esses metadados.

Para próximos commits, configure explicitamente a identidade e copie o endereço
noreply exato das configurações de email da sua conta GitHub:

```bash
git config --local user.name 'Nome público escolhido'
git config --local user.email 'SEU_ENDERECO_NOREPLY_DO_GITHUB'
git config --local user.useConfigOnly true
```

Substitua os exemplos antes de executar. A configuração afeta somente commits
futuros e não foi alterada automaticamente nesta revisão.

## Opções para o histórico (decisão separada)

1. **Manter o histórico**, sanitizar a versão atual e usar noreply daqui em diante.
   Preserva SHAs, assinaturas e referências; aceita a exposição histórica descrita.
2. **Reescrever seletivamente em clone isolado**, com `git-filter-repo`: substituir
   os dados dos documentos em todas as referências afetadas e mapear explicitamente
   emails/identidades antigos de autor e committer para a identidade pública
   escolhida. Inventariar branches, tags e referências de PR; validar conteúdo e
   metadados antes de qualquer publicação. `.mailmap` melhora a apresentação em
   algumas ferramentas, mas não remove os dados dos objetos Git.
3. **Publicar um novo repositório com snapshot sanitizado** reduz a continuidade
   pública do histórico, mas perde contexto/blame e referências de colaboração.
   Enquanto o repositório antigo e suas cópias existirem, a exposição anterior
   continua disponível; essa opção não equivale a apagar os dados.

Reescrever muda os SHAs dos commits alterados e seus descendentes, invalida
assinaturas e pode quebrar tags, links, PRs e automações dependentes de SHAs.
Exige coordenação, janela sem contribuições, autorização específica para
force-push/ajustes de proteção e novos clones ou limpeza cuidadosa dos clones
existentes para evitar reintrodução. Em instalações com CD, SHAs também podem
referenciar imagens/baselines: avaliar esse impacto antes de qualquer operação.

Mesmo após reescrever branches/tags, cópias de terceiros e referências/caches
podem preservar os dados. A remoção em forks depende dos donos; eventual remoção
de referências/caches no GitHub depende do suporte e dos critérios aplicáveis.
Consulte os [impactos e limites documentados pelo GitHub](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository).
Não há promessa de apagamento global. Nenhuma reescrita histórica, mudança de
visibilidade ou operação de infraestrutura faz parte desta alteração.
