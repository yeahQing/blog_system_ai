-- PostgreSQL：修复 pinned 列（若已存在但为 NULL，或 Hibernate 曾生成无 DEFAULT 的 NOT NULL 导致失败）
-- 在 psql 或客户端中针对 blog 库执行一次即可。

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'blog_article'
    ) THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'blog_article'
          AND column_name = 'pinned'
    ) THEN
        ALTER TABLE blog_article
            ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;
    ELSE
        UPDATE blog_article SET pinned = FALSE WHERE pinned IS NULL;
        ALTER TABLE blog_article ALTER COLUMN pinned SET DEFAULT FALSE;
        ALTER TABLE blog_article ALTER COLUMN pinned SET NOT NULL;
    END IF;
END $$;
