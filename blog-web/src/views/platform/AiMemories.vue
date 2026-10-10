<template>
  <div class="page-container">
    <div class="page-header">
      <h2>记忆存储</h2>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div style="margin-bottom: 16px">
      <el-input-number v-model="userId" :min="1" @change="load" />
      <span style="margin-left: 12px; color: #909399; font-size: 13px">用户 ID（默认 1 = admin）</span>
    </div>

    <el-table :data="memories" border stripe v-loading="loading">
      <el-table-column prop="sessionId" label="会话 ID" width="220" />
      <el-table-column prop="userId" label="用户 ID" width="100" />
      <el-table-column label="消息数" width="100" align="center">
        <template #default="{ row }">{{ countMessages(row.messages) }}</template>
      </el-table-column>
      <el-table-column prop="summary" label="摘要" show-overflow-tooltip />
      <el-table-column prop="lastActiveAt" label="最后活跃" width="180">
        <template #default="{ row }">{{ formatTime(row.lastActiveAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="view(row)">查看</el-button>
          <el-button size="small" type="danger" @click="clear(row)">清除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="会话记忆详情" width="720px" destroy-on-close align-center>
      <div v-if="current">
        <el-alert v-if="current.summary" type="info" :closable="false" style="margin-bottom: 12px">
          <template #title>摘要：{{ current.summary }}</template>
        </el-alert>
        <h4>历史消息</h4>
        <div v-for="(m, i) in parseMessages(current.messages)" :key="i" class="mem-item" :class="m.role">
          <el-tag :type="m.role === 'user' ? 'primary' : 'success'" size="small">{{ m.role }}</el-tag>
          <div class="mem-content">{{ m.content }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listMemories, clearMemory } from '../../api/platform'

const loading = ref(false)
const userId = ref(1)
const memories = ref<any[]>([])
const dialogVisible = ref(false)
const current = ref<any>(null)

async function load() {
  loading.value = true
  try {
    const res: any = await listMemories(userId.value)
    if (res.code === 200) memories.value = res.data || []
  } finally {
    loading.value = false
  }
}

function view(row: any) {
  current.value = row
  dialogVisible.value = true
}

async function clear(row: any) {
  await ElMessageBox.confirm(`确认清除会话「${row.sessionId}」的记忆？此操作不可恢复。`, '提示', { type: 'warning' })
  const res: any = await clearMemory(row.sessionId)
  if (res.code === 200) {
    ElMessage.success('已清除')
    load()
  }
}

function countMessages(msgs: string): number {
  try {
    return JSON.parse(msgs || '[]').length
  } catch {
    return 0
  }
}

function parseMessages(msgs: string): any[] {
  try {
    return JSON.parse(msgs || '[]')
  } catch {
    return []
  }
}

function formatTime(t: string): string {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 24px; background: #fff; min-height: 100%; box-sizing: border-box; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; }
.mem-item { margin-bottom: 12px; padding: 10px; border-radius: 6px; background: #fafafa; }
.mem-item.user { background: #ecf5ff; }
.mem-item.assistant { background: #f0f9eb; }
.mem-content { margin-top: 6px; white-space: pre-wrap; word-break: break-word; font-size: 13px; }
</style>
