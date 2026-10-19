#!/usr/bin/env bash
# Build on the developer machine, never on the 2 GiB EC2.
set -euo pipefail
umask 077
root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
output=${1:?Uso: bash scripts/package-demo.sh /caminho/novo/do/pacote}
mkdir -p -- "$output"
output=$(cd -- "$output" && pwd)
if [[ -e "$output/images.tar.gz" || -e "$output/compose.demo.yml" ]]; then
  echo 'Escolha uma pasta nova para preservar os pacotes anteriores.' >&2
  exit 1
fi
revision=$(git -C "$root" rev-parse --short HEAD)
release="${revision}-$(date -u +%Y%m%d%H%M%S)"
backend="finance-manager-backend:$release"
frontend="finance-manager-frontend:$release"
docker build --platform linux/amd64 --tag "$backend" "$root/backend"
docker build --platform linux/amd64 --build-arg NEXT_PUBLIC_API_URL=/api --tag "$frontend" "$root/frontend"
docker pull --platform linux/amd64 postgres:17
docker pull --platform linux/amd64 caddy:2-alpine
docker save "$backend" "$frontend" postgres:17 caddy:2-alpine | gzip > "$output/images.tar.gz"
mkdir -p "$output/deploy"
cp "$root/compose.demo.yml" "$output/compose.demo.yml"
cp "$root/deploy/Caddyfile" "$output/deploy/Caddyfile"
cp "$root/deploy/.env.example" "$output/deploy/.env.example"
printf 'BACKEND_IMAGE=%s\nFRONTEND_IMAGE=%s\n' "$backend" "$frontend" > "$output/images.env"
printf '%s\n' "$release" > "$output/RELEASE"
(cd "$output" && sha256sum images.tar.gz compose.demo.yml deploy/Caddyfile deploy/.env.example images.env RELEASE > SHA256SUMS)
echo "Pacote criado em $output (release $release). Nenhum segredo foi incluído."
