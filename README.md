# 博客系统（blog_system_ai）

单体仓库，包含后端 API 与前台/管理端前端。

| 目录 | 说明 |
|------|------|
| `blog-api/` | Spring Boot 3.2 + Java 21，REST API、JWT、JPA、Redis 等 |
| `blog-web/` | Vue 3 + TypeScript + Vite，Element Plus、Pinia、md-editor-v3 |

## 环境要求

- **后端**：JDK 21、Maven 3.8+；PostgreSQL、Redis（按 `application-*.yml` 配置）
- **前端**：Node.js 18+（建议 LTS）、npm 10+

## 后端 `blog-api`

```bash
cd blog-api
mvn spring-boot:run
```

默认端口与数据源等见 `blog-api/src/main/resources/application.yml` 及 `application-dev.yml` / `application-prod.yml`。敏感配置通过环境变量注入，勿将真实密钥写入仓库。

## 前端 `blog-web`

```bash
cd blog-web
npm ci   # 或 npm install
npm run dev
```

在 `blog-web/` 下可新建 `.env` 或 `.env.local`（勿提交）：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `VITE_API_URL` | 后端 API 根路径 | `http://localhost:8080/api` |

```env
VITE_API_URL=http://localhost:8080/api
```

| 命令 | 说明 |
|------|------|
| `npm run dev` | 开发服务器 |
| `npm run build` | 类型检查并构建至 `dist/` |
| `npm run preview` | 预览构建结果 |

编辑器建议安装 **Vue - Official（Volar）**；推荐扩展见 `blog-web/.vscode/extensions.json`。

## 云服务器部署（Docker Compose）

**按步骤操作请看：[deploy/DEPLOY.md](deploy/DEPLOY.md)。**

`deploy/` 下是 **两个独立 Compose 项目**，共享外部网络 `blog-network`，这样 **升级只重启前后端**，不必动 PostgreSQL / Redis。

| 文件 | Compose 项目名 | 内容 |
|------|----------------|------|
| `docker-compose.infra.yml` | `blog-infra` | PostgreSQL、Redis（长期运行） |
| `docker-compose.app.yml` | `blog-app` | 在服务器 **docker build** 业务镜像 |
| `docker-compose.app.images.yml` | `blog-app` | 使用已 **`docker load`** 的 `blog-app-api:latest`、`blog-app-web:latest` |

业务镜像与 **`blog-app-images.tar`** 内标签一致（`docker save blog-app-api:latest blog-app-web:latest`）。

数据卷：`../data/postgres`、`../data/redis`（相对 `deploy/`）。

`blog-api` 使用 **Spring Boot layertools**：`mvn package` 后生成 **`target/docker-layers/`**（四层目录），Dockerfile 分层组装；**依赖不变时**通常只同步其中的 **`application/`** 再在服务器 `docker build`（见 `deploy/DEPLOY.md` §4.3）。`blog-web` 仍复制 `dist/`。在服务器上用 `docker-compose.app.yml --build` 前须先：

```bash
( cd blog-api && mvn -DskipTests package )
( cd blog-web && VITE_API_URL=/api npm ci && VITE_API_URL=/api npm run build )
```

**首次部署（先中间件，再业务）：**

```bash
cd deploy
cp .env.example .env
# 编辑 .env

docker compose -f docker-compose.infra.yml --env-file .env up -d
docker compose -f docker-compose.app.images.yml --env-file .env up -d
# 或在服务器 build：docker compose -f docker-compose.app.yml --env-file .env up -d --build
```

### 服务器上本地 build（`docker-compose.app.yml`）

在服务器上保留完整仓库（含 `blog-api/`、`blog-web/`），**在容器里用 Dockerfile 现打镜像**，不依赖 `docker load` 的 tar。须先启动中间件：`docker compose -f docker-compose.infra.yml --env-file .env up -d`。

发版前先在本机或服务器上完成产物（与上文「`docker-compose.app.yml --build` 前」一致）：

```bash
( cd blog-api && mvn -DskipTests package )
( cd blog-web && VITE_API_URL=/api npm ci && VITE_API_URL=/api npm run build )
```

在 **`deploy/`** 目录执行（**HTTP** 仅用第一个 `-f`；**HTTPS** 再叠加 `docker-compose.app.https.yml`，证书与说明见 [deploy/DEPLOY.md](deploy/DEPLOY.md)）：

```bash
cd deploy

# 只重建并启动后端
docker compose -f docker-compose.app.yml -f docker-compose.app.https.yml --env-file .env up -d --build blog-api

# 只重建并启动前端
docker compose -f docker-compose.app.yml -f docker-compose.app.https.yml --env-file .env up -d --build blog-web

# 前后端一起更新
docker compose -f docker-compose.app.yml -f docker-compose.app.https.yml --env-file .env up -d --build
```

说明：

- 指定服务名 **`blog-api`** / **`blog-web`** 时，只 build/重建对应容器，**不会动** `docker-compose.infra.yml` 里的 PostgreSQL / Redis。
- 代码需先同步到服务器（如 `git pull`），再执行 `mvn` / `npm run build`，最后 `docker compose ... --build`。
- **仅修改 `.env`**、不改代码与镜像：去掉 `--build`，执行 `docker compose ... up -d` 即可让容器使用新环境变量。
- 若**不用 HTTPS**，去掉 `-f docker-compose.app.https.yml` 即可。

**仅升级前后端（推荐，使用预构建镜像）：** 不要对 `docker-compose.infra.yml` 再执行 `up`/`down`。只操作 **blog-app** 项目：

```bash
docker load -i blog-app-images.tar   # 若使用 tar 更新镜像
docker compose -f docker-compose.app.images.yml --env-file .env up -d
```

Compose 发现镜像变更后会重建 `blog-api`、`blog-web`；**`blog-db`、`blog-redis` 不受影响**。

**常用命令：**

| 命令 | 说明 |
|------|------|
| `docker compose -f docker-compose.app.images.yml logs -f blog-api` | 只看业务 API 日志 |
| `docker compose -f docker-compose.app.images.yml down` | 只停 **blog-api、blog-web**（网络 `blog-network` 保留，中间件继续跑） |
| `docker compose -f docker-compose.infra.yml down` | 停数据库与 Redis（**升级业务时不要执行**；停前建议先 `docker compose -f docker-compose.app.images.yml down`） |

首次空库可在 `.env` 保留 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`；稳定后改为 `validate`。

仅在本机起数据库时，仍可用仓库根目录的 `docker-compose.local.yml`。

### 本地构建镜像，导出 tar 部署（不用镜像仓库）

**1. 本机（仓库根目录）**

```bash
bash deploy/build-images.sh
```

打包成功后会默认 `docker rmi` 删掉本机 `blog-app-api:latest`、`blog-app-web:latest` 以省空间；保留镜像用 `KEEP_LOCAL_IMAGES=1 bash deploy/build-images.sh`。

或手动：

```bash
docker build -t blog-app-api:latest -f blog-api/Dockerfile blog-api
docker build -t blog-app-web:latest -f blog-web/Dockerfile blog-web
docker save blog-app-api:latest blog-app-web:latest -o deploy/blog-app-images.tar
```

**2. 传到服务器**：`blog-app-images.tar`、`docker-compose.infra.yml`、`docker-compose.app.images.yml`、`.env`。

**3. 服务器示例目录**

```text
/opt/blog-system/
  data/
  deploy/
    .env
    docker-compose.infra.yml
    docker-compose.app.images.yml
    blog-app-images.tar
```

**4. 首次**

```bash
cd /opt/blog-system/deploy
docker load -i blog-app-images.tar
docker compose -f docker-compose.infra.yml --env-file .env up -d
docker compose -f docker-compose.app.images.yml --env-file .env up -d
```

**5. 后续只发版前后端**

```bash
docker load -i blog-app-images.tar
docker compose -f docker-compose.app.images.yml --env-file .env up -d
```

首次中间件仍会 **从 Docker Hub 拉取** `postgres:15-alpine`、`redis:7-alpine`。完全离线时在本机：

```bash
docker pull postgres:15-alpine
docker pull redis:7-alpine
docker save postgres:15-alpine redis:7-alpine blog-app-api:latest blog-app-web:latest -o deploy/full-stack-images.tar
```

## 版本控制说明

仓库根目录 `.gitignore` 跟踪 `blog-api/`、`blog-web/`、`deploy/`、根目录 `README.md` 与 `.gitignore`；其他根目录文件默认不纳入 Git。
