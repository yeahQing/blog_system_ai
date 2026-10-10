# 博客系统云服务器部署指南

本文说明如何将 **blog-api**（Spring Boot）与 **blog-web**（Vue 静态 + Nginx）部署到 Linux 云服务器。编排采用 **两个独立的 Docker Compose 项目**：

| 项目 | 文件 | 作用 |
|------|------|------|
| `blog-infra` | `docker-compose.infra.yml` | PostgreSQL、Redis，**长期运行** |
| `blog-app` | `docker-compose.app.images.yml` | 前后端容器，**发版时只更新本项目** |

业务镜像标签固定为 **`blog-app-api:latest`**、**`blog-app-web:latest`**，与 `blog-app-images.tar` 内容一致。

---

## 一、服务器与环境要求

### 1.1 系统与软件

- 推荐 **Ubuntu 22.04 LTS** 或同类 Linux（x86_64）。
- 安装 **Docker Engine** 与 **Docker Compose 插件**（`docker compose version` 能执行即可）。
- 若采用 **方式 B（在服务器上 build 镜像）**，还需 **JDK 21、Maven 3.8+、Node.js 18+**。

### 1.2 网络与安全组

- 放行 **`HTTP_PORT`**（默认 **80**）入站；若改用 `8080` 等，在云厂商安全组中同步放行。
- 服务器需能访问 **Docker Hub**（拉取 `postgres:15-alpine`、`redis:7-alpine`），除非使用下文「完全离线」打包方式。

### 1.3 目录约定（重要）

所有命令默认在 **`deploy/`** 目录执行；数据卷使用相对路径 **`../data`**，因此请在服务器上保持与仓库相同的父子关系：

```text
/opt/blog-system/          # 示例根目录，可自定
├── data/                  # 首次可为空；Postgres/Redis 数据落盘于此
│   ├── postgres/
│   └── redis/
└── deploy/
    ├── .env
    ├── docker-compose.infra.yml
    ├── docker-compose.app.images.yml   # 或仅用 app.yml（服务器 build）
    └── blog-app-images.tar             # 方式 A 需要
```

将本仓库的 **`deploy/`** 整夹复制到服务器对应位置，并确保 **`data` 与 `deploy` 同级**。

---

## 二、配置环境变量

在服务器 `deploy/` 下：

```bash
cp .env.example .env
nano .env   # 或用其他编辑器
```

**必改项：**

- `DB_PASSWORD`：PostgreSQL 密码（强密码）。
- `REDIS_PASSWORD`：Redis `requirepass`（强密码）。
- `JWT_SECRET`：至少 32 字符的随机串。

**按需填写：**

- `COS_*`：使用腾讯云 COS 上传时与后端 `application.yml` 一致；不用可留空（视你后端逻辑是否强依赖）。

**数据库表结构：**

- 首次空库建议保留 `SPRING_JPA_HIBERNATE_DDL_AUTO=update` 自动建表。
- 结构稳定后改为 `validate`，并自行规划迁移方式。

**端口：**

- `HTTP_PORT=80`：Nginx 映射到宿主机端口；若 80 被占用可改为 `8080` 等，访问 `http://IP:端口`。

---

## 三、方式 A：本机构建镜像 + tar 上传（推荐）

不在服务器上保留前后端源码；适合与你当前 `deploy/build-images.sh` 流程一致。

### 3.1 在开发机（有完整仓库）

需已安装 Docker、JDK 21、Maven、Node.js。

在**仓库根目录**执行：

```bash
bash deploy/build-images.sh
```

脚本会依次：`mvn package` → `npm ci` + `npm run build`（`VITE_API_URL=/api`）→ `docker build` 两个镜像 → 生成 **`deploy/blog-app-images.tar`** → **默认再删除本机这两个镜像**（`docker rmi`），节省磁盘；若本机仍有容器占用对应镜像，删除会失败并提示，**不影响已生成的 tar**。

需要保留本地镜像以便本机调试时，执行：

```bash
KEEP_LOCAL_IMAGES=1 bash deploy/build-images.sh
```

手动等价命令见仓库根目录 `README.md`。

### 3.2 上传到服务器

将以下文件拷到服务器的 `deploy/`（例如 `scp` / SFTP）：

- `blog-app-images.tar`
- `docker-compose.infra.yml`
- `docker-compose.app.images.yml`
- `.env`（不要提交 Git）

### 3.3 在服务器首次启动

```bash
cd /opt/blog-system/deploy   # 按你的实际路径

docker load -i blog-app-images.tar

docker compose -f docker-compose.infra.yml --env-file .env up -d
docker compose -f docker-compose.app.images.yml --env-file .env up -d
```

说明：

1. 第一条会创建网络 **`blog-network`** 并启动 **blog-db、blog-redis**（若可访问公网会拉镜像）。
2. 第二条启动 **blog-api、blog-web**，加入同一网络，通过主机名 **`blog-db`、`blog-redis`** 连接中间件。

浏览器访问：`http://服务器公网IP`（或 `http://IP:HTTP_PORT`）。

### 3.4 后续仅升级前后端（不重启数据库）

```bash
cd /opt/blog-system/deploy
docker load -i blog-app-images.tar
docker compose -f docker-compose.app.images.yml --env-file .env up -d
```

**不要**对 `docker-compose.infra.yml` 再执行 `up/down`，除非你要维护或迁移数据库。

查看业务日志：

```bash
docker compose -f docker-compose.app.images.yml logs -f blog-api
```

仅停止前后端（库仍运行）：

```bash
docker compose -f docker-compose.app.images.yml down
```

---

## 四、方式 B：在服务器上 build 业务镜像

适用于把**完整仓库**克隆到服务器、在机器上编译 jar 与 `dist` 再打镜像。

### 4.1 准备产物

在仓库根目录：

```bash
( cd blog-api && mvn -DskipTests package )
( cd blog-web && VITE_API_URL=/api npm ci && VITE_API_URL=/api npm run build )
```

### 4.2 启动

仍在 **`deploy/`** 且存在 `.env`：

```bash
docker compose -f docker-compose.infra.yml --env-file .env up -d
docker compose -f docker-compose.app.yml --env-file .env up -d --build
```

升级业务（改代码后重新 build）：

```bash
docker compose -f docker-compose.app.yml --env-file .env up -d --build
```

同样**不要**为发版去 `down` infra。

### 4.3 后端 Layertools 分层：部署与更新怎么操作

Spring Boot 打出来的 **`blog-api.jar` 是带分层信息的 fat jar**。`mvn package` 末尾会用 **`java -Djarmode=layertools -jar blog-api.jar extract`** 自动解压到 **`blog-api/target/docker-layers/`**，下面四个目录就是 Docker 里要 `COPY` 的内容：

| 目录 | 含义 | 体积 | 日常更新 |
|------|------|------|----------|
| `dependencies/` | 第三方依赖 jar | **大** | 不改 `pom` 依赖时 **不用重传** |
| `spring-boot-loader/` | Spring Boot 启动器 | 小 | 一般 **不用动** |
| `snapshot-dependencies/` | 其它 SNAPSHOT 依赖 | 通常很小 | 有 SNAPSHOT 依赖变更时再传 |
| `application/` | 你的业务代码（`BOOT-INF/classes` 等） | **小** | **最常替换的就是它** |

镜像里用 **`JarLauncher`** 启动，和 `java -jar fat.jar` 行为一致，只是文件被拆成四层放在磁盘上。

---

#### 你需要记住的两句话

1. **首次 / 依赖有变动**：保证服务器上有 **完整的 `target/docker-layers/`**（四个子目录都在）。  
2. **只改了 Java 业务代码、没动 `pom` 依赖**：通常只要上传 **`application/`** 这一层，再 `docker build` 即可。

---

#### 首次部署后端镜像（在服务器上 `docker build`）

**做法 A：服务器能跑 Maven（推荐省事）**

1. 把 **`blog-api/` 源码**（含 `pom.xml`）放到服务器，例如 `/opt/blog-system/blog-api/`。  
2. 在服务器执行：

   ```bash
   cd /opt/blog-system/blog-api
   mvn -DskipTests package
   ```

   完成后应存在：`target/docker-layers/dependencies/`、`spring-boot-loader/`、`snapshot-dependencies/`、`application/`。

3. 在 **`deploy/`** 执行（与文档 4.2 一致）：

   ```bash
   cd /opt/blog-system/deploy
   docker compose -f docker-compose.infra.yml --env-file .env up -d
   docker compose -f docker-compose.app.yml --env-file .env up -d --build
   ```

**做法 B：本机打包，上传目录**

1. **本机**仓库根目录：

   ```bash
   cd blog-api && mvn -DskipTests package
   ```

2. 把 **`blog-api/` 下的 `Dockerfile`** 以及 **`blog-api/target/docker-layers/` 整个文件夹**（四个子目录）拷到服务器，保持路径类似：

   ```text
   /opt/blog-system/blog-api/Dockerfile
   /opt/blog-system/blog-api/target/docker-layers/dependencies/...
   /opt/blog-system/blog-api/target/docker-layers/application/...
   ```

3. 同样执行上面的 **`docker compose ... up -d --build`**。

---

#### 日常更新（依赖没变，只改代码）

1. **本机**：

   ```bash
   cd blog-api && mvn -DskipTests package
   ```

2. **只同步 `application/`** 到服务器（覆盖原目录），例如：

   ```bash
   rsync -avz --delete blog-api/target/docker-layers/application/ \
     user@你的服务器:/opt/blog-system/blog-api/target/docker-layers/application/
   ```

   没有 `rsync` 时用 `scp` 打包该目录再解压到同一路径亦可。

3. **服务器**只重建 API 镜像并启动（**不要**动 `docker-compose.infra.yml`）：

   ```bash
   cd /opt/blog-system/deploy
   docker compose -f docker-compose.app.yml --env-file .env build blog-api
   docker compose -f docker-compose.app.yml --env-file .env up -d
   ```

Docker 会复用前几层缓存，构建快、上传量主要是 **`application/`**。

---

#### 什么时候不能只传 `application/`？

在 **`pom.xml` 里增删依赖、升级版本** 之后：必须重新 `mvn package`，并至少把新的 **`dependencies/`** 同步到服务器（**最省事是整份重新上传 `docker-layers/`** 或在服务器再跑一次 `mvn package`）。

---

#### 和 `blog-app-images.tar` 的关系

- 走 **`build-images.sh` 打 tar`**：本机 `mvn package` 已会生成 `docker-layers/`，`docker build` 用当前 **Dockerfile**，**逻辑与上面一致**；每次 tar 仍是**整镜像**，省流量主要体现在 **服务器 build + 只传 `application/`** 这条路径。

---

#### 本机自检（可选）

```bash
cd blog-api
mvn -q -DskipTests package
ls target/docker-layers
```

应看到四个目录；任缺则不要 `docker build`，先检查 Maven 是否报错。

---

## 附：HTTPS 访问（域名 yeahqing.cn、`yeahqing.cn_nginx.zip` 等证书包）

思路：在 **Nginx 容器内终止 TLS**（443），HTTP 80 只做跳转 HTTPS；证书用**卷挂载**，不打进镜像。

### 1. 腾讯云 Nginx 证书包（与官方文档一致）

参考：[Nginx 服务器 SSL 证书安装部署（Linux）](https://cloud.tencent.com/document/product/400/35244)。

在控制台下载 **服务器类型：Nginx**，解压后通常包含 `域名_bundle.crt`、`域名.key` 等。本项目约定放在仓库 **`deploy/yeahqing.cn_nginx/`**（与 `docker-compose.app.https.yml` 挂载一致），例如：

| 文件名 | 说明 |
|--------|------|
| `yeahqing.cn_bundle.crt` | 证书（含链），对应 Nginx 的 `ssl_certificate` |
| `yeahqing.cn.key` | 私钥，对应 `ssl_certificate_key` |

`yeahqing.cn_bundle.pem`、`.csr` 可保留在同一目录；Nginx 配置只用到上面两个。**勿将 `.key` / `.crt` 提交 Git**（根目录 `.gitignore` 已忽略）。

服务器上路径示例：`/opt/blog-system/deploy/yeahqing.cn_nginx/`。权限建议：`chmod 600 yeahqing.cn.key`

详情见 **`deploy/yeahqing.cn_nginx/README.md`**。

### 2. 改域名（若与示例不一致）

编辑 **`blog-web/nginx/default-https.conf`** 里两处 `server_name yeahqing.cn www.yeahqing.cn;` 为你的主域名。

### 3. 云安全组与 DNS

- 安全组放行 **TCP 443**（以及 **80**，用于跳转）。
- 域名 **A 记录** 指向服务器公网 IP。

### 4. 启动时合并 HTTPS 编排

在 **`deploy/`** 下，**在原有 app 编排基础上多加一个 `-f`**：

```bash
docker compose -f docker-compose.infra.yml --env-file .env up -d

docker compose -f docker-compose.app.yml -f docker-compose.app.https.yml --env-file .env up -d --build
```

`docker-compose.app.https.yml` 会：

- 映射 **`HTTPS_PORT`（默认 443）** 与 **`HTTP_PORT`（80）**；
- 把 **`deploy/yeahqing.cn_nginx`** 挂到容器内 `/etc/nginx/certs`（腾讯云 `*_bundle.crt` + `*.key`）；
- 用 **`blog-web/nginx/default-https.conf`** 覆盖站点配置：**HTTP 80 统一 `301` 跳转到 HTTPS**。

`.env` 可增加（可选）：`HTTPS_PORT=443`。

### 5. 前端与 API

构建前端时已使用 **`VITE_API_URL=/api`** 时，浏览器走 **https://域名/api/...**，与当前 Nginx 反代一致，**一般无需改前端**。

### 6. 证书续期

到期前更新服务器上 **`deploy/yeahqing.cn_nginx/`** 内证书与私钥，然后：

```bash
docker compose -f docker-compose.app.yml -f docker-compose.app.https.yml exec blog-web nginx -s reload
```

或 `docker compose ... restart blog-web`。

---

## 五、完全离线（可选）

若服务器不能访问 Docker Hub，在**能联网的机器**上：

```bash
docker pull postgres:15-alpine
docker pull redis:7-alpine
# 已本地 build 好业务镜像后：
docker save postgres:15-alpine redis:7-alpine blog-app-api:latest blog-app-web:latest -o full-stack-images.tar
```

将 `full-stack-images.tar` 拷到服务器后：

```bash
docker load -i full-stack-images.tar
```

再按 **3.3** 启动 infra 与 app（infra 的 `pull` 会因本地已有镜像而不再联网）。

---

## 六、常见问题

### 6.1 `network blog-network declared as external but could not be found`

必须先执行：

```bash
docker compose -f docker-compose.infra.yml --env-file .env up -d
```

再启动 `docker-compose.app.images.yml`。

### 6.2 API 启动报连不上数据库

确认 infra 已启动、`blog-db` 容器在跑；`blog-api` 配置了 `restart: unless-stopped`，可稍等自动重试或手动：

```bash
docker compose -f docker-compose.app.images.yml restart blog-api
```

### 6.3 前端 502 或 `/api` 不通

- 确认 `blog-api` 健康、同一 `blog-network` 内 Nginx 能解析 `blog-api`。
- 生产前端构建必须使用 **`VITE_API_URL=/api`**，与 Nginx 反代路径一致。

### 6.4 修改了数据库密码或 Redis 密码

需同步修改 `.env`，并**重建业务容器**（infra 中 Postgres/Redis 首次初始化后改密码会更复杂，生产环境建议在首次部署前定好密码）：

```bash
docker compose -f docker-compose.app.images.yml --env-file .env up -d
```

### 6.5 `docker build` 报错找不到 `target/docker-layers/...`

先在 **`blog-api`** 目录执行 **`mvn -DskipTests package`**，确认生成 `target/docker-layers/` 下四个子目录后再构建镜像。

---

## 七、文件对照表

| 文件 | 用途 |
|------|------|
| `docker-compose.infra.yml` | Postgres + Redis |
| `docker-compose.app.images.yml` | 使用已 load 的 `blog-app-*` 镜像 |
| `docker-compose.app.yml` | 在服务器 build 业务镜像 |
| `docker-compose.app.https.yml` | 与上一文件叠加：443 + 挂载证书、`default-https.conf` |
| `blog-web/nginx/default-https.conf` | HTTPS + 80 跳转 |
| `blog-api/Dockerfile` | 按 `target/docker-layers/*` 四层 `COPY`，`JarLauncher` 启动 |
| `blog-api/target/docker-layers/` | `mvn package` 自动生成；增量可只替换其中 `application/` |
| `.env.example` | 环境变量模板 |
| `build-images.sh` | 本机一键打 tar（仓库根目录执行） |

更简略说明见仓库根目录 **`README.md`**。
