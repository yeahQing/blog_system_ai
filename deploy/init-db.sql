-- 数据库扩展初始化脚本
-- 此脚本挂载到 /docker-entrypoint-initdb.d/，仅在新数据库首次创建时执行
-- 如果数据库已存在（data 目录有数据），此脚本不会被执行

-- pgvector 扩展：用于向量存储与相似度搜索（配合 Ollama bge-small-zh embedding）
CREATE EXTENSION IF NOT EXISTS vector;

-- postgis 扩展：用于地理空间数据查询
-- postgis/postgis 镜像会自动创建此扩展，此处作为兜底
CREATE EXTENSION IF NOT EXISTS postgis;
