<template>
  <div class="page my-center">
    <h1>我的空间（本地）</h1>
    <p class="hint">当前为浏览器本地记录，清除站点数据后会丢失。完整账号体系可在后续版本接入。</p>

    <el-tabs v-model="tab">
      <el-tab-pane label="浏览记录" name="history">
        <el-empty v-if="history.length === 0" description="暂无记录" />
        <ul v-else class="list">
          <li v-for="h in history" :key="h.id + h.at">
            <router-link :to="`/article/${h.id}`">{{ h.title || '文章 #' + h.id }}</router-link>
            <span class="t">{{ h.at }}</span>
          </li>
        </ul>
        <el-button v-if="history.length" type="danger" text @click="clearHistory">清空记录</el-button>
      </el-tab-pane>
      <el-tab-pane label="收藏" name="fav">
        <el-empty v-if="favorites.length === 0" description="在文章页点击「收藏」即可加入" />
        <ul v-else class="list">
          <li v-for="f in favorites" :key="f.id">
            <router-link :to="`/article/${f.id}`">{{ f.title || '文章 #' + f.id }}</router-link>
            <el-button type="danger" link size="small" @click="removeFav(f.id)">移除</el-button>
          </li>
        </ul>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getHistory, clearHistory as clearHist, getFavorites, removeFavorite } from '../utils/portalStorage'

const tab = ref('history')
const history = ref<{ id: number; title: string; at: string }[]>([])
const favorites = ref<{ id: number; title: string }[]>([])

function load() {
  history.value = getHistory()
  favorites.value = getFavorites()
}

function clearHistory() {
  clearHist()
  history.value = []
}

function removeFav(id: number) {
  removeFavorite(id)
  favorites.value = getFavorites()
}

onMounted(load)
</script>

<style scoped>
.my-center {
  max-width: 640px;
  margin: 0 auto;
  padding: 24px clamp(16px, 4vw, 32px) 48px;
}
.my-center h1 {
  margin: 0 0 8px;
  font-size: 1.5rem;
  color: var(--portal-text, #0f172a);
}
.hint {
  margin: 0 0 20px;
  font-size: 13px;
  color: var(--portal-muted, #64748b);
  line-height: 1.5;
}
.list {
  list-style: none;
  padding: 0;
  margin: 0 0 16px;
}
.list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--portal-border, #e2e8f0);
}
.list a {
  color: #3b82f6;
  text-decoration: none;
}
.list a:hover {
  text-decoration: underline;
}
.t {
  font-size: 12px;
  color: #94a3b8;
  flex-shrink: 0;
}
</style>
