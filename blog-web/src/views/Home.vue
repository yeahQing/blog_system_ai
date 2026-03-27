<template>
  <div class="home-container">
    <div class="welcome-banner">
      <h1 class="hero-title">{{ portalSite.heroTitle }}</h1>
      <p class="hero-subtitle">{{ portalSite.heroSubtitle }}</p>
      <div class="search-row">
        <el-input
          v-model="keywordInput"
          placeholder="搜索标题或正文…"
          clearable
          class="search-input"
          @keyup.enter="applySearch"
        >
          <template #append>
            <el-button type="primary" @click="applySearch">搜索</el-button>
          </template>
        </el-input>
      </div>
      <div v-if="activeFilterHint" class="filter-hint">
        <span>{{ activeFilterHint }}</span>
        <el-button type="primary" link @click="clearFilters">清除筛选</el-button>
      </div>
    </div>

    <div class="content-wrapper">
      <div
        :key="listKey"
        class="article-list"
        v-infinite-scroll="loadMore"
        :infinite-scroll-disabled="disabled"
      >
        <div v-for="article in articles" :key="article.id" class="article-item" @click="goToDetail(article.id)">
          <el-card shadow="hover" class="article-card">
            <div class="card-body">
              <div class="article-content">
                <div class="article-header">
                  <el-tag v-if="article.category" size="small" effect="plain" class="category-tag">
                    {{ article.category.name }}
                  </el-tag>
                  <h2 class="title">{{ article.title }}</h2>
                </div>
                <div v-if="article.tags?.length" class="tag-row">
                  <el-tag
                    v-for="t in article.tags"
                    :key="t.id"
                    size="small"
                    effect="light"
                    class="tag-pill"
                    @click.stop="filterByTag(t.id)"
                  >
                    {{ t.name }}
                  </el-tag>
                </div>
                <p class="summary">{{ article.summary || '这是一篇关于技术探索的深度好文，点击进入查看更多精彩内容...' }}</p>
                <div class="meta">
                  <div class="meta-left">
                    <span class="meta-item">
                      <el-icon><Calendar /></el-icon>
                      {{ formatDate(article.createdAt) }}
                    </span>
                    <span class="meta-item">
                      <el-icon><View /></el-icon>
                      {{ article.views }} 次阅读
                    </span>
                    <span v-if="article.likeCount != null" class="meta-item">
                      <el-icon><Pointer /></el-icon>
                      {{ article.likeCount }}
                    </span>
                  </div>
                  <el-button link type="primary" class="read-more">
                    阅读全文 <el-icon><ArrowRight /></el-icon>
                  </el-button>
                </div>
              </div>
              <div class="article-cover" v-if="article.cover">
                <el-image :src="article.cover" fit="cover" lazy loading="lazy">
                  <template #placeholder>
                    <div class="image-slot">加载中...</div>
                  </template>
                </el-image>
              </div>
            </div>
          </el-card>
        </div>

        <div v-if="loading" class="status-info">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-if="noMore && articles.length" class="status-info no-more">
          <el-divider>已经到底啦</el-divider>
        </div>
        <el-empty v-if="!loading && !articles.length" description="暂无文章，试试调整筛选条件" />
      </div>

      <aside class="sidebar" aria-label="侧栏">
        <div class="sidebar-sticky">
        <el-card shadow="never" class="sidebar-card profile-card">
          <div class="profile-header">
            <el-avatar :size="60" :src="author.avatar" />
            <h3>{{ author.name }}</h3>
            <p>{{ author.bio }}</p>
            <el-button type="primary" size="small" plain @click="$router.push('/author')">博主主页</el-button>
          </div>
          <div class="stats">
            <div class="stat-item">
              <span class="count">{{ totalArticleHint }}</span>
              <span class="label">篇目(约)</span>
            </div>
            <div class="stat-item">
              <span class="count">{{ categories.length }}</span>
              <span class="label">分类</span>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="sidebar-card">
          <template #header>
            <span>分类</span>
          </template>
          <div class="chip-list">
            <el-button text :type="categoryId == null ? 'primary' : 'default'" size="small" @click="filterByCategory(undefined)">
              全部
            </el-button>
            <el-button
              v-for="c in categories"
              :key="c.id"
              text
              size="small"
              :type="categoryId === c.id ? 'primary' : 'default'"
              @click="filterByCategory(c.id)"
            >
              {{ c.name }}
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="sidebar-card">
          <template #header>
            <span>标签云</span>
          </template>
          <div class="tag-cloud">
            <el-tag
              v-for="t in tags"
              :key="t.id"
              :effect="tagId === t.id ? 'dark' : 'plain'"
              class="cloud-tag"
              @click="filterByTag(t.id)"
            >
              {{ t.name }}
            </el-tag>
          </div>
        </el-card>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, inject, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPortalArticles } from '../api/article'
import { listCategories } from '../api/category'
import { listPublicTags } from '../api/tag'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

type PortalSite = {
  navTitle: string
  navLogoUrl: string
  heroTitle: string
  heroSubtitle: string
  authorName: string
  authorBio: string
  authorAvatar: string
}
const portalSite = inject<Ref<PortalSite>>(
  'portalSite',
  ref({
    navTitle: '',
    navLogoUrl: '',
    heroTitle: '探索代码与生活',
    heroSubtitle: '分享技术洞见，记录成长瞬间',
    authorName: '博主',
    authorBio: '记录技术与生活',
    authorAvatar: ''
  })
)

const articles = ref<any[]>([])
const loading = ref(false)
const noMore = ref(false)
const page = ref(0)
const size = 10
const totalElements = ref(0)

const categories = ref<{ id: number; name: string }[]>([])
const tags = ref<{ id: number; name: string }[]>([])
const author = computed(() => ({
  name: portalSite.value.authorName || '博主',
  bio: portalSite.value.authorBio || '记录技术与生活',
  avatar: portalSite.value.authorAvatar || ''
}))

const keywordInput = ref('')

const categoryId = computed(() => {
  const v = route.query.categoryId
  if (v === undefined || v === '') return undefined
  const n = Number(v)
  return Number.isFinite(n) ? n : undefined
})

const tagId = computed(() => {
  const v = route.query.tagId
  if (v === undefined || v === '') return undefined
  const n = Number(v)
  return Number.isFinite(n) ? n : undefined
})

const keyword = computed(() => {
  const q = route.query.q
  return typeof q === 'string' ? q.trim() : ''
})

const listKey = computed(() => `${categoryId.value ?? ''}-${tagId.value ?? ''}-${keyword.value}`)

const disabled = computed(() => loading.value || noMore.value)

const totalArticleHint = computed(() => {
  if (totalElements.value > 0) return String(totalElements.value)
  return articles.value.length ? `${articles.value.length}+` : '0'
})

const activeFilterHint = computed(() => {
  const parts: string[] = []
  if (keyword.value) parts.push(`关键词「${keyword.value}」`)
  if (categoryId.value != null) {
    const c = categories.value.find((x) => x.id === categoryId.value)
    parts.push(`分类：${c?.name ?? categoryId.value}`)
  }
  if (tagId.value != null) {
    const t = tags.value.find((x) => x.id === tagId.value)
    parts.push(`标签：${t?.name ?? tagId.value}`)
  }
  return parts.length ? parts.join(' · ') : ''
})

function syncKeywordFromRoute() {
  keywordInput.value = keyword.value
}

function filterByCategory(id?: number) {
  const q = { ...route.query } as Record<string, string>
  if (id == null) delete q.categoryId
  else q.categoryId = String(id)
  router.push({ path: '/', query: q })
}

function filterByTag(id: number) {
  const q = { ...route.query } as Record<string, string>
  if (tagId.value === id) delete q.tagId
  else q.tagId = String(id)
  router.push({ path: '/', query: q })
}

function applySearch() {
  const q = { ...route.query } as Record<string, string>
  const k = keywordInput.value.trim()
  if (k) q.q = k
  else delete q.q
  router.push({ path: '/', query: q })
}

function clearFilters() {
  router.push({ path: '/' })
}

async function resetAndLoadFirst() {
  articles.value = []
  noMore.value = false
  page.value = 0
  await loadMore()
}

const loadMore = async () => {
  if (loading.value || noMore.value) return

  loading.value = true
  try {
    const res: any = await getPortalArticles({
      page: page.value,
      size,
      categoryId: categoryId.value,
      tagId: tagId.value,
      keyword: keyword.value || undefined
    })
    if (res.code === 200) {
      const { content, last, totalElements: te } = res.data
      articles.value.push(...(content || []))
      noMore.value = !!last
      page.value++
      if (typeof te === 'number') totalElements.value = te
    }
  } catch {
    ElMessage.error('加载文章列表失败')
  } finally {
    loading.value = false
  }
}

const goToDetail = (id: number) => {
  router.push(`/article/${id}`)
}

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

watch(
  () => listKey.value,
  () => {
    syncKeywordFromRoute()
    resetAndLoadFirst()
  }
)

onMounted(async () => {
  syncKeywordFromRoute()
  try {
    const [catRes, tagRes]: any[] = await Promise.all([listCategories(), listPublicTags()])
    if (catRes.code === 200) categories.value = catRes.data || []
    if (tagRes.code === 200) tags.value = tagRes.data || []
  } catch {
    /* 侧栏可选 */
  }
  await resetAndLoadFirst()
})
</script>

<style scoped>
.home-container {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  /* 不可使用 clip/hidden：会破坏侧栏 position:sticky（与视口吸顶） */
  overflow-x: visible;
}

.welcome-banner {
  text-align: center;
  padding: 56px 20px 40px;
  background-color: var(--portal-card, #fff);
  border-bottom: 1px solid var(--portal-border, #f1f5f9);
}
.hero-title {
  font-size: clamp(2rem, 6vw, 3.5rem);
  font-weight: 900;
  color: var(--portal-text, #0f172a);
  margin-bottom: 16px;
  background: linear-gradient(to right, #2563eb, #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1.2;
  padding-bottom: 8px;
}
.hero-subtitle {
  font-size: clamp(1rem, 2.5vw, 1.35rem);
  color: var(--portal-muted, #64748b);
  line-height: 1.6;
  max-width: 800px;
  margin: 0 auto 20px;
}

.search-row {
  max-width: 480px;
  margin: 0 auto;
}
.search-input {
  width: 100%;
}

.filter-hint {
  margin-top: 16px;
  font-size: 14px;
  color: var(--portal-muted, #64748b);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.content-wrapper {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  margin: 40px 0;
  padding: 0 clamp(16px, 4vw, 40px);
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 300px);
  gap: clamp(16px, 3vw, 40px);
  box-sizing: border-box;
  align-items: stretch;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.article-item {
  cursor: pointer;
}

.article-card {
  border: none;
  border-radius: 16px;
  background: var(--portal-card, #fff);
  border: 1px solid var(--portal-border, transparent);
  max-width: 100%;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.article-card :deep(.el-card__body) {
  max-width: 100%;
  box-sizing: border-box;
}
.article-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.card-body {
  display: flex;
  gap: 24px;
  padding: 8px;
  min-width: 0;
}

.article-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.article-header {
  margin-bottom: 8px;
}
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}
.tag-pill {
  cursor: pointer;
}
.category-tag {
  margin-bottom: 8px;
  border-radius: 4px;
}
.title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--portal-text, #1e293b);
  line-height: 1.4;
  transition: color 0.2s;
}
.article-card:hover .title {
  color: #3b82f6;
}

.summary {
  font-size: 15px;
  color: var(--portal-muted, #64748b);
  line-height: 1.6;
  margin-bottom: 20px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.meta-left {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #94a3b8;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.read-more {
  font-weight: 600;
  font-size: 14px;
}

.article-cover {
  width: 200px;
  height: 140px;
  flex-shrink: 0;
  border-radius: 12px;
  overflow: hidden;
}
.article-cover .el-image {
  width: 100%;
  height: 100%;
}

.status-info {
  padding: 40px 0;
}
.no-more {
  color: #cbd5e1;
}

.sidebar {
  min-width: 0;
}

/* 相对视口吸顶：依赖整页滚动（PortalLayout 已取消 el-main 内部滚动） */
.sidebar-sticky {
  position: sticky;
  top: var(--portal-sticky-offset, 76px);
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
}
.sidebar-card {
  border-radius: 16px;
  border: 1px solid var(--portal-border, #e2e8f0);
  background: var(--portal-card, #fff);
}
.profile-card {
  text-align: center;
  padding: 20px 0;
}
.profile-header h3 {
  margin: 16px 0 4px;
  color: var(--portal-text, #1e293b);
}
.profile-header p {
  font-size: 14px;
  color: var(--portal-muted, #64748b);
  margin-bottom: 12px;
}
.stats {
  display: flex;
  justify-content: space-around;
  border-top: 1px solid var(--portal-border, #f1f5f9);
  padding-top: 20px;
}
.stat-item {
  display: flex;
  flex-direction: column;
}
.stat-item .count {
  font-size: 18px;
  font-weight: 700;
  color: var(--portal-text, #1e293b);
}
.stat-item .label {
  font-size: 12px;
  color: #94a3b8;
}

.chip-list {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.cloud-tag {
  cursor: pointer;
}

@media (max-width: 900px) {
  .content-wrapper {
    grid-template-columns: 1fr;
  }
  .sidebar {
    display: none;
  }
}

@media (max-width: 600px) {
  .card-body {
    flex-direction: column-reverse;
  }
  .article-cover {
    width: 100%;
    height: 180px;
  }
}
</style>
