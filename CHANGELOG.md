# 更新日志

所有显著变更将记录在本文件中。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [0.0.1] - 进行中

### 新增

**博客核心**

- 文章管理：发布 / 草稿 / 置顶，Markdown 编辑（md-editor-v3），分类与标签体系
- 前台门户：首页、文章详情、博主主页、留言板、常用工具（友链）、"我的空间"
- 管理后台：数据概览、站点设置、站点品牌（Site Branding）、菜单可见性管理
- 评论与留言板、友链申请与管理
- JWT 认证 + Spring Security 路径级放行配置

**AI 能力**

- AI 写作辅助：封面生成、摘要生成、标题建议（MiniMax）
- 创作智能体（Agent）：管理后台对话式自主写作，SSE 流式输出思考过程，8 个文章工具（语义搜索 / 读取 / 写入 / 保存 / 大纲 / 摘要 / 标题 / 封面），见 `docs/AGENT_DESIGN.md`
- AI 平台化：模型、提示词、消息流、向量库、记忆存储全部数据库驱动，后台可视化管理（`/admin/platform/*`）
- RAG 检索增强：pgvector 向量存储 + Ollama bge-m3（1024 维）Embedding + Markdown 分块 + 混合检索（预留重排序）

**部署与运维**

- Docker 分层镜像（layertools）与增量构建脚本
- 双 Compose 项目拆分：`blog-infra`（PostgreSQL + Redis 常驻）/ `blog-app`（业务，可独立升级）
- HTTPS 部署编排（证书卷挂载）、预构建镜像发版流程（`docker load` + `build-images.sh`）
- 公安备案与 ICP 备案页脚展示

### 变更

- 本地开发端口调整为 8081（避开 8080 占用），`VITE_API_URL` 同步
- MiniMax 对话 max-tokens 由 1024 调整为 8192
