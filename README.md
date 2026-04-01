# 博客系统（blog_system_ai）

单体仓库：Spring Boot API + Vue 3 前台与管理端。

| 目录 | 说明 |
|------|------|
| `blog-api/` | Spring Boot 3.3 + Java 21，REST、JWT、JPA、Redis、Spring AI（MiniMax 等，可选） |
| `blog-web/` | Vue 3 + TypeScript + Vite，Element Plus、Pinia、md-editor-v3 |
| `deploy/` | 生产 Docker Compose、脚本与部署说明 |

**版本记录**：根目录 [`VERSION`](./VERSION) 与 [`CHANGELOG.md`](./CHANGELOG.md)。

---

## 环境要求

- **后端**：JDK 21、Maven 3.8+；PostgreSQL、Redis（配置见 `application-*.yml`）
- **前端**：Node.js 18+（建议 LTS）、npm 10+

---

## 本地开发

### 后端 `blog-api`

```bash
cd blog-api
mvn spring-boot:run
```

（Windows PowerShell 若控制台中文日志乱码，可先执行 `chcp 65001` 再运行 Maven。）

默认端口、数据源、日志路径等见 `blog-api/src/main/resources/application.yml` 及 `application-dev.yml` / `application-prod.yml`。

### 前端 `blog-web`

```bash
cd blog-web
npm ci   # 或 npm install
npm run dev
```

在 `blog-web/` 下可新建 `.env` 或 `.env.local`（勿提交）：

| 变量 | 说明 | 本地默认 |
|------|------|----------|
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

---

## 配置与密钥（重要）

- `application.yml` 中的 `${MINIMAX_API_KEY}`、`${JWT_SECRET}` 等表示读取 **进程环境变量**，**不会**自动读取仓库里的 `.env` 文件。
- **本地**：在终端 `export`、IDE 运行配置，或自行加载 env。
- **Docker**：在 **`deploy/.env`**（与 Compose 文件同目录）填写变量；执行时使用 **`--env-file .env`**，Compose 会把 `${...}` 展开并写入各服务的 `environment`。  
  说明见 [`deploy/.env.example`](deploy/.env.example) 顶部注释。
- **生产** `SPRING_AI_MODEL_CHAT` 默认 `none`（见 `application-prod.yml`）；启用 MiniMax 对话需在环境中同时设置 `SPRING_AI_MODEL_CHAT=minimax` 与 `MINIMAX_API_KEY`。

---

## 云服务器部署（Docker Compose）

**完整步骤与 HTTPS 证书见：[deploy/DEPLOY.md](deploy/DEPLOY.md)。**

`deploy/` 下为 **两个独立 Compose 项目**，共享外部网络 `blog-network`，**升级业务可只重启前后端**，不必动 PostgreSQL / Redis。

| 文件 | Compose 项目名 | 用途 |
|------|----------------|------|
| `docker-compose.infra.yml` | `blog-infra` | PostgreSQL、Redis（长期运行） |
| `docker-compose.app.yml` | `blog-app` | 在服务器 **docker build** 业务镜像 |
| `docker-compose.app.images.yml` | `blog-app` | 使用已 **`docker load`** 的 `blog-app-api:latest`、`blog-app-web:latest` |
| `docker-compose.app.https.yml` | （叠加） | 与 `app.yml` 组合使用，开启 443 与证书卷 |

业务镜像标签与 `blog-app-images.tar` 内一致。数据卷：`../data/`（相对 `deploy/`）。

### 一键删旧镜像并重建（服务器现场 build）

在 **`deploy/`** 目录、已存在 **`.env`** 时：

```bash
chmod +x rebuild-and-up.sh
./rebuild-and-up.sh              # HTTP
./rebuild-and-up.sh --use-https   # 叠加 HTTPS
./rebuild-and-up.sh --use-https --no-cache   # 无缓存构建（先 build 再 up，兼容各版本 compose）
```

脚本顺序：`compose down` → 删除 `blog-app-api:latest` / `blog-app-web:latest` → `build`（可选 `--no-cache`）→ `up -d`。  
**不适用**「仅使用预构建镜像 + `docker load`」的流程（勿删 load 进来的标签）。

### 首次部署（先中间件，再业务）

```bash
cd deploy
cp .env.example .env
# 编辑 .env

docker compose -f docker-compose.infra.yml --env-file .env up -d
docker compose -f docker-compose.app.images.yml --env-file .env up -d
# 或在服务器 build：
# docker compose -f docker-compose.app.yml --env-file .env up -d --build
```

### 服务器上 `docker-compose.app.yml` + HTTPS

代码与产物需已在服务器就绪（`git pull` 后 `mvn package`、`npm run build` 等见 [DEPLOY.md](deploy/DEPLOY.md)）。在 **`deploy/`** 下：

```bash
# 前后端一起更新（HTTP + HTTPS 叠加）
docker compose -f docker-compose.app.yml -f docker-compose.app.https.yml --env-file .env up -d --build
```

- 只更新单个服务可加服务名，例如：`... up -d --build blog-api`。
- **仅改 `.env`**、不改镜像：`up -d`（无 `--build`）。
- **不用 HTTPS** 时去掉 `-f docker-compose.app.https.yml`。

### 使用预构建镜像发版（推荐）

不要对 `docker-compose.infra.yml` 反复 `down`（数据库/redis 常驻）。只更新 **blog-app**：

```bash
docker load -i blog-app-images.tar   # 若用 tar 更新镜像
docker compose -f docker-compose.app.images.yml --env-file .env up -d
```

### 本机构建镜像并打 tar（无镜像仓库）

```bash
bash deploy/build-images.sh
```

成功后会生成 `deploy/blog-app-images.tar`；默认会删除本机 `blog-app-api:latest`、`blog-app-web:latest` 以省空间，保留镜像可：`KEEP_LOCAL_IMAGES=1 bash deploy/build-images.sh`。

手动构建与 `docker save` 示例见上文或 [DEPLOY.md](deploy/DEPLOY.md)。

### 常用命令

| 命令 | 说明 |
|------|------|
| `docker compose -f docker-compose.app.images.yml logs -f blog-api` | 跟踪 API 日志 |
| `docker compose -f docker-compose.app.images.yml down` | 仅停 blog-api、blog-web（中间件继续） |
| `docker compose -f docker-compose.infra.yml down` | 停数据库与 Redis（**业务升级时不要执行**） |

首次空库可在 `.env` 保留 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`；稳定后改为 `validate`。

### 本机仅起数据库 / Redis

可用仓库根目录 [`docker-compose.local.yml`](docker-compose.local.yml)（与 `deploy/` 下 infra 编排不同，仅供本地联调）。

---

## 版本控制说明

根目录 [`.gitignore`](.gitignore) 约定：跟踪 `blog-api/`、`blog-web/`、`deploy/`、`README.md`、`.gitignore` 等；其他根目录文件默认不纳入 Git。密钥勿提交；`.env` 已忽略。

---

## AI 与 Cursor 规则（可选）

仓库内 [`.cursor/rules/`](.cursor/rules/) 与 [`.cursor/skills/`](.cursor/skills/) 提供前后端与部署的约定及可复用技能，详见 [`.cursor/README.md`](.cursor/README.md)。
