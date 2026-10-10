<template>
  <div class="page-container">
    <div class="page-header">
      <h2>模型管理</h2>
      <el-button type="primary" :icon="Plus" @click="openEdit()">新增模型</el-button>
    </div>

    <el-table v-loading="loading" :data="models" border stripe>
      <el-table-column prop="name" label="名称" width="160" />
      <el-table-column prop="type" label="类型" width="110">
        <template #default="{ row }">
          <el-tag :type="row.type === 'CHAT' ? 'primary' : 'success'" size="small">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="provider" label="Provider" width="130" />
      <el-table-column prop="modelId" label="Model ID" width="160" />
      <el-table-column prop="dimension" label="维度" width="90" />
      <el-table-column label="默认" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault" type="warning" size="small">默认</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="!row.isDefault" size="small" type="warning" @click="setDefault(row)">设为默认</el-button>
          <el-button size="small" :type="row.enabled ? 'info' : 'success'" @click="toggleEnabled(row)">
            {{ row.enabled ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" :disabled="row.isDefault" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑模型' : '新增模型'" width="560px" destroy-on-close align-center>
      <el-form :model="editing" label-position="top">
        <el-form-item label="名称（唯一）">
          <el-input v-model="editing.name" placeholder="如 minimax-m2.7" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="editing.type" style="width: 100%">
            <el-option label="CHAT（对话）" value="CHAT" />
            <el-option label="EMBEDDING（向量化）" value="EMBEDDING" />
          </el-select>
        </el-form-item>
        <el-form-item label="Provider">
          <el-select v-model="editing.provider" style="width: 100%">
            <el-option label="MINIMAX" value="MINIMAX" />
            <el-option label="OLLAMA" value="OLLAMA" />
          </el-select>
        </el-form-item>
        <el-form-item label="Model ID">
          <el-input v-model="editing.modelId" placeholder="如 MiniMax-M2.7 / bge-m3" />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="editing.baseUrl" placeholder="如 https://api.minimax.chat" />
        </el-form-item>
        <el-form-item label="API Key（留空则不修改）">
          <el-input v-model="editing.apiKeyEncrypted" placeholder="********（编辑时留空表示不变）" show-password />
        </el-form-item>
        <el-form-item v-if="editing.type === 'EMBEDDING'" label="向量维度">
          <el-input-number v-model="editing.dimension" :min="1" :max="8192" style="width: 100%" />
        </el-form-item>
        <el-form-item label="参数（JSON，可选）">
          <el-input v-model="editing.params" type="textarea" :rows="2" placeholder='{"temperature":0.4,"maxTokens":4096}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listModels, createModel, updateModel, setDefaultModel, deleteModel } from '../../api/platform'

const loading = ref(false)
const saving = ref(false)
const models = ref<any[]>([])
const dialogVisible = ref(false)
const editing = ref<any>({})

async function load() {
  loading.value = true
  try {
    const res: any = await listModels()
    if (res.code === 200) models.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openEdit(row?: any) {
  editing.value = row
    ? { ...row, apiKeyEncrypted: '' }
    : { type: 'CHAT', provider: 'MINIMAX', enabled: true, isDefault: false, dimension: 1024 }
  dialogVisible.value = true
}

async function save() {
  if (!editing.value.name || !editing.value.modelId) {
    ElMessage.warning('名称和 Model ID 不能为空')
    return
  }
  saving.value = true
  try {
    const payload = { ...editing.value }
    // 编辑时 apiKey 为空则不传
    if (editing.value.id && !payload.apiKeyEncrypted) {
      delete payload.apiKeyEncrypted
    }
    const res: any = editing.value.id
      ? await updateModel(editing.value.id, payload)
      : await createModel(payload)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      load()
    }
  } finally {
    saving.value = false
  }
}

async function setDefault(row: any) {
  const res: any = await setDefaultModel(row.id)
  if (res.code === 200) {
    ElMessage.success('已设为默认')
    load()
  }
}

async function toggleEnabled(row: any) {
  const res: any = await updateModel(row.id, { enabled: !row.enabled })
  if (res.code === 200) {
    ElMessage.success(row.enabled ? '已禁用' : '已启用')
    load()
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除模型「${row.name}」？`, '提示', { type: 'warning' })
  const res: any = await deleteModel(row.id)
  if (res.code === 200) {
    ElMessage.success('已删除')
    load()
  }
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 24px; background: #fff; min-height: 100%; box-sizing: border-box; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; }
</style>
