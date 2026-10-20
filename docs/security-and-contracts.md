# Segurança e contratos HTTP

## Sessão

HS256, chave Base64 externa, expiração de 3600 segundos, tolerância temporal padrão do validador Spring (60 segundos). Decoder real exige exp/iat e rejeita assinatura errada, algoritmo diferente e subject ausente/não decimal/não positivo/fora de Long. Não registrar tokens, senhas ou chaves. sessionStorage tem escopo de aba; logout remove a cópia local e não revoga tokens. Em 401, o cliente limpa o armazenamento e conduz ao login. O CTA inicial ignora tokens expirados. Isso é apenas uma otimização de interface: autenticação é responsabilidade do backend.

## Proteção contra abuso

Não há limitador declarado no Caddyfile do repositório; controles externos não foram acessados. A camada responsável aqui é o filtro da API, antes da autenticação, para POST `/auth/login` e `/users`.

| Propriedade Spring | Padrão |
|---|---:|
| `app.auth-rate-limit.login` | 60 |
| `app.auth-rate-limit.registration` | 20 |
| `app.auth-rate-limit.window-seconds` | 600 |
| `app.auth-rate-limit.capacity` | 10000 |

Os limites são por endereço da conexão e rota, não por existência de conta. Quando a capacidade de chaves é atingida, novas chaves recebem 429 até liberar janelas, preservando memória limitada. Retorna JSON padrão e `Retry-After` em segundos; a janela recupera automaticamente. Métrica recomendada: contagem de respostas 429 por rota e réplica e taxa de recuperação, sem rótulos por IP/email/token. O filtro também mantém contador agregado local de recusas para instrumentação; não foi aberto endpoint administrativo.

`server.forward-headers-strategy=none` é intencional: `X-Forwarded-For` e similares não alteram identidade. Em demo todos os usuários passam por Caddy e compartilham uma cota, assim como NATs. Ajustar parâmetros à população esperada sem contar por email, para evitar enumeração ou lockout direcionado. Não habilitar interpretação genérica de forwarded headers. Para distinguir clientes atrás de proxy, implementar e testar explicitamente rede de proxies confiáveis ou aplicar a limitação no gateway. Escala horizontal multiplica a cota pelo número de réplicas; este mecanismo não alega quota global ou defesa DDoS. Nenhum Redis/serviço externo foi adicionado.

## Respostas e erros

Dinheiro é string decimal de duas casas. Erros usam `{timestamp,status,error,message,path,fields}`; timestamp UTC com `Z`. JSON, enum, data e parâmetros malformados retornam 400. Duplicidades com constraints conhecidas retornam 409 sem SQL/valores; vínculos conhecidos têm mensagem distinta. Integridade desconhecida retorna 500 sanitizado. Ausência/invalidez de JWT é 401 nesse mesmo contrato. Testes de corrida forçam as duas operações a ultrapassar a pré-checagem antes de gravar.

## Capacidade de consultas

Listar contas usa uma chamada de contas e uma de saldos em lote; criar/repetir saldo isolado ainda pode usar a rota individual. A exportação preserva filtros/autorização no banco e recusa conjuntos com mais de 10000 transações, sem gerar arquivo parcial. É uma mudança de contrato de GET `/transactions`, que era ilimitado; clientes com maior volume devem usar `/transactions/page` e planejar exportação específica. A página aceita até 100 itens. Não usar paginação mutável para juntar um CSV silenciosamente inconsistente.

Envelope inicial de ensaio: 100 contas por usuário, 100000 transações distribuídas e CSV até 10000 registros. Orçamentos de engenharia propostos para validação isolada: consulta de saldos em lote <500 ms no banco e serialização CSV <64 MiB adicionais de heap; não são SLA de produção. Medições, máquinas e limitações devem acompanhar o relatório. Índices só serão adotados após comparar planos na distribuição real e avaliar escrita. O benchmark sintético não prevê latência de rede ou carga concorrente.
