<template>
  <div class="page-container">
    <div class="page-header">
      <h2>消息流</h2>
      <el-button :icon="Refresh" @click="loadSessions">刷新</el-button>
    </div>

    <div class="messages-layout">
      <!-- 左侧会话列表 -->
      <div class="session-list">
        <div class="list-title">会话（{{ sessions.length }}）</div>
        <div
          v-for="s in sessions"
          :key="s.session_id"
          class="session-item"
          :class="{ active: currentSession === s.session_id }"
          @click="selectSession(s.session_id)"
        >
          <div class="session-id">{{ s.session_id }}</div>
          <div class="session-meta">{{ s.msg_count }} 条 · {{ formatTime(s.last_active) }}</div>
        </div>
        <el-empty v-if="!sessions.length" description="暂无会话" :image-size="60" />
      </div>

      <!-- 右侧消息流 -->
      <div class="message-stream">
        <template v-if="currentSession">
          <div class="stream-header">
            <span>会话：{{ currentSession }}</span>
            <div>
              <el-select v-model="roleFilter" size="small" style="width: 140px" @change="loadMessages">
                <el-option label="全部角色" value="" />
                <el-option label="USER" value="USER" />
                <el-option label="ASSISTANT" value="ASSISTANT" />
                <el-option label="TOOL" value="TOOL" />
                <el-option label="SYSTEM" value="SYSTEM" />
              </el-select>
              <el-button size="small" :icon="Refresh" @click="loadMessages" style="margin-left: 8px" />
            </div>
          </div>
          <div class="message-list" v-loading="loading">
            <div v-for="m in messages" :key="m.id" class="message-item" :class="m.role.toLowerCase()">
              <div class="msg-header">
                <el-tag :type="roleTagType(m.role)" size="small">{{ m.role }}</el-tag>
                <span v-if="m.toolName" class="tool-name">🔧 {{ m.toolName }}</span>
                <span v-if="m.modelName" class="model-name">{{ m.modelName }}</span>
                <span class="msg-time">{{ formatTime(m.createdAt) }}</span>
                <span v-if="m.latencyMs" class="latency">{{ m.latencyMs }}ms</span>
              </div>
              <div v-if="m.content" class="msg-content">{{ m.content }}</div>
              <div v-if="m.toolArgs" class="msg-tool">
                <div class="tool-label">入参：</div>
                <pre>{{ formatJson(m.toolArgs) }}</pre>
              </div>
              <div v-if="m.toolResult" class="msg-tool">
                <div class="tool-label">结果：</div>
                <pre>{{ formatJson(m.toolResult) }}</pre>
              </div>
            </div>
            <el-empty v-if="!messages.length && !loading" description="无消息" :image-size="60" />
          </div>
        </template>
        <el-empty v-else description="请选择左侧会话查看消息流" :image-size="80" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { listSessions, getMessagesBySession } from '../../api/platform'

const sessions = ref<any[]>([])
const messages = ref<any[]>([])
const currentSession = ref('')
const roleFilter = ref('')
const loading = ref(false)

async function loadSessions() {
  const res: any = await listSessions()
  if (res.code === 200) {
    sessions.value = res.data || []
    if (sessions.value.length && !currentSession.value) {
      selectSession(sessions.value[0].session_id)
    }
  }
}

async function selectSession(sid: string) {
  currentSession.value = sid
  await loadMessages()
}

async function loadMessages() {
  if (!currentSession.value) return
  loading.value = true
  try {
    const params = roleFilter.value ? { role: roleFilter.value } : undefined
    const res: any = await getMessagesBySession(currentSession.value, params)
    if (res.code === 200) messages.value = res.data || []
  } finally {
    loading.value = false
  }
}

function roleTagType(role: string): any {
  return { USER: 'primary', ASSISTANT: 'success', TOOL: 'warning', SYSTEM: 'info' }[role] || 'info'
}

function formatTime(t: string): string {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

function formatJson(s: string): string {
  try {
    return JSON.stringify(JSON.parse(s), null, 2)
  } catch {
    return s || ''
  }
}

onMounted(loadSessions)
</script>

<style scoped>
.page-container { padding: 24px; background: #fff; min-height: 100%; box-sizing: border-box; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; }
.messages-layout { display: flex; gap: 16px; height: calc(100vh - 180px); }
.session-list { width: 280px; border: 1px solid #ebeef5; border-radius: 8px; overflow: auto; }
.list-title { padding: 12px 16px; font-weight: 600; border-bottom: 1px solid #ebeef5; background: #fafafa; }
.session-item { padding: 12px 16px; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
.session-item:hover { background: #f5f7fa; }
.session-item.active { background: #ecf5ff; border-left: 3px solid #409eff; }
.session-id { font-size: 13px; font-family: monospace; word-break: break-all; }
.session-meta { font-size: 12px; color: #909399; margin-top: 4px; }
.message-stream { flex: 1; border: 1px solid #ebeef5; border-radius: 8px; display: flex; flex-direction: column; }
.stream-header { padding: 12px 16px; border-bottom: 1px solid #ebeef5; display: flex; justify-content: space-between; align-items: center; font-size: 13px; }
.message-list { flex: 1; overflow: auto; padding: 16px; }
.message-item { margin-bottom: 16px; padding: 12px; border-radius: 8px; background: #fafafa; }
.message-item.assistant { background: #f0f9eb; }
.message-item.tool { background: #fdf6ec; }
.msg-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 12px; flex-wrap: wrap; }
.tool-name { color: #e6a23c; font-weight: 600; }
.model-name { color: #909399; }
.msg-time { color: #c0c4cc; }
.latency { color: #909399; font-size: 11px; }
.msg-content { white-space: pre-wrap; word-break: break-word; font-size: 14px; line-height: 1.6; }
.msg-tool { margin-top: 8px; }
.tool-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.msg-tool pre { background: #fff; padding: 8px; border-radius: 4px; font-size: 12px; margin: 0; max-height: 200px; overflow: auto; }
</style>
