<template>
  <div class="site-settings" v-loading="loading">
    <header class="page-head">
      <h1 class="title">站点设置</h1>
      <p class="sub">控制前台导航栏、首页横幅、页脚文案，以及首页侧栏与博主页的个人信息。留空并保存则恢复为配置文件默认值。</p>
    </header>

    <el-form :model="form" label-position="top" class="form-card" @submit.prevent>
      <el-card shadow="never">
        <template #header>
          <span class="card-title">导航栏（左侧）</span>
        </template>
        <el-form-item label="站点标题（文字）">
          <el-input v-model="form.navTitle" placeholder="例如：AI BLOG" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="Logo 图片 URL（选填）">
          <el-input
            v-model="form.navLogoUrl"
            type="textarea"
            :rows="2"
            placeholder="填写则显示图片；留空则前台使用默认图标"
          />
        </el-form-item>
      </el-card>

      <el-card shadow="never" class="mt">
        <template #header>
          <span class="card-title">首页横幅</span>
        </template>
        <el-form-item label="主标题">
          <el-input v-model="form.heroTitle" placeholder="例如：探索代码与生活" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input
            v-model="form.heroSubtitle"
            type="textarea"
            :rows="2"
            placeholder="例如：分享技术洞见，记录成长瞬间"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-card>

      <el-card shadow="never" class="mt">
        <template #header>
          <span class="card-title">页脚</span>
        </template>
        <el-form-item label="页脚文案">
          <el-input
            v-model="form.footerText"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="例如：© {year} 我的博客 · 保留所有权利"
          />
        </el-form-item>
        <p class="field-hint">使用 <code>{year}</code> 表示当前年份（如 2026）。保存后刷新前台生效。</p>
      </el-card>

      <el-card shadow="never" class="mt">
        <template #header>
          <span class="card-title">博主展示（首页侧栏 / 博主页）</span>
        </template>
        <el-form-item label="显示名称">
          <el-input v-model="form.authorName" placeholder="昵称或站点名" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="一句话介绍">
          <el-input v-model="form.authorBio" type="textarea" :rows="3" placeholder="个人简介" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="头像 URL">
          <el-input v-model="form.authorAvatar" type="textarea" :rows="2" placeholder="https://..." />
        </el-form-item>
        <div v-if="form.authorAvatar" class="avatar-preview">
          <span class="preview-label">预览</span>
          <el-avatar :size="72" :src="form.authorAvatar" />
        </div>
      </el-card>

      <div class="actions">
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
        <el-button @click="load">重新加载</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getSiteBrandingAdmin, saveSiteBranding, type SiteConfig } from '../api/siteBranding'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const form = reactive<SiteConfig>({
  navTitle: '',
  navLogoUrl: '',
  heroTitle: '',
  heroSubtitle: '',
  authorName: '',
  authorBio: '',
  authorAvatar: '',
  footerText: ''
})

const load = async () => {
  loading.value = true
  try {
    const res: any = await getSiteBrandingAdmin()
    if (res.code === 200 && res.data) {
      Object.assign(form, res.data)
    }
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  saving.value = true
  try {
    const res: any = await saveSiteBranding({
      navTitle: form.navTitle,
      navLogoUrl: form.navLogoUrl,
      heroTitle: form.heroTitle,
      heroSubtitle: form.heroSubtitle,
      authorName: form.authorName,
      authorBio: form.authorBio,
      authorAvatar: form.authorAvatar,
      footerText: form.footerText
    })
    if (res.code === 200) {
      ElMessage.success('已保存，刷新前台即可看到效果')
      if (res.data) Object.assign(form, res.data)
    }
  } catch {
    /* axios 提示 */
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.site-settings {
  padding: 24px;
  max-width: 720px;
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

.form-card :deep(.el-card) {
  border-radius: 12px;
  border: 1px solid #ebeef5;
}

.card-title {
  font-weight: 600;
  color: #303133;
}

.mt {
  margin-top: 20px;
}

.avatar-preview {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
}

.preview-label {
  font-size: 13px;
  color: #909399;
}

.actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

.field-hint {
  margin: -4px 0 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}

.field-hint code {
  padding: 1px 6px;
  border-radius: 4px;
  background: #f4f4f5;
  font-size: 12px;
}
</style>
