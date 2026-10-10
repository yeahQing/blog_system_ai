#!/usr/bin/env bash
# 在服务器 deploy/ 目录执行（与 docker-compose.app.yml、.env 同目录）：
#   chmod +x rebuild-and-up.sh
#   ./rebuild-and-up.sh [--use-https] [--no-cache]
#
# 适用：在服务器上用 Dockerfile **现场构建** 的场景（docker-compose.app.yml）。
# 若使用已打好的镜像（docker-compose.app.images.yml + docker load），不要用本脚本删镜像。
#
# 作用：先停掉 compose 服务并删容器，再删除业务镜像，最后重新 build 并启动。
# 镜像名与 compose 一致：blog-app-api:latest、blog-app-web:latest
#
# 参数：
#   --use-https   叠加 docker-compose.app.https.yml
#   --no-cache    先 docker compose build --no-cache，再 up（部分版本 up 不支持 --no-cache）
#   -h, --help    显示说明
set -euo pipefail

usage() {
  cat <<'EOF'
用法: ./rebuild-and-up.sh [--use-https] [--no-cache]

  --use-https   叠加 docker-compose.app.https.yml
  --no-cache    构建时不使用缓存（较慢）
  -h, --help    显示本说明
EOF
  exit "${1:-0}"
}

USE_HTTPS=0
NO_CACHE=0
while [ $# -gt 0 ]; do
  case "$1" in
    --use-https) USE_HTTPS=1 ;;
    --no-cache)  NO_CACHE=1 ;;
    -h|--help)   usage 0 ;;
    *)
      echo "未知参数: $1" >&2
      echo "用法: $0 [--use-https] [--no-cache]" >&2
      exit 1
      ;;
  esac
  shift
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -f .env ]; then
  echo "错误：当前目录缺少 .env，请先 cp .env.example .env 并填写。" >&2
  exit 1
fi

FILES=( -f docker-compose.app.yml )
if [ "$USE_HTTPS" = "1" ]; then
  FILES+=( -f docker-compose.app.https.yml )
  echo ">>> 使用 HTTPS 叠加：docker-compose.app.https.yml"
fi

echo ">>> 停止并删除本 compose 项目中的容器（不删数据卷、不删 infra）"
docker compose "${FILES[@]}" --env-file .env down

echo ">>> 删除旧业务镜像（若仍被占用会跳过该行，可先确认无其它容器引用）"
docker rmi blog-app-api:latest blog-app-web:latest 2>/dev/null || {
  echo ">>> 提示：部分镜像未删除（可能仍有容器引用或标签已不存在），继续 build …"
}

if [ "$NO_CACHE" = "1" ]; then
  echo ">>> 构建镜像（--no-cache，较慢）"
  docker compose "${FILES[@]}" --env-file .env build --no-cache
  echo ">>> 启动容器"
  docker compose "${FILES[@]}" --env-file .env up -d
else
  echo ">>> 构建镜像并后台启动容器"
  docker compose "${FILES[@]}" --env-file .env up -d --build
fi

echo ">>> 完成。查看状态：docker compose ${FILES[*]} --env-file .env ps"
