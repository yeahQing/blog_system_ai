<template>
  <div class="agent-page">
    <div class="chat-area">
      <div class="chat-header">
        <h2>AI 创作助手</h2>
        <div>
          <span v-if="sessionId" class="session-info">会话：{{ sessionId.substring(0, 16) }}…</span>
          <el-button size="small" text @click="newSession">新对话</el-button>
        </div>
      </div>

      <div class="message-list" ref="listRef">
        <div v-for="(msg, i) in chatMessages" :key="i" class="chat-msg" :class="msg.role">
          <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
          <div class="bubble">
            <div v-if="msg.streaming && !msg.content" class="typing">思考中…（智能体正在调用工具完成写作，可能需要 1-2 分钟）</div>
            <div v-else class="bubble-content" v-html="renderContent(msg.content)"></div>
            <div v-if="msg.latencyMs" class="bubble-meta">{{ msg.latencyMs }}ms</div>
          </div>
        </div>
        <el-empty v-if="!chatMessages.length" description="输入指令开始对话，例如「写一篇 Docker 部署 Redis 的文章」" :image-size="80" />
      </div>

      <div class="quick-commands">
        <el-button size="small" v-for="cmd in quickCommands" :key="cmd" @click="send(cmd)" :disabled="loading">
          {{ cmd }}
        </el-button>
      </div>

      <div class="input-area">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          placeholder="输入消息，Ctrl+Enter 发送…"
          :disabled="loading"
          @keydown.ctrl.enter="send(input)"
          resize="none"
        />
        <el-button type="primary" :loading="loading" :icon="Promotion" @click="send(input)" style="margin-left: 8px; height: 100%">
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'

const input = ref('')
const loading = ref(false)
const sessionId = ref('')
// chatMessages 顶层用 ref 即可；内部对象用 reactive 包装，属性修改才能被 Vue 追踪
const chatMessages = ref<{ role: string; content: string; latencyMs?: number; streaming?: boolean }[]>([])
const listRef = ref<HTMLElement>()
const quickCommands = [
  '写一篇 Docker 部署 Redis 的文章',
  '我有哪些关于 PostgreSQL 的文章？',
  '帮我生成一个文章大纲，主题是 Spring Boot 性能优化'
]

/** 调用 SSE 流式接口，逐字接收智能体回复 */
async function send(message: string) {
  const text = (message || input.value).trim()
  if (!text || loading.value) return
  input.value = ''
  chatMessages.value.push({ role: 'user', content: text })
  // 用 reactive 包装的占位对象
  const assistantMsg = reactive<{ role: string; content: string; latencyMs?: number; streaming?: boolean }>({
    role: 'assistant',
    content: '思考中…（智能体正在调用工具完成写作，可能需要 1-3 分钟）',
    streaming: true
  })
  chatMessages.value.push(assistantMsg)
  loading.value = true
  await scrollToBottom()

  const baseURL = import.meta.env.VITE_API_URL || '/api'
  const token = localStorage.getItem('token') || ''

  // SSE 流式调用：fetch + ReadableStream 接收 server-sent events
  try {
    const resp = await fetch(`${baseURL}/admin/agent/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'Accept': 'text/event-stream'
      },
      body: JSON.stringify({ message: text, sessionId: sessionId.value || undefined, userId: 1 })
    })
    if (!resp.ok || !resp.body) {
      throw new Error(`HTTP ${resp.status}`)
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // SSE 事件以 \n\n 分隔
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      for (const evt of events) {
        const lines = evt.split('\n')
        let eventType = ''
        let dataStr = ''
        for (const line of lines) {
          if (line.startsWith('event:')) eventType = line.slice(6).trim()
          else if (line.startsWith('data:')) dataStr += line.slice(5).trim()
        }
        if (!dataStr || eventType === 'HEARTBEAT' || eventType === 'STARTED') continue
        try {
          const data = JSON.parse(dataStr)
          switch (eventType) {
            case 'ASSISTANT_MESSAGE':
              if (data.content) {
                assistantMsg.content += data.content
                await scrollToBottom()
              }
              break
            case 'TOOL_CALL': {
              const toolLabel = toolDisplayName(data.toolName)
              const args = data.toolData ? `（${truncate(data.toolData, 80)}）` : ''
              assistantMsg.content += `\n\n🔧 ${toolLabel}${args}...\n`
              await scrollToBottom()
              break
            }
            case 'TOOL_RESULT': {
              const toolLabel = toolDisplayName(data.toolName)
              const result = data.toolData || ''
              if (data.toolName === 'write_section') {
                assistantMsg.content += `\n${result}\n`
              } else {
                assistantMsg.content += `✅ ${toolLabel}：${truncate(result, 200)}\n`
              }
              await scrollToBottom()
              break
            }
            case 'DONE': {
              const m = (data.content || '').match(/sessionId=([^,]+)/)
              if (m) sessionId.value = m[1]
              const lm = (data.content || '').match(/latency=(\d+)ms/)
              assistantMsg.content += '\n\n✅ 完成'
              if (lm) assistantMsg.content += `（${lm[1]}ms）`
              break
            }
            case 'ERROR':
              assistantMsg.content += `\n\n❌ ${data.content || data.message || '执行失败'}`
              break
          }
        } catch (e) {
          console.error('解析 SSE 事件失败:', e, dataStr)
        }
      }
    }
    if (!assistantMsg.content || assistantMsg.content.trim() === '') {
      assistantMsg.content = '（智能体已执行完成，回复已截断，请到「消息流」查看详情）'
    }
  } catch (e: any) {
    assistantMsg.content = `❌ 请求失败：${e?.message || '网络错误'}`
  } finally {
    assistantMsg.streaming = false
    loading.value = false
    await scrollToBottom()
  }
}

/** 格式化 ai_message 为可显示文本 */
function formatMessage(m: any): string {
  const role = m.role || ''
  const tool = m.toolName || ''
  const content = m.content || ''
  const args = m.toolArgs ? `\n  入参：${truncate(m.toolArgs, 200)}` : ''
  const result = m.toolResult ? `\n  结果：${truncate(m.toolResult, 400)}` : ''
  if (role === 'USER') return `👤 用户：${content}`
  if (role === 'ASSISTANT') {
    if (tool) return `🔧 工具：${toolDisplayName(tool)}${args}${result}`
    return `🤖 助手：${truncate(content, 1500)}`
  }
  if (role === 'TOOL') {
    return `📋 ${toolDisplayName(tool)}${result || content ? '：' + truncate(content, 200) : ''}`
  }
  if (role === 'SYSTEM') return `⚙️ 系统：${truncate(content, 200)}`
  return `${role}：${truncate(content, 500)}`
}

function truncate(s: string, n: number): string {
  if (!s) return ''
  return s.length > n ? s.substring(0, n) + '…' : s
}

function newSession() {
  sessionId.value = ''
  chatMessages.value = []
  ElMessage.success('已开始新对话')
}

function renderContent(text: string): string {
  // 简单转义 + 换行处理（不做完整 Markdown 渲染，避免引入依赖）
  return (text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br/>')
}

/** 工具名 → 人类可读标签 */
const TOOL_LABELS: Record<string, string> = {
  search_articles: '搜索历史文章',
  get_article_detail: '读取文章详情',
  generate_outline: '生成大纲',
  write_section: '写作章节',
  generate_summary: '生成摘要',
  suggest_titles: '推荐标题',
  generate_cover: '生成封面图',
  save_draft: '保存草稿'
}
function toolDisplayName(name: string): string {
  return TOOL_LABELS[name] || name
}

async function scrollToBottom() {
  await nextTick()
  if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
}
</script>

<style scoped>
.agent-page { padding: 24px; background: #f5f7fa; min-height: 100%; box-sizing: border-box; }
.chat-area { max-width: 900px; margin: 0 auto; background: #fff; border-radius: 12px; display: flex; flex-direction: column; height: calc(100vh - 130px); overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.chat-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #ebeef5; }
.chat-header h2 { margin: 0; font-size: 18px; }
.session-info { font-size: 12px; color: #909399; margin-right: 8px; font-family: monospace; }
.message-list { flex: 1; overflow: auto; padding: 20px; }
.chat-msg { display: flex; margin-bottom: 16px; gap: 10px; }
.chat-msg.user { flex-direction: row-reverse; }
.avatar { font-size: 24px; flex-shrink: 0; }
.bubble { max-width: 70%; padding: 10px 14px; border-radius: 10px; font-size: 14px; line-height: 1.6; }
.chat-msg.user .bubble { background: #409eff; color: #fff; }
.chat-msg.assistant .bubble { background: #f4f4f5; color: #303133; }
.bubble-content { word-break: break-word; }
.bubble-meta { font-size: 11px; color: #c0c4cc; margin-top: 4px; }
.chat-msg.user .bubble-meta { color: rgba(255,255,255,0.7); }
.typing { color: #909399; }
.quick-commands { padding: 8px 20px; border-top: 1px solid #f0f0f0; display: flex; gap: 8px; flex-wrap: wrap; }
.input-area { padding: 12px 20px; border-top: 1px solid #ebeef5; display: flex; align-items: stretch; }
.input-area :deep(.el-textarea) { flex: 1; }
</style>
