<template>
  <div class="admin-system" v-loading="menuLoading">
    <header class="page-head">
      <h1 class="title">系统管理</h1>
      <p class="sub">修改登录密码、配置后台侧边栏菜单是否显示（「系统管理」始终保留，避免无法恢复）。</p>
    </header>

    <el-card shadow="never" class="panel">
      <template #header>
        <span class="card-title">修改密码</span>
      </template>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top" class="pwd-form">
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="pwdForm.currentPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码（至少 6 位）" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-button type="primary" :loading="pwdSaving" @click="submitPassword">保存新密码</el-button>
      </el-form>
    </el-card>

    <el-card shadow="never" class="panel mt">
      <template #header>
        <span class="card-title">侧边栏菜单</span>
      </template>
      <p class="hint">关闭后对应入口将从左侧菜单隐藏；直接输入地址仍可访问。保存后立即生效。</p>
      <div class="switch-list">
        <div v-for="row in menuRows" :key="row.key" class="switch-row">
          <span class="switch-label">{{ row.label }}</span>
          <el-switch v-model="menuForm[row.key]" />
        </div>
      </div>
      <div class="actions">
        <el-button type="primary" :loading="menuSaving" @click="submitMenu">保存菜单配置</el-button>
        <el-button @click="loadMenu">重新加载</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { changeAdminPassword, getMenuVisibility, saveMenuVisibility } from '../api/adminSystem'
import { useAdminMenuStore, ADMIN_MENU_KEYS } from '../stores/adminMenu'

const adminMenuStore = useAdminMenuStore()

const pwdFormRef = ref<FormInstance>()
const pwdSaving = ref(false)
const pwdForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules: FormRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_r, v, cb) => {
        if (v !== pwdForm.newPassword) cb(new Error('两次输入不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

const menuLoading = ref(false)
const menuSaving = ref(false)
const menuForm = reactive<Record<string, boolean>>({})

const menuRows = [
  { key: 'dashboard', label: '数据概览' },
  { key: 'site-settings', label: '站点设置' },
  { key: 'articles', label: '文章管理' },
  { key: 'categories', label: '分类管理' },
  { key: 'tags', label: '标签管理' },
  { key: 'friend-links', label: '常用链接' }
] as const

function syncMenuFormFromStore() {
  for (const k of ADMIN_MENU_KEYS) {
    menuForm[k] = adminMenuStore.isVisible(k)
  }
}

const loadMenu = async () => {
  menuLoading.value = true
  try {
    const res: any = await getMenuVisibility()
    if (res.code === 200 && res.data) {
      adminMenuStore.setFromSave(res.data)
    }
    syncMenuFormFromStore()
  } catch {
    ElMessage.error('加载菜单配置失败')
  } finally {
    menuLoading.value = false
  }
}

const submitPassword = async () => {
  const form = pwdFormRef.value
  if (!form) return
  await form.validate(async (ok) => {
    if (!ok) return
    pwdSaving.value = true
    try {
      const res: any = await changeAdminPassword({
        currentPassword: pwdForm.currentPassword,
        newPassword: pwdForm.newPassword
      })
      if (res.code === 200) {
        ElMessage.success('密码已更新，请牢记新密码')
        pwdForm.currentPassword = ''
        pwdForm.newPassword = ''
        pwdForm.confirmPassword = ''
        form.resetFields()
      }
    } catch {
      /* axios 已提示 */
    } finally {
      pwdSaving.value = false
    }
  })
}

const submitMenu = async () => {
  menuSaving.value = true
  try {
    const payload: Record<string, boolean> = {}
    for (const k of ADMIN_MENU_KEYS) {
      payload[k] = !!menuForm[k]
    }
    const res: any = await saveMenuVisibility(payload)
    if (res.code === 200) {
      ElMessage.success('菜单配置已保存')
      if (res.data) adminMenuStore.setFromSave(res.data)
      syncMenuFormFromStore()
    }
  } catch {
    /* */
  } finally {
    menuSaving.value = false
  }
}

onMounted(async () => {
  await loadMenu()
})
</script>

<style scoped>
.admin-system {
  padding: 24px;
  max-width: 640px;
  box-sizing: border-box;
}

.page-head {
  margin-bottom: 24px;
}

.title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.sub {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
  line-height: 1.55;
}

.panel {
  border-radius: 12px;
  border: 1px solid #ebeef5;
}

.panel :deep(.el-card__header) {
  font-weight: 600;
  color: #303133;
}

.mt {
  margin-top: 20px;
}

.pwd-form {
  max-width: 400px;
}

.hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}

.switch-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.switch-label {
  font-size: 14px;
  color: #303133;
}

.actions {
  display: flex;
  gap: 12px;
}
</style>
