<template>
  <div class="article-detail-wrap" v-loading="loading">
    <div v-if="article" class="detail-outer">
      <div class="detail-main">
        <div class="detail-container">
          <div class="article-header">
            <el-breadcrumb separator="/" class="breadcrumb">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="article.category" :to="{ path: '/', query: { categoryId: article.category.id } }">
                {{ article.category.name }}
              </el-breadcrumb-item>
              <el-breadcrumb-item>正文</el-breadcrumb-item>
            </el-breadcrumb>

            <div class="title-row">
              <el-tag v-if="article.pinned" type="danger" effect="plain" size="small" class="pin-tag">置顶</el-tag>
              <h1 class="title">{{ article.title }}</h1>
            </div>

            <div class="meta">
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                发布 {{ formatDate(article.createdAt) }}
              </span>
              <span v-if="article.updatedAt" class="meta-item">
                <el-icon><Clock /></el-icon>
                更新 {{ formatDate(article.updatedAt) }}
              </span>
              <span class="meta-item">
                <el-icon><View /></el-icon>
                {{ article.views }} 次阅读
              </span>
              <span v-if="article.likeCount != null" class="meta-item">
                <el-icon><Pointer /></el-icon>
                {{ article.likeCount }} 赞
              </span>
              <el-tag v-if="article.category" size="small" effect="light" class="cat-tag">
                {{ article.category.name }}
              </el-tag>
            </div>

            <div v-if="article.tags?.length" class="tag-row">
              <el-tag
                v-for="t in article.tags"
                :key="t.id"
                size="small"
                effect="plain"
                class="tag-pill"
                @click="$router.push({ path: '/', query: { tagId: t.id } })"
              >
                {{ t.name }}
              </el-tag>
            </div>

            <div class="action-bar">
              <el-button :icon="Pointer" :loading="likeLoading" @click="onLike">点赞</el-button>
              <el-button :icon="Star" :type="favorited ? 'warning' : 'default'" @click="onFavorite">
                {{ favorited ? '已收藏' : '收藏' }}
              </el-button>
              <el-button :icon="Share" @click="onShare">分享</el-button>
            </div>
          </div>

          <div class="article-cover" v-if="article.cover">
            <el-image :src="article.cover" fit="cover" lazy loading="lazy" />
          </div>

          <div class="preview-wrap">
            <MdPreview
              :editor-id="previewEditorId"
              :model-value="article.content"
              :theme="mdTheme"
              :md-heading-id="portalMdHeadingId"
            />
          </div>

          <section v-if="related.length" class="related">
            <h2 class="section-title">相关阅读</h2>
            <ul>
              <li v-for="r in related" :key="r.id">
                <router-link :to="`/article/${r.id}`">{{ r.title }}</router-link>
                <span class="r-meta">{{ r.views }} 阅读</span>
              </li>
            </ul>
          </section>

          <section class="comments">
            <h2 class="section-title">评论</h2>
            <div v-for="c in commentsWithDepth" :key="c.id" class="comment-item" :style="{ marginLeft: c._depth * 16 + 'px' }">
              <div class="comment-head">
                <strong>{{ c.nickname }}</strong>
                <span class="c-time">{{ formatDateTime(c.createdAt) }}</span>
              </div>
              <p class="comment-body">{{ c.content }}</p>
              <div class="comment-actions">
                <el-button text type="primary" size="small" @click="startReply(c)">回复</el-button>
                <el-button text size="small" :loading="commentLikeId === c.id" @click="onCommentLike(c)">
                  赞 {{ c.likeCount ?? 0 }}
                </el-button>
              </div>
            </div>

            <el-card class="comment-form-card" shadow="never">
              <p v-if="replyTo" class="reply-hint">
                回复 @{{ replyTo.nickname }}
                <el-button type="primary" link @click="replyTo = null">取消</el-button>
              </p>
              <el-form label-position="top">
                <el-row :gutter="12">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="昵称" required>
                      <el-input v-model="cForm.nickname" maxlength="64" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="邮箱（选填）">
                      <el-input v-model="cForm.email" type="email" maxlength="120" />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item label="内容" required>
                  <el-input v-model="cForm.content" type="textarea" :rows="4" maxlength="5000" show-word-limit />
                </el-form-item>
                <el-button type="primary" :loading="commentSubmitting" @click="submitComment">发表评论</el-button>
              </el-form>
            </el-card>
          </section>

          <div class="article-footer">
            <el-divider />
            <div class="copyright">
              <p>本文由 <strong>本站</strong> 发布</p>
              <p>发布于：{{ new Date(article.createdAt).toLocaleString() }}</p>
            </div>
          </div>
        </div>
      </div>

      <aside class="detail-toc" aria-label="文章目录">
        <div class="toc-sticky">
          <div class="toc-head">
            <span class="toc-title">目录</span>
            <span class="toc-hint">点击跳转</span>
          </div>
          <MdCatalog
            class="article-toc-catalog"
            :editor-id="previewEditorId"
            :theme="mdTheme"
            :md-heading-id="portalMdHeadingId"
            scroll-element="html"
            :offset-top="tocViewportOffset"
            :scroll-element-offset-top="tocViewportOffset"
            :on-click="onCatalogClick"
          />
        </div>
      </aside>
    </div>

    <el-empty v-else-if="!loading" description="文章不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, inject, type Ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  getPortalArticleDetail,
  getRelatedArticles,
  likePortalArticle
} from '../api/article'
import { listArticleComments, createComment, likeComment } from '../api/comment'
import { MdPreview, MdCatalog } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { ElMessage } from 'element-plus'
import { Pointer, Star, Share, Calendar, View, Clock } from '@element-plus/icons-vue'
import { pushArticleHistory, isFavorite, toggleFavorite } from '../utils/portalStorage'

const route = useRoute()
const portalIsDark = inject<Ref<boolean>>(
  'portalIsDark',
  ref(false)
)

const article = ref<any>(null)
const loading = ref(false)
const related = ref<any[]>([])
const commentsFlat = ref<any[]>([])
const likeLoading = ref(false)
const favorited = ref(false)
const commentSubmitting = ref(false)
const commentLikeId = ref<number | null>(null)
const replyTo = ref<{ id: number; nickname: string } | null>(null)

const cForm = ref({ nickname: '', email: '', content: '' })

const articleIdParam = computed(() => {
  const raw = route.params.id
  return Array.isArray(raw) ? raw[0] : raw
})

const previewEditorId = computed(() => `portal-article-${articleIdParam.value ?? ''}`)

const mdTheme = computed(() => (portalIsDark.value ? 'dark' : 'light'))

/** 与 PortalLayout --portal-sticky-offset 一致，保证目录高亮与点击滚动对齐顶栏 */
const tocViewportOffset = 76

/** 与 md-editor-v3 默认一致，显式传给 Preview + Catalog，保证 id 与点击查找一致 */
function portalMdHeadingId(payload: { text: string }) {
  return payload.text
}

function resolveHeadingElement(id: string): HTMLElement | null {
  if (!id) return null
  const byId = document.getElementById(id)
  if (byId) return byId
  try {
    return document.querySelector(`[id="${CSS.escape(id)}"]`) as HTMLElement | null
  } catch {
    return null
  }
}

function onCatalogClick(ev: MouseEvent, item: { text: string }) {
  ev.preventDefault()
  const id = portalMdHeadingId({ text: item.text })
  const el = resolveHeadingElement(id)
  if (!el) return
  const top = el.getBoundingClientRect().top + window.scrollY - tocViewportOffset
  window.scrollTo({ top: Math.max(0, top), behavior: 'smooth' })
}

const commentsWithDepth = computed(() => {
  const flat = commentsFlat.value
  const map = new Map(flat.map((c) => [c.id, c]))
  const memo = new Map<number, number>()
  function depthOf(id: number): number {
    if (memo.has(id)) return memo.get(id)!
    const c = map.get(id)
    if (!c || c.parentId == null) {
      memo.set(id, 0)
      return 0
    }
    const v = 1 + depthOf(c.parentId)
    memo.set(id, v)
    return v
  }
  return flat.map((c) => ({ ...c, _depth: depthOf(c.id) }))
})

async function fetchDetail() {
  const id = articleIdParam.value
  if (!id) return

  article.value = null
  related.value = []
  commentsFlat.value = []
  replyTo.value = null

  loading.value = true
  try {
    const res: any = await getPortalArticleDetail(id)
    if (res.code === 200 && res.data) {
      article.value = res.data
      document.title = `${article.value.title} - AI BLOG`
      favorited.value = isFavorite(Number(id))
      pushArticleHistory(Number(id), article.value.title)

      const [rel, com]: any[] = await Promise.all([
        getRelatedArticles(id, 6),
        listArticleComments(id)
      ])
      if (rel.code === 200) related.value = rel.data || []
      if (com.code === 200) commentsFlat.value = com.data || []
    }
  } catch {
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchDetail)
watch(articleIdParam, fetchDetail)

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

const formatDateTime = (dateStr: string) => {
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function onLike() {
  if (!article.value) return
  likeLoading.value = true
  try {
    const res: any = await likePortalArticle(article.value.id)
    if (res.code === 200) {
      article.value.likeCount = (article.value.likeCount ?? 0) + 1
      ElMessage.success('感谢点赞')
    }
  } catch {
    /* */
  } finally {
    likeLoading.value = false
  }
}

function onFavorite() {
  if (!article.value) return
  const on = toggleFavorite(article.value.id, article.value.title)
  favorited.value = on
  ElMessage.success(on ? '已加入收藏（本地）' : '已取消收藏')
}

async function onShare() {
  const url = window.location.href
  const title = article.value?.title || ''
  try {
    if (navigator.share) {
      await navigator.share({ title, url })
    } else {
      await navigator.clipboard.writeText(url)
      ElMessage.success('链接已复制')
    }
  } catch {
    try {
      await navigator.clipboard.writeText(url)
      ElMessage.success('链接已复制')
    } catch {
      ElMessage.info(url)
    }
  }
}

function startReply(c: any) {
  replyTo.value = { id: c.id, nickname: c.nickname }
}

async function onCommentLike(c: any) {
  commentLikeId.value = c.id
  try {
    const res: any = await likeComment(c.id)
    if (res.code === 200) {
      c.likeCount = (c.likeCount ?? 0) + 1
    }
  } catch {
    /* */
  } finally {
    commentLikeId.value = null
  }
}

async function submitComment() {
  if (!article.value) return
  const nick = cForm.value.nickname?.trim()
  const content = cForm.value.content?.trim()
  if (!nick || !content) {
    ElMessage.warning('请填写昵称与评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    const res: any = await createComment({
      articleId: article.value.id,
      parentId: replyTo.value?.id,
      nickname: nick,
      email: cForm.value.email?.trim() || undefined,
      content
    })
    if (res.code === 200) {
      ElMessage.success('评论已发布')
      cForm.value.content = ''
      replyTo.value = null
      const com: any = await listArticleComments(article.value.id)
      if (com.code === 200) commentsFlat.value = com.data || []
    }
  } catch {
    /* */
  } finally {
    commentSubmitting.value = false
  }
}
</script>

<style scoped>
.article-detail-wrap {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px clamp(12px, 3vw, 24px) 48px;
  box-sizing: border-box;
}

.detail-outer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 248px;
  gap: 24px;
  align-items: stretch;
}

.detail-main {
  min-width: 0;
}

.detail-container {
  background-color: var(--portal-card, #fff);
  padding: clamp(20px, 4vw, 36px);
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.08);
  border: 1px solid var(--portal-border, #e2e8f0);
  box-sizing: border-box;
  overflow-x: clip;
}

.article-header {
  margin-bottom: 24px;
}

.breadcrumb {
  margin-bottom: 20px;
}

.title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.title-row .pin-tag {
  flex-shrink: 0;
}

.title {
  font-size: clamp(1.6rem, 4vw, 2.25rem);
  font-weight: 800;
  color: var(--portal-text, #1e293b);
  margin: 0;
  line-height: 1.25;
  flex: 1;
  min-width: 0;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  color: var(--portal-muted, #64748b);
  font-size: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tag-row {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag-pill {
  cursor: pointer;
}

.action-bar {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.article-cover {
  width: 100%;
  max-height: 420px;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 28px;
}

.preview-wrap {
  position: relative;
  line-height: 1.8;
  color: var(--portal-text, #334155);
  font-size: 1.05rem;
}

.preview-wrap :deep(.md-editor-preview),
.preview-wrap :deep(.md-editor-preview-wrapper) {
  max-width: 100%;
}

.preview-wrap :deep(pre),
.preview-wrap :deep(table) {
  max-width: 100%;
  overflow-x: auto;
}

.preview-wrap :deep(img) {
  max-width: 100%;
  height: auto;
}

.preview-wrap :deep(h1),
.preview-wrap :deep(h2),
.preview-wrap :deep(h3),
.preview-wrap :deep(h4),
.preview-wrap :deep(h5),
.preview-wrap :deep(h6) {
  scroll-margin-top: var(--portal-sticky-offset, 76px);
}

.related {
  margin-top: 40px;
}
.section-title {
  font-size: 1.1rem;
  margin: 0 0 12px;
  color: var(--portal-text, #1e293b);
}
.related ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.related li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--portal-border, #f1f5f9);
  font-size: 15px;
}
.related a {
  color: #3b82f6;
  text-decoration: none;
}
.related a:hover {
  text-decoration: underline;
}
.r-meta {
  font-size: 12px;
  color: #94a3b8;
  flex-shrink: 0;
}

.comments {
  margin-top: 40px;
}

.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--portal-border, #f1f5f9);
}
.comment-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 14px;
  margin-bottom: 6px;
}
.c-time {
  font-size: 12px;
  color: #94a3b8;
}
.comment-body {
  margin: 0 0 8px;
  white-space: pre-wrap;
  line-height: 1.55;
  font-size: 14px;
}
.comment-actions {
  display: flex;
  gap: 4px;
}

.comment-form-card {
  margin-top: 20px;
  border-radius: 12px;
}
.reply-hint {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--portal-muted, #64748b);
}

.article-footer {
  margin-top: 40px;
}

.copyright {
  background-color: color-mix(in srgb, var(--portal-border, #e2e8f0) 35%, transparent);
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #3b82f6;
  font-size: 14px;
  color: var(--portal-muted, #475569);
}
.copyright p {
  margin: 4px 0;
}

.detail-toc {
  position: relative;
  min-width: 0;
}

.toc-sticky {
  position: sticky;
  top: var(--portal-sticky-offset, 76px);
  padding: 14px 14px 12px;
  border-radius: 14px;
  background: linear-gradient(
    145deg,
    color-mix(in srgb, var(--portal-card, #fff) 96%, #3b82f6 4%),
    var(--portal-card, #fff)
  );
  border: 1px solid var(--portal-border, #e2e8f0);
  box-shadow: 0 1px 2px color-mix(in srgb, var(--portal-text, #0f172a) 6%, transparent);
  max-height: calc(100vh - var(--portal-sticky-offset, 76px) - 24px);
  overflow-y: auto;
  overflow-x: hidden;
  width: 100%;
  box-sizing: border-box;
}

.toc-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--portal-border, #e2e8f0);
}

.toc-title {
  font-weight: 700;
  font-size: 15px;
  letter-spacing: 0.02em;
  color: var(--portal-text, #1e293b);
}

.toc-hint {
  font-size: 11px;
  color: var(--portal-muted, #94a3b8);
  flex-shrink: 0;
}

/* md-editor-v3 目录：结构类名为 md-editor-catalog-* */
.article-toc-catalog :deep(.md-editor-catalog) {
  background: transparent;
}

.article-toc-catalog :deep(.md-editor-catalog-container) {
  position: relative;
  margin: 0;
  padding: 0 0 0 12px;
  border-left: 2px solid color-mix(in srgb, #3b82f6 35%, var(--portal-border, #e2e8f0));
}

.article-toc-catalog :deep(.md-editor-catalog-indicator) {
  left: 0;
  width: 3px;
  border-radius: 3px;
  background: linear-gradient(180deg, #3b82f6, #6366f1);
  opacity: 0.95;
}

.article-toc-catalog :deep(.md-editor-catalog-link) {
  display: block;
  padding: 7px 10px;
  margin: 2px 0;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.45;
  color: var(--portal-muted, #64748b);
  cursor: pointer;
  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.article-toc-catalog :deep(.md-editor-catalog-link:hover) {
  background: color-mix(in srgb, var(--portal-border, #e2e8f0) 55%, transparent);
  color: var(--portal-text, #1e293b);
}

.article-toc-catalog :deep(.md-editor-catalog-active) {
  color: #2563eb;
  font-weight: 600;
  background: color-mix(in srgb, #3b82f6 14%, transparent);
}

.article-toc-catalog :deep(.md-editor-catalog-wrapper) {
  margin-top: 2px;
  padding-left: 0;
}

.article-toc-catalog :deep(.md-editor-catalog-wrapper .md-editor-catalog-link) {
  font-size: 12.5px;
  padding-left: 8px;
}

@media (max-width: 960px) {
  .detail-outer {
    grid-template-columns: 1fr;
  }
  .detail-toc {
    display: none;
  }
}
</style>
