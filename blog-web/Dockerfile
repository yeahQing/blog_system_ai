# 仅 Nginx 托管静态文件；请先在 blog-web 目录执行生产构建，例如:
#   set VITE_API_URL=/api && npm run build   (Windows CMD: set VITE_API_URL=/api&& npm run build)
# 生成 dist/ 后再构建镜像: docker build -t blog-app-web:latest .
# syntax=docker/dockerfile:1
FROM nginx:alpine
COPY dist/ /usr/share/nginx/html/
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80 443
