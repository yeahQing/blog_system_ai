<template>
  <div class="page guestbook-page">
    <header class="page-head">
      <h1>留言板</h1>
      <p class="sub">欢迎留言与交流（内容经简单校验后展示）</p>
    </header>

    <el-card class="form-card" shadow="never">
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="昵称" required>
              <el-input v-model="form.nickname" maxlength="64" show-word-limit placeholder="怎么称呼您" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="邮箱（选填）">
              <el-input v-model="form.email" type="email" maxlength="120" placeholder="方便回复时联系" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="留言内容" required>
          <el-input v-model="form.content" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">发表留言</el-button>
      </el-form>
    </el-card>

    <div v-loading="loading" class="list">
      <el-empty v-if="!loading && rows.length === 0" description="还没有留言，来抢沙发吧" />
      <el-card v-for="m in rows" :key="m.id" class="msg-card" shadow="hover">
        <div class="msg-head">
          <strong>{{ m.nickname }}</strong>
          <span class="time">{{ formatTime(m.createdAt) }}</span>
        </div>
        <p class="msg-body">{{ m.content }}</p>
      </el-card>
      <div v-if="totalPages > 1" class="pager">
        <el-pagination
          v-model:current-page="page"
          layout="prev, pager, next"
          :total="totalElements"
          :page-size="size"
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listGuestbook, createGuestbook } from '../api/guestbook'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const submitting = ref(false)
const rows = ref<any[]>([])
const page = ref(1)
const size = 15
const totalElements = ref(0)
const totalPages = ref(0)

const form = ref({ nickname: '', email: '', content: '' })

const load = async () => {
  loading.value = true
  try {
    const res: any = await listGuestbook({ page: page.value - 1, size })
    if (res.code === 200 && res.data) {
      rows.value = res.data.content || []
      totalElements.value = res.data.totalElements ?? 0
      totalPages.value = res.data.totalPages ?? 0
    }
  } catch {
    ElMessage.error('加载留言失败')
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!form.value.nickname?.trim() || !form.value.content?.trim()) {
    ElMessage.warning('请填写昵称与内容')
    return
  }
  submitting.value = true
  try {
    const res: any = await createGuestbook({
      nickname: form.value.nickname.trim(),
      email: form.value.email?.trim() || undefined,
      content: form.value.content.trim()
    })
    if (res.code === 200) {
      ElMessage.success('留言已发布')
      form.value = { nickname: form.value.nickname, email: form.value.email, content: '' }
      page.value = 1
      await load()
    }
  } catch {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(load)

const formatTime = (s: string) => {
  const d = new Date(s)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.guestbook-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px clamp(16px, 4vw, 32px) 48px;
  box-sizing: border-box;
}
.page-head h1 {
  margin: 0 0 8px;
  font-size: 1.75rem;
  color: var(--portal-text, #0f172a);
}
.sub {
  margin: 0 0 24px;
  color: var(--portal-muted, #64748b);
  font-size: 14px;
}
.form-card {
  margin-bottom: 28px;
  border-radius: 12px;
}
.msg-card {
  margin-bottom: 16px;
  border-radius: 12px;
}
.msg-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}
.time {
  color: var(--portal-muted, #94a3b8);
  font-size: 13px;
}
.msg-body {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.6;
  color: var(--portal-text, #334155);
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
