<template>
  <div class="page tools-page">
    <header class="page-head">
      <h1>常用站点与工具</h1>
      <p class="sub">整理日常用到的网站与服务，点击卡片会在新标签页打开，方便快速跳转。</p>
    </header>

    <div v-loading="loading" class="grid">
      <el-empty v-if="!loading && links.length === 0" description="暂无链接，可在后台「常用链接」里添加" />
      <a
        v-for="l in links"
        :key="l.id"
        class="tool-card"
        :href="l.url"
        target="_blank"
        rel="noopener noreferrer"
        :aria-label="`${l.name}，新窗口打开`"
      >
        <el-card shadow="hover" class="card-inner">
          <div class="card-top">
            <h3>{{ l.name }}</h3>
            <el-icon class="open-icon" aria-hidden="true"><TopRight /></el-icon>
          </div>
          <p v-if="l.description" class="desc">{{ l.description }}</p>
          <div class="card-foot">
            <span class="host">{{ linkHost(l.url) }}</span>
            <span class="hint">新标签打开</span>
          </div>
        </el-card>
      </a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { TopRight } from '@element-plus/icons-vue'
import { listPublicFriendLinks } from '../api/friendLink'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const links = ref<any[]>([])

function linkHost(url: string) {
  try {
    const u = new URL(url)
    return u.hostname + (u.pathname && u.pathname !== '/' ? u.pathname : '')
  } catch {
    return url
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await listPublicFriendLinks()
    if (res.code === 200) {
      links.value = res.data || []
    }
  } catch {
    ElMessage.error('加载链接失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.tools-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px clamp(16px, 4vw, 32px) 48px;
}
.page-head h1 {
  margin: 0 0 8px;
  font-size: 1.75rem;
  color: var(--portal-text, #0f172a);
}
.sub {
  margin: 0 0 28px;
  color: var(--portal-muted, #64748b);
  font-size: 14px;
  line-height: 1.55;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}
.tool-card {
  text-decoration: none;
  color: inherit;
  display: block;
  border-radius: 12px;
  transition: transform 0.15s ease;
}
.tool-card:hover {
  transform: translateY(-2px);
}
.tool-card:focus-visible {
  outline: 2px solid #3b82f6;
  outline-offset: 3px;
}
.card-inner {
  height: 100%;
  border-radius: 12px;
  transition: border-color 0.2s ease;
}
.tool-card:hover .card-inner {
  border-color: #93c5fd;
}
.card-inner :deep(.el-card__body) {
  padding: 18px 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 120px;
}
.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}
.card-top h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--portal-text, #1e293b);
  line-height: 1.35;
}
.open-icon {
  flex-shrink: 0;
  font-size: 18px;
  color: var(--portal-muted, #94a3b8);
}
.desc {
  margin: 0 0 auto;
  padding-bottom: 12px;
  font-size: 13px;
  color: var(--portal-muted, #64748b);
  line-height: 1.5;
  flex: 1;
}
.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid var(--el-border-color-lighter, #ebeef5);
  font-size: 12px;
}
.host {
  color: #3b82f6;
  word-break: break-all;
  min-width: 0;
}
.hint {
  flex-shrink: 0;
  color: var(--portal-muted, #94a3b8);
}
</style>
