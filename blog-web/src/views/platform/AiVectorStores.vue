<template>
  <div class="page-container">
    <div class="page-header">
      <h2>向量库</h2>
      <div>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="warning" :icon="MagicStick" :loading="backfilling" @click="backfill">回填文章向量</el-button>
      </div>
    </div>

    <el-table :data="stores" border stripe v-loading="loading">
      <el-table-column prop="code" label="Code" width="160" />
      <el-table-column prop="name" label="名称" width="160" />
      <el-table-column prop="businessType" label="业务类型" width="120" />
      <el-table-column prop="tableName" label="物理表" width="200" />
      <el-table-column prop="dimension" label="维度" width="90" />
      <el-table-column prop="chunkStrategy" label="分块策略" width="120" />
      <el-table-column prop="chunkMaxChars" label="最大字符" width="100" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openSearch(row)">检索测试</el-button>
          <el-button size="small" :type="row.enabled ? 'info' : 'success'" @click="toggle(row)">
            {{ row.enabled ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-card shadow="never" style="margin-top: 16px; border-radius: 12px">
      <template #header><span>新建 Collection</span></template>
      <el-form :model="form" label-position="top" inline>
        <el-form-item label="Code"><el-input v-model="form.code" placeholder="article_chunks" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="form.businessType" style="width: 160px">
            <el-option label="ARTICLE" value="ARTICLE" />
            <el-option label="COMMENT" value="COMMENT" />
            <el-option label="GUESTBOOK" value="GUESTBOOK" />
          </el-select>
        </el-form-item>
        <el-form-item label="分块策略">
          <el-select v-model="form.chunkStrategy" style="width: 160px">
            <el-option label="MARKDOWN" value="MARKDOWN" />
            <el-option label="PLAINTEXT" value="PLAINTEXT" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大字符"><el-input-number v-model="form.chunkMaxChars" :min="200" :max="4000" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="creating" @click="create">创建</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 检索测试弹窗 -->
    <el-dialog v-model="searchVisible" title="检索测试" width="720px" destroy-on-close align-center>
      <el-input v-model="searchQuery" placeholder="输入查询文本" @keyup.enter="doSearch">
        <template #append>
          <el-button :loading="searching" @click="doSearch">搜索</el-button>
        </template>
      </el-input>
      <div style="margin-top: 16px">
        <div v-for="h in searchHits" :key="h.chunkId" class="hit-item">
          <div class="hit-header">
            <span class="hit-score">相似度 {{ (h.score * 100).toFixed(1) }}%</span>
            <span class="hit-title">《{{ h.articleTitle }}》</span>
            <el-tag size="small">{{ h.chunkType }}</el-tag>
          </div>
          <div class="hit-content">{{ h.content }}</div>
        </div>
        <el-empty v-if="!searchHits.length && !searching" description="输入查询后展示结果" :image-size="60" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, MagicStick } from '@element-plus/icons-vue'
import { listVectorStores, createVectorStore, toggleVectorStoreEnabled, ragBackfill, ragSearch } from '../../api/platform'

const loading = ref(false)
const creating = ref(false)
const backfilling = ref(false)
const stores = ref<any[]>([])
const form = ref<any>({ businessType: 'ARTICLE', chunkStrategy: 'MARKDOWN', chunkMaxChars: 800 })

const searchVisible = ref(false)
const searchQuery = ref('')
const searching = ref(false)
const searchHits = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    const res: any = await listVectorStores({ all: true })
    if (res.code === 200) stores.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function create() {
  if (!form.value.code || !form.value.name) {
    ElMessage.warning('Code 和名称不能为空')
    return
  }
  creating.value = true
  try {
    const res: any = await createVectorStore(form.value)
    if (res.code === 200) {
      ElMessage.success('创建成功')
      form.value = { businessType: 'ARTICLE', chunkStrategy: 'MARKDOWN', chunkMaxChars: 800 }
      load()
    }
  } finally {
    creating.value = false
  }
}

async function toggle(row: any) {
  const res: any = await toggleVectorStoreEnabled(row.id, !row.enabled)
  if (res.code === 200) {
    ElMessage.success(row.enabled ? '已禁用' : '已启用')
    load()
  }
}

async function backfill() {
  backfilling.value = true
  try {
    const res: any = await ragBackfill()
    if (res.code === 200) {
      ElMessage.success(`回填完成：总计 ${res.data.total}，新建 ${res.data.processed}，跳过 ${res.data.skipped}`)
    }
  } finally {
    backfilling.value = false
  }
}

function openSearch(_row: any) {
  searchQuery.value = ''
  searchHits.value = []
  searchVisible.value = true
}

async function doSearch() {
  if (!searchQuery.value.trim()) return
  searching.value = true
  try {
    const res: any = await ragSearch(searchQuery.value, 5)
    if (res.code === 200) searchHits.value = res.data || []
  } finally {
    searching.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 24px; background: #fff; min-height: 100%; box-sizing: border-box; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; }
.hit-item { border: 1px solid #ebeef5; border-radius: 8px; padding: 12px; margin-bottom: 12px; }
.hit-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.hit-score { color: #e6a23c; font-weight: 600; font-size: 13px; }
.hit-title { color: #409eff; font-size: 13px; }
.hit-content { white-space: pre-wrap; word-break: break-word; font-size: 13px; color: #606266; max-height: 160px; overflow: auto; }
</style>
