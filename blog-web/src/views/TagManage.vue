<template>
  <div class="tag-manage-page">
    <header class="page-header">
      <div class="page-title-block">
        <h1 class="page-title">标签管理</h1>
        <p class="page-desc">维护全站标签库；与文章的关联在编辑文章时的「设置」里完成。</p>
      </div>
      <div class="page-actions">
        <el-button @click="load">
          <el-icon class="btn-icon"><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="$router.push('/admin/articles')">
          <el-icon class="btn-icon"><Document /></el-icon>
          去文章列表
        </el-button>
      </div>
    </header>

    <el-alert type="info" show-icon :closable="false" class="how-alert">
      <template #title>
        <span class="alert-title">标签如何关联到文章？</span>
      </template>
      <ol class="how-steps">
        <li>在本页<strong>新建标签</strong>（或保留已有标签）。</li>
        <li>打开 <strong>文章管理</strong> → 新建或<strong>编辑</strong>一篇文章。</li>
        <li>点击编辑器右上角 <strong>「设置」</strong>，在抽屉中找到 <strong>「标签」</strong>，多选标签。</li>
        <li>点击 <strong>「发布」</strong> 或 <strong>「保存草稿」</strong>，后台会保存文章与标签的多对多关系。</li>
      </ol>
      <p class="how-note">前台首页侧栏「标签云」、文章卡片上的标签、按标签筛选列表，都依赖这里的关联。</p>
    </el-alert>

    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="8">
        <div class="stat-card">
          <span class="stat-label">标签总数</span>
          <span class="stat-value">{{ rows.length }}</span>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="card-head">
          <span class="card-title">新建标签</span>
          <span class="card-sub">名称保存后系统会自动生成 URL 用的 Slug</span>
        </div>
      </template>
      <div class="create-row">
        <el-input
          v-model="form.name"
          placeholder="例如：Vue3、读书笔记"
          clearable
          maxlength="64"
          show-word-limit
          class="name-input"
          @keyup.enter="handleSave"
        />
        <el-button type="primary" :loading="saving" @click="handleSave">添加标签</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="panel-card table-card">
      <template #header>
        <div class="card-head">
          <span class="card-title">标签列表</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="rows" border stripe class="data-table" empty-text="暂无标签，先在上方添加">
        <el-table-column prop="id" label="ID" width="88" align="center" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="slug" label="Slug" min-width="160">
          <template #default="{ row }">
            <code class="slug-code">{{ row.slug }}</code>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listAdminTags, saveTag, deleteTag } from '../api/tag'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Document } from '@element-plus/icons-vue'
const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const form = ref({ name: '' })

const load = async () => {
  loading.value = true
  try {
    const res: any = await listAdminTags()
    if (res.code === 200) {
      rows.value = res.data || []
    }
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  const n = form.value.name?.trim()
  if (!n) {
    ElMessage.warning('请输入名称')
    return
  }
  if (saving.value) return
  saving.value = true
  try {
    const res: any = await saveTag({ name: n })
    if (res.code === 200) {
      ElMessage.success('已保存')
      form.value.name = ''
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
    await ElMessageBox.confirm(
      `确定删除标签「${row.name}」？若文章仍在使用该标签，后台会先解除关联再删除。`,
      '确认删除',
      { type: 'warning' }
    )
    const res: any = await deleteTag(row.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      await load()
    }
  } catch {
    /* cancel */
  }
}

onMounted(load)
</script>

<style scoped>
.tag-manage-page {
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
  letter-spacing: 0.02em;
}

.page-desc {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
  max-width: 520px;
}

.page-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.btn-icon {
  margin-right: 4px;
  vertical-align: middle;
}

.how-alert {
  align-items: flex-start;
}

.alert-title {
  font-weight: 600;
}

.how-steps {
  margin: 8px 0 0;
  padding-left: 1.25rem;
  color: #606266;
  line-height: 1.75;
  font-size: 14px;
}

.how-steps li {
  margin-bottom: 4px;
}

.how-note {
  margin: 12px 0 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
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

.create-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.name-input {
  flex: 1;
  min-width: 220px;
  max-width: 400px;
}

.table-card :deep(.el-card__body) {
  padding: 0 20px 20px;
}

.data-table {
  width: 100%;
}

.slug-code {
  font-size: 12px;
  color: #475569;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}
</style>
