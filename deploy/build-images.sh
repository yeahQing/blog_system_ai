#!/usr/bin/env bash
# 在仓库根目录执行：bash deploy/build-images.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo ">>> Maven package (blog-api)"
( cd blog-api && mvn -q -DskipTests package )

echo ">>> npm build (blog-web, VITE_API_URL=/api)"
( cd blog-web && VITE_API_URL=/api npm ci && VITE_API_URL=/api npm run build )

echo ">>> Docker build blog-app-api:latest"
docker build -t blog-app-api:latest -f blog-api/Dockerfile blog-api

echo ">>> Docker build blog-app-web:latest"
docker build -t blog-app-web:latest -f blog-web/Dockerfile blog-web

OUT="${ROOT}/deploy/blog-app-images.tar"
echo ">>> Saving to ${OUT}"
docker save blog-app-api:latest blog-app-web:latest -o "${OUT}"
ls -lh "${OUT}"

if [ "${KEEP_LOCAL_IMAGES:-}" = "1" ]; then
  echo ">>> KEEP_LOCAL_IMAGES=1，保留本地镜像。"
else
  echo ">>> 删除本地镜像以节省空间（tar 已生成）"
  if docker rmi blog-app-api:latest blog-app-web:latest 2>/dev/null; then
    echo ">>> 已删除 blog-app-api:latest、blog-app-web:latest"
  else
    echo ">>> 警告：未能删除镜像（常见原因：本机仍有容器在使用）。tar 已可用。可稍后执行:"
    echo "    docker rmi blog-app-api:latest blog-app-web:latest"
    echo "    或需要保留镜像时下次打包前设置: KEEP_LOCAL_IMAGES=1 bash deploy/build-images.sh"
  fi
fi

echo "Done. 服务器：先 docker compose -f docker-compose.infra.yml --env-file .env up -d，再 docker compose -f docker-compose.app.images.yml --env-file .env up -d；升级只执行第二条。"
