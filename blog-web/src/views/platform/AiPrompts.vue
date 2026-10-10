<template>
  <div class="page-container">
    <div class="page-header">
      <h2>提示词管理</h2>
      <div>
        <el-input v-model="code" placeholder="输入 Prompt code，如 agent_system" style="width: 240px" clearable @keyup.enter="load" />
        <el-button type="primary" :icon="Search" @click="load" style="margin-left: 8px">查询</el-button>
        <el-button type="success" :icon="Plus" @click="openCreate">新建版本</el-button>
      </div>
    </div>

    <el-alert v-if="active" type="success" :closable="false" style="margin-bottom: 16px">
      <template #title>
        当前生效版本：<b>{{ active.code }}</b> · v{{ active.version }} · {{ active.name }}
      </template>
    </el-alert>

    <el-card v-if="active" shadow="never" style="margin-bottom: 16px; border-radius: 12px">
      <template #header>
        <span>生效版本内容</span>
        <el-button text type="primary" @click="openEdit(active)">编辑（新建版本）</el-button>
      </template>
      <pre class="prompt-content">{{ active.content }}</pre>
    </el-card>

    <h3 style="margin: 16px 0 8px">版本历史</h3>
    <el-table :data="versions" border stripe v-loading="loading">
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="name" label="名称" width="200" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isActive" type="success" size="small">生效中</el-tag>
          <el-tag v-else type="info" size="small">历史</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">基于此新建</el-button>
          <el-button v-if="!row.isActive" size="small" type="warning" @click="rollback(row)">回滚</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建提示词版本" width="720px" destroy-on-close align-center>
      <el-form :model="editing" label-position="top">
        <el-form-item label="Code">
          <el-input v-model="editing.code" :disabled="!!active" placeholder="如 agent_system" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="editing.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="editing.type" style="width: 100%">
            <el-option label="SYSTEM" value="SYSTEM" />
            <el-option label="TEMPLATE" value="TEMPLATE" />
            <el-option label="TOOL" value="TOOL" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容（支持 {placeholder} 占位符）">
          <el-input v-model="editing.content" type="textarea" :rows="12" style="font-family: monospace" />
        </el-form-item>
        <el-form-item label="变量定义（JSON，可选）">
          <el-input v-model="editing.variables" type="textarea" :rows="2" placeholder='[{"name":"topic","desc":"文章主题"}]' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存为新版本</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { getActivePrompt, getPromptVersions, savePrompt, rollbackPrompt } from '../../api/platform'

const loading = ref(false)
const saving = ref(false)
const code = ref('agent_system')
const active = ref<any>(null)
const versions = ref<any[]>([])
const dialogVisible = ref(false)
const editing = ref<any>({})

async function load() {
  if (!code.value) return
  loading.value = true
  try {
    const [actRes, verRes]: any[] = await Promise.all([
      getActivePrompt(code.value),
      getPromptVersions(code.value)
    ])
    active.value = actRes.code === 200 ? actRes.data : null
    versions.value = verRes.code === 200 ? verRes.data || [] : []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = { type: 'SYSTEM', content: '', variables: '[]' }
  dialogVisible.value = true
}

function openEdit(row: any) {
  editing.value = { ...row, variables: row.variables || '[]' }
  dialogVisible.value = true
}

async function save() {
  if (!editing.value.code || !editing.value.content) {
    ElMessage.warning('Code 和内容不能为空')
    return
  }
  saving.value = true
  try {
    const res: any = await savePrompt(editing.value)
    if (res.code === 200) {
      ElMessage.success('已保存为新版本并设为生效')
      dialogVisible.value = false
      code.value = editing.value.code
      load()
    }
  } finally {
    saving.value = false
  }
}

async function rollback(row: any) {
  await ElMessageBox.confirm(`确认回滚到 v${row.version}？当前生效版本将被替换。`, '提示', { type: 'warning' })
  const res: any = await rollbackPrompt(row.id)
  if (res.code === 200) {
    ElMessage.success('已回滚')
    load()
  }
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 24px; background: #fff; min-height: 100%; box-sizing: border-box; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; }
.prompt-content { white-space: pre-wrap; word-break: break-word; font-family: monospace; font-size: 13px; background: #f6f8fa; padding: 12px; border-radius: 6px; margin: 0; max-height: 320px; overflow: auto; }
</style>
