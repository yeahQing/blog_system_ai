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

## 版本控制说明

仓库根目录 `.gitignore` 仅跟踪 `blog-api/`、`blog-web/`、根目录 `README.md` 与 `.gitignore` 本身；根目录其他文件（如本地 `.env`、`docker-compose` 等）默认不纳入 Git。若需跟踪更多根目录文件，请在根 `.gitignore` 中增加对应的 `!文件名` 规则。
