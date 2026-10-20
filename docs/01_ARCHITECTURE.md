# Arquitetura atual

Referência: revisão-base `4f5d781e116091d42fb14d6392ec700452634191`, alterações F01–F11 de 20/10/2026 ainda sem commit. Veja [validação](validation-f01-f11.md) para distinguir implementação e execução comprovada.

## Componentes e dados

O frontend usa Next.js 16.3.8, React 19.2.8, TypeScript e Node 24. Páginas protegidas são consumidores da API; autorização efetiva ocorre no backend. O backend usa Java 21, Spring Boot 4.1.0, Spring Security, Spring Data JPA, Flyway e PostgreSQL 17. Maven resolve as versões de JUnit/Mockito pelo BOM do Spring Boot; consulte `backend/pom.xml` e a árvore de dependências para versões exatas.

A navegação cobre cadastro/login, contas, categorias, transações, dashboard, comparação mensal, orçamento e importação CSV com prévia e confirmação. Transações pertencem a contas e categorias do mesmo usuário; os serviços validam a propriedade e as consultas filtram pelo identificador autenticado. Constraints preservam unicidade, vínculos e positividade no banco. Orçamentos pertencem às categorias e são únicos por mês.

Valores financeiros trafegam como strings decimais com duas casas, persistem em NUMERIC(19,2) e são calculados em BigDecimal. No navegador, centavos usam BigInt. Agregados podem exceder a faixa de uma entrada sem perder precisão. Detalhes do contrato e compatibilidade estão no [registro de decisões](02_DECISION_LOG.md).

## Segurança e disponibilidade

O JWT HS256 stateless dura 3600 segundos. O backend valida assinatura, algoritmo, prazo e subject positivo representável por Long. O cliente armazena o token em sessionStorage, trata 401 e descarta tokens expirados para navegação. Logout encerra a cópia local; não revoga tokens emitidos. O limitador de login/cadastro trabalha no processo, por endereço da conexão e rota, com janelas configuráveis e estado limitado. Veja [segurança](security-and-contracts.md).

`GET /health/live` verifica o processo sem consultar o banco. `GET /health/ready` adquire conexão e valida o banco; retorna 503 se indisponível. As respostas públicas contêm apenas status. Migrações e validação do esquema são pré-requisitos da partida. Compose aguarda readiness; Docker não reinicia automaticamente um processo apenas por ficar unhealthy.

## Ambientes e entrega

`docker-compose.yml` fornece banco para desenvolvimento; `compose.app.yml` constrói o ambiente local integrado. `compose.demo.yml` usa imagens pré-construídas, Caddy, volumes e limites de memória. No caminho de demo, Caddy encaminha `/api` à API e as demais rotas ao frontend; as imagens locais são linux/amd64. A existência desses manifests não comprova disponibilidade em produção.

CI executa Maven verify com PostgreSQL descartável, testes Node, lint/tipos/build, smoke HTTP e Playwright com axe em ambiente Compose isolado. Relatórios são identificados pelo SHA; não se publicam dumps, ambientes, tokens, screenshots, vídeos ou traces de navegador.

Release exige checkout limpo e evidências de checks da mesma revisão. O pacote offline inclui SHA completo, identidades imutáveis das imagens, referências upstream, plataforma e checksums. Promoção/deploy exigem autorização separada. Consulte [deploy](deploy-ec2.md) e [backup](backup-and-restore.md).
