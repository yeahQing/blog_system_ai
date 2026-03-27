<template>
  <div class="page author-page">
    <el-card class="hero" shadow="never">
      <div class="hero-inner">
        <el-avatar :size="88" :src="author.avatar" />
        <div class="info">
          <h1>{{ author.name }}</h1>
          <p class="bio">{{ author.bio }}</p>
          <div class="actions">
            <el-button type="primary" @click="$router.push('/')">全部文章</el-button>
            <el-button @click="$router.push('/guestbook')">留言</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <h2 class="section-title">近期文章</h2>
    <div v-loading="loading" class="articles">
      <el-empty v-if="!loading && articles.length === 0" description="暂无已发布文章" />
      <el-card
        v-for="a in articles"
        :key="a.id"
        class="art-row"
        shadow="hover"
        @click="$router.push(`/article/${a.id}`)"
      >
        <h3>{{ a.title }}</h3>
        <p class="sum">{{ a.summary || '点击查看全文' }}</p>
        <div class="meta">
          <span>{{ formatDate(a.createdAt) }}</span>
          <span>{{ a.views }} 阅读</span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, inject, type Ref } from 'vue'
import { getPortalArticles } from '../api/article'
import { ElMessage } from 'element-plus'

type PortalSite = {
  authorName: string
  authorBio: string
  authorAvatar: string
}
const portalSite = inject<Ref<PortalSite>>(
  'portalSite',
  ref({ authorName: '博主', authorBio: '', authorAvatar: '' })
)

const author = computed(() => ({
  name: portalSite.value.authorName || '博主',
  bio: portalSite.value.authorBio || '',
  avatar: portalSite.value.authorAvatar || ''
}))

const loading = ref(false)
const articles = ref<any[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await getPortalArticles({ page: 0, size: 8 })
    if (res.code === 200 && res.data) {
      articles.value = res.data.content || []
    }
  } catch {
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
})

const formatDate = (dateStr: string) => {
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
}
</script>

<style scoped>
.author-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px clamp(16px, 4vw, 32px) 48px;
}
.hero {
  border-radius: 16px;
  margin-bottom: 32px;
}
.hero-inner {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  flex-wrap: wrap;
}
.info h1 {
  margin: 0 0 8px;
  font-size: 1.5rem;
  color: var(--portal-text, #0f172a);
}
.bio {
  margin: 0 0 16px;
  color: var(--portal-muted, #64748b);
  line-height: 1.6;
  max-width: 560px;
}
.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.section-title {
  font-size: 1.15rem;
  margin: 0 0 16px;
  color: var(--portal-text, #1e293b);
}
.art-row {
  margin-bottom: 14px;
  border-radius: 12px;
  cursor: pointer;
}
.art-row h3 {
  margin: 0 0 8px;
  font-size: 1.05rem;
}
.sum {
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--portal-muted, #64748b);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
