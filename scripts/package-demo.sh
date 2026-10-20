#!/usr/bin/env bash
# Build locally; deployment/promotion requires separate authorization.
set -euo pipefail
umask 077
root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
output=${1:?Uso: RELEASE_CHECKS_FILE=/checks.json bash scripts/package-demo.sh /pasta/nova}
if [[ -n "$(git -C "$root" status --porcelain --untracked-files=all)" ]]; then
  echo 'Release exige checkout limpo, incluindo arquivos não rastreados.' >&2
  exit 1
fi
revision=$(git -C "$root" rev-parse HEAD)
checks=${RELEASE_CHECKS_FILE:?Informe evidência sanitizada dos checks da revisão}
python3 - "$checks" "$revision" <<'PY'
import json,sys
checks=json.load(open(sys.argv[1]))
if (checks.get('revision') != sys.argv[2] or checks.get('approved') is not True
    or not isinstance(checks.get('commands'), list) or not checks['commands']
    or not all(isinstance(command, str) and command.strip() for command in checks['commands'])):
    raise SystemExit('Checks ausentes ou de outra revisão')
PY
# Refuse any existing destination; never overwrite the preceding package.
mkdir -- "$output"
output=$(cd -- "$output" && pwd)
sources=$(mktemp -d)
trap 'rm -rf -- "$sources"' EXIT
# Build the immutable commit archive, excluding ignored files and concurrent edits.
git -C "$root" archive "$revision" | tar -x -C "$sources"
release="${revision}-$(date -u +%Y%m%d%H%M%S)"
backend="finance-manager-backend:$release"
frontend="finance-manager-frontend:$release"
postgres=postgres:17@sha256:2d2b8998d31037bf721cfdf764d76ba74171b4fab3431b7f72c27c56ddbdf9e3
caddy=caddy:2-alpine@sha256:d8542f48d34a9cf4e4c11a478865229840e87e4c96ea3f439101f31a5d35f75f
docker build --platform linux/amd64 --label "org.opencontainers.image.revision=$revision" --tag "$backend" "$sources/backend"
docker build --platform linux/amd64 --label "org.opencontainers.image.revision=$revision" --build-arg NEXT_PUBLIC_API_URL=/api --tag "$frontend" "$sources/frontend"
docker pull --platform linux/amd64 "$postgres"
docker pull --platform linux/amd64 "$caddy"
postgres_offline="finance-manager-postgres:$release"
caddy_offline="finance-manager-caddy:$release"
docker tag "$postgres" "$postgres_offline"
docker tag "$caddy" "$caddy_offline"
docker save "$backend" "$frontend" "$postgres_offline" "$caddy_offline" | gzip > "$output/images.tar.gz"
mkdir "$output/deploy"
cp "$sources/compose.demo.yml" "$output/compose.demo.yml"
cp "$sources/deploy/Caddyfile" "$output/deploy/Caddyfile"
cp "$sources/deploy/.env.example" "$output/deploy/.env.example"
printf 'BACKEND_IMAGE=%s\nFRONTEND_IMAGE=%s\nPOSTGRES_IMAGE=%s\nCADDY_IMAGE=%s\n' "$backend" "$frontend" "$postgres_offline" "$caddy_offline" > "$output/images.env"
printf '%s\n' "$release" > "$output/RELEASE"
# Query only public image metadata, never container environments/configuration.
docker image inspect --format '{{json .Id}} {{json .RepoDigests}}' "$backend" "$frontend" "$postgres_offline" "$caddy_offline" > "$output/IMAGE_IDENTITIES"
python3 - "$output" "$revision" "$checks" "$backend" "$frontend" "$postgres_offline" "$caddy_offline" "$postgres" "$caddy" <<'PY'
import json,sys
from pathlib import Path
output=Path(sys.argv[1])
checks=json.load(open(sys.argv[3]))
# Copy only the explicitly supported public check fields.
manifest={'revision':sys.argv[2], 'platform':'linux/amd64', 'images':sys.argv[4:8], 'upstream_images':sys.argv[8:],
          'identities':(output/'IMAGE_IDENTITIES').read_text().splitlines(),
          'checks':{'approved':True,'commands':checks['commands']},
          'source_materialization':'git archive '+sys.argv[2],
          'build_commands':['docker build --platform linux/amd64 --label org.opencontainers.image.revision='+sys.argv[2]+' --tag '+sys.argv[4]+' backend',
                            'docker build --platform linux/amd64 --label org.opencontainers.image.revision='+sys.argv[2]+' --build-arg NEXT_PUBLIC_API_URL=/api --tag '+sys.argv[5]+' frontend']}
(output/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
PY
(cd "$output" && sha256sum images.tar.gz compose.demo.yml deploy/Caddyfile deploy/.env.example images.env RELEASE IMAGE_IDENTITIES manifest.json > SHA256SUMS && sha256sum --check --status SHA256SUMS)
echo "Pacote criado em $output (release $release). Revise o manifesto antes da promoção."
