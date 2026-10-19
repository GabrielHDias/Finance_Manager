# Frontend — Finance Manager

Interface em Next.js e TypeScript para autenticação, contas, categorias, transações e dashboard por período.

Use Node.js 24, conforme `.nvmrc` na raiz do repositório. As instruções completas para banco, backend, Docker e CI estão no [README principal](../README.md).

## Desenvolvimento

Na raiz, execute `node scripts/init-env.mjs` para preparar os arquivos locais. Dentro de `frontend`:

```bash
npm ci
npm run dev
```

A interface fica em `http://localhost:3000`. A variável `NEXT_PUBLIC_API_URL` em `.env.local` aponta para a API, por padrão `http://localhost:8080`. Reinicie o servidor de desenvolvimento se alterar esse arquivo.

## Qualidade

```bash
npm run lint
npm run typecheck
npm run build
```

Ou execute todas as verificações:

```bash
npm run check
```

`typecheck` executa `next typegen` antes de `tsc`, permitindo verificar um checkout novo sem depender de arquivos gerados por um servidor de desenvolvimento anterior.

## Execução com Docker

O Dockerfile usa build em etapas e a saída standalone do Next.js. Na raiz, `docker compose -f compose.app.yml up --build --detach --wait` constrói e inicia todos os serviços.

A URL pública da API é um argumento de build da imagem, pois `NEXT_PUBLIC_API_URL` é incorporada ao JavaScript enviado ao navegador. Os arquivos `.env.local` e `node_modules` da máquina não entram no contexto da imagem.
