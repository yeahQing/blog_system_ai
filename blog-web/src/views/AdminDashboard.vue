<template>
  <div class="dash-page" v-loading="loading">
    <header class="dash-head">
      <div>
        <h1 class="title">数据概览</h1>
        <p class="sub">文章、互动与站点资源一览</p>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </header>

    <el-row :gutter="20" class="stat-grid">
      <el-col :xs="24" :sm="12" :lg="8" v-for="card in cards" :key="card.key">
        <el-card shadow="hover" class="stat-card" :class="card.tone">
          <div class="stat-inner">
            <span class="label">{{ card.label }}</span>
            <span class="value">{{ formatNum(stats[card.key]) }}</span>
            <span v-if="card.hint" class="hint">{{ card.hint }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboardStats } from '../api/dashboard'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const loading = ref(false)

type StatKey =
  | 'articleTotal'
  | 'articlePublished'
  | 'articleDrafts'
  | 'totalViews'
  | 'categories'
  | 'tags'
  | 'comments'
  | 'guestbookMessages'
  | 'friendLinksTotal'
  | 'friendLinksEnabled'

const emptyStats: Record<StatKey, number> = {
  articleTotal: 0,
  articlePublished: 0,
  articleDrafts: 0,
  totalViews: 0,
  categories: 0,
  tags: 0,
  comments: 0,
  guestbookMessages: 0,
  friendLinksTotal: 0,
  friendLinksEnabled: 0
}

const stats = ref<Record<StatKey, number>>({ ...emptyStats })

const cards: { key: StatKey; label: string; hint?: string; tone?: string }[] = [
  { key: 'articleTotal', label: '文章总数', hint: '未删除', tone: 'tone-blue' },
  { key: 'articlePublished', label: '已发布', tone: 'tone-green' },
  { key: 'articleDrafts', label: '草稿', tone: 'tone-amber' },
  { key: 'totalViews', label: '累计阅读量', tone: 'tone-violet' },
  { key: 'categories', label: '分类数' },
  { key: 'tags', label: '标签数' },
  { key: 'comments', label: '评论数' },
  { key: 'guestbookMessages', label: '留言数' },
  { key: 'friendLinksTotal', label: '常用链接总数', hint: '含未启用' },
  { key: 'friendLinksEnabled', label: '已启用链接', tone: 'tone-teal' }
]

function formatNum(n: number | undefined) {
  if (n == null || Number.isNaN(n)) return '0'
  return n > 1_000_000 ? `${(n / 1_000_000).toFixed(1)}M` : String(n)
}

const load = async () => {
  loading.value = true
  try {
    const res: any = await getDashboardStats()
    if (res.code === 200 && res.data) {
      stats.value = { ...emptyStats, ...res.data }
    }
  } catch {
    ElMessage.error('加载统计失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.dash-page {
  padding: 24px;
  min-height: 100%;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 40%);
  box-sizing: border-box;
}

.dash-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.sub {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.stat-grid {
  margin: 0 !important;
}

.stat-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  margin-bottom: 20px;
}

.stat-card :deep(.el-card__body) {
  padding: 20px 22px;
}

.stat-inner {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label {
  font-size: 14px;
  color: #64748b;
}

.value {
  font-size: 32px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.15;
  letter-spacing: -0.02em;
}

.hint {
  font-size: 12px;
  color: #94a3b8;
}

.tone-blue {
  border-color: #bfdbfe;
  background: linear-gradient(135deg, #eff6ff 0%, #fff 70%);
}
.tone-green {
  border-color: #bbf7d0;
  background: linear-gradient(135deg, #ecfdf5 0%, #fff 70%);
}
.tone-amber {
  border-color: #fde68a;
  background: linear-gradient(135deg, #fffbeb 0%, #fff 70%);
}
.tone-violet {
  border-color: #ddd6fe;
  background: linear-gradient(135deg, #f5f3ff 0%, #fff 70%);
}
.tone-teal {
  border-color: #99f6e4;
  background: linear-gradient(135deg, #f0fdfa 0%, #fff 70%);
}
</style>
