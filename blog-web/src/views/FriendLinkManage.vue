<template>
  <div class="fl-page">
    <header class="page-header">
      <div class="page-title-block">
        <h1 class="page-title">常用链接管理</h1>
        <p class="page-desc">维护前台「常用工具」页的站点与工具链接，方便访客一键跳转；仅「启用」的条目会按排序显示。</p>
      </div>
      <div class="page-actions">
        <el-button @click="load">
          <el-icon class="btn-icon"><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button @click="openFrontPreview">
          <el-icon class="btn-icon"><View /></el-icon>
          预览前台
        </el-button>
        <el-button type="primary" @click="openEdit()">
          <el-icon class="btn-icon"><Plus /></el-icon>
          新增链接
        </el-button>
      </div>
    </header>

    <el-alert type="success" show-icon :closable="false" class="tip-alert">
      <template #title>前台展示规则</template>
      访客在 <strong>常用工具</strong> 页面只能看到「启用」的链接；数字 <strong>排序</strong> 越小越靠前。关闭「启用」可临时下线而不删除记录。
    </el-alert>

    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="8">
        <div class="stat-card">
          <span class="stat-label">链接总数</span>
          <span class="stat-value">{{ rows.length }}</span>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card stat-card--accent">
          <span class="stat-label">已启用</span>
          <span class="stat-value">{{ enabledCount }}</span>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card">
          <span class="stat-label">未启用</span>
          <span class="stat-value">{{ rows.length - enabledCount }}</span>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="card-head">
          <span class="card-title">链接列表</span>
          <span class="card-sub">点击「编辑」修改名称、地址、备注与排序</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="rows" border stripe class="data-table" empty-text="暂无链接，点击右上角「新增链接」">
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" sortable />
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="url" label="URL" min-width="220" show-overflow-tooltip />
        <el-table-column prop="description" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="enabled" label="启用" width="88" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small" effect="plain">
              {{ row.enabled ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="visible"
      :title="editing?.id ? '编辑链接' : '新增链接'"
      width="520px"
      align-center
      destroy-on-close
      class="fl-dialog"
    >
      <el-form :model="form" label-position="top" class="dialog-form">
        <el-form-item label="显示名称" required>
          <el-input v-model="form.name" maxlength="64" show-word-limit placeholder="如：GitHub、图床、在线 JSON" />
        </el-form-item>
        <el-form-item label="链接地址" required>
          <el-input v-model="form.url" type="textarea" :rows="2" placeholder="https:// 开头完整 URL" />
        </el-form-item>
        <el-form-item label="备注（可选）">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="一句话说明用途，显示在卡片上" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序（越小越靠前）">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用展示">
              <div class="switch-row">
                <el-switch v-model="form.enabled" active-text="前台可见" inactive-text="仅后台保留" />
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { listAdminFriendLinks, saveFriendLink, deleteFriendLink } from '../api/friendLink'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, View, Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const visible = ref(false)
const editing = ref<any>(null)

const enabledCount = computed(() => rows.value.filter((r) => r.enabled !== false).length)

const form = reactive({
  name: '',
  url: '',
  description: '',
  sortOrder: 0,
  enabled: true
})

const load = async () => {
  loading.value = true
  try {
    const res: any = await listAdminFriendLinks()
    if (res.code === 200) {
      rows.value = res.data || []
    }
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function openFrontPreview() {
  const path = `${window.location.origin}/tools`
  window.open(path, '_blank', 'noopener,noreferrer')
}

const openEdit = (row?: any) => {
  editing.value = row || null
  if (row) {
    form.name = row.name
    form.url = row.url
    form.description = row.description || ''
    form.sortOrder = row.sortOrder ?? 0
    form.enabled = row.enabled !== false
  } else {
    form.name = ''
    form.url = ''
    form.description = ''
    form.sortOrder = 0
    form.enabled = true
  }
  visible.value = true
}

const submit = async () => {
  if (!form.name?.trim() || !form.url?.trim()) {
    ElMessage.warning('请填写名称与链接')
    return
  }
  saving.value = true
  try {
    const payload: any = {
      name: form.name.trim(),
      url: form.url.trim(),
      description: form.description?.trim() || undefined,
      sortOrder: form.sortOrder,
      enabled: form.enabled
    }
    if (editing.value?.id) {
      payload.id = editing.value.id
    }
    const res: any = await saveFriendLink(payload)
    if (res.code === 200) {
      ElMessage.success('已保存')
      visible.value = false
      await load()
    }
  } catch {
    /* */
  } finally {
    saving.value = false
  }
}

const remove = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除链接「${row.name}」？`, '确认', { type: 'warning' })
    const res: any = await deleteFriendLink(row.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      await load()
    }
  } catch {
    /* */
  }
}

onMounted(load)
</script>

<style scoped>
.fl-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px;
  background: #fff;
  min-height: 100%;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.page-title {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

.page-desc {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
  max-width: 560px;
}

.page-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.btn-icon {
  margin-right: 4px;
  vertical-align: middle;
}

.tip-alert :deep(.el-alert__description) {
  margin-top: 6px;
  line-height: 1.65;
  font-size: 14px;
}

.stats-row {
  margin: 0 !important;
}

.stat-card {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  height: 100%;
}

.stat-card--accent {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border-color: #a7f3d0;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.panel-card {
  border-radius: 12px;
  border: 1px solid #ebeef5;
}

.panel-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.card-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-sub {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}

.panel-card :deep(.el-card__body) {
  padding: 0 20px 20px;
}

.data-table {
  width: 100%;
}

.dialog-form {
  padding-right: 4px;
}

.switch-row {
  padding-top: 4px;
}
</style>
