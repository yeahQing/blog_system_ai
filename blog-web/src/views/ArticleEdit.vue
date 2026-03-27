<template>
  <div class="editor-page" v-loading="loading">
    <header class="editor-toolbar">
      <div class="toolbar-left">
        <el-button text bg @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
        <el-divider direction="vertical" class="toolbar-divider" />
        <el-input
          v-model="article.title"
          placeholder="在此输入文章标题"
          class="title-input"
          maxlength="200"
          show-word-limit
        />
      </div>
      <div class="toolbar-right">
        <el-button @click="showSettings = true">
          <el-icon><Setting /></el-icon>
          设置
        </el-button>
        <el-button @click="handleSave(true)">保存草稿</el-button>
        <el-button type="primary" @click="handleSave(false)">发布</el-button>
      </div>
    </header>

    <div class="editor-surface">
      <MdEditor
        v-if="!loading"
        v-model="article.content"
        :toolbars="toolbars"
        :height="editorHeight"
        placeholder="正文支持 Markdown…"
        @onUploadImg="onUploadImg"
        class="md-wrap"
      />
    </div>

    <el-drawer
      v-model="showSettings"
      title="文章设置"
      size="400px"
      direction="rtl"
      append-to-body
      :z-index="DRAWER_Z_INDEX"
    >
      <el-form :model="article" label-position="top" class="settings-form">
        <el-form-item label="分类">
          <el-select v-model="article.category" placeholder="选择分类" value-key="id" clearable style="width: 100%">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图 URL">
          <el-input v-model="article.cover" placeholder="https://..." type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="article.summary" type="textarea" :rows="5" placeholder="列表页与 SEO 摘要" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="article.tags"
            multiple
            filterable
            placeholder="多选标签；无标签请先到「标签管理」添加"
            value-key="id"
            style="width: 100%"
          >
            <el-option v-for="t in allTags" :key="t.id" :label="t.name" :value="t" />
          </el-select>
          <p class="tag-field-hint">
            标签在 <strong>后台 → 标签管理</strong> 中维护；此处勾选后点击「发布 / 保存草稿」即与当前文章建立关联（多对多）。前台列表、标签云、按标签筛选均依赖此关联。
          </p>
          <el-button type="primary" link class="tag-link" @click="$router.push('/admin/tags')">打开标签管理</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="showSettings = false">完成</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { ElMessage } from 'element-plus'
import { saveArticle, getAdminArticleDetail } from '../api/article'
import { listCategories } from '../api/category'
import { listAdminTags } from '../api/tag'

/** md-editor-v3 默认 editorConfig.zIndex 为 20000，需高于它以免代码块等浮层盖住抽屉 */
const DRAWER_Z_INDEX = 40000

const route = useRoute()
const router = useRouter()
const showSettings = ref(false)
const loading = ref(false)
const categories = ref<{ id: number; name: string }[]>([])
const allTags = ref<{ id: number; name: string }[]>([])

const viewportH = ref(typeof window !== 'undefined' ? window.innerHeight : 800)
const editorHeight = computed(() => `${Math.max(380, viewportH.value - 200)}px`)

const article = reactive({
  id: null as number | null,
  title: '',
  content: '',
  summary: '',
  cover: '',
  isDraft: true,
  category: null as { id: number; name: string } | null,
  tags: [] as { id: number; name: string }[]
})

const toolbars: ToolbarNames[] = [
  'bold',
  'underline',
  'italic',
  '-',
  'title',
  'strikeThrough',
  'sub',
  'sup',
  'quote',
  'unorderedList',
  'orderedList',
  'task',
  '-',
  'codeRow',
  'code',
  'link',
  'image',
  'table',
  'mermaid',
  'katex',
  '-',
  'revoke',
  'next',
  'save',
  '=',
  'pageFullscreen',
  'fullscreen',
  'preview',
  'htmlPreview',
  'catalog'
]

function onResize() {
  viewportH.value = window.innerHeight
}

onMounted(async () => {
  window.addEventListener('resize', onResize)
  loading.value = true
  try {
    const [catRes, tagRes]: any[] = await Promise.all([listCategories(), listAdminTags()])
    if (catRes.code === 200) {
      categories.value = catRes.data || []
    }
    if (tagRes.code === 200) {
      allTags.value = tagRes.data || []
    }

    const id = route.params.id
    if (id) {
      const res: any = await getAdminArticleDetail(id as string)
      if (res.code === 200 && res.data) {
        article.id = res.data.id
        article.title = res.data.title ?? ''
        article.content = res.data.content ?? ''
        article.summary = res.data.summary ?? ''
        article.cover = res.data.cover ?? ''
        article.isDraft = res.data.isDraft !== false
        article.category = res.data.category ?? null
        const rawTags = res.data.tags
        article.tags = Array.isArray(rawTags) ? rawTags.map((t: any) => ({ id: t.id, name: t.name })) : []
      } else {
        ElMessage.error(res.message || '获取文章失败')
      }
    }
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
})

onUnmounted(() => window.removeEventListener('resize', onResize))

const goBack = () => router.push('/admin/articles')

const handleSave = async (isDraft: boolean) => {
  if (!article.title?.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  article.isDraft = isDraft
  try {
    const res: any = await saveArticle({ ...article })
    if (res.code === 200) {
      ElMessage.success(isDraft ? '已保存草稿' : '已发布')
      if (res.data?.id && !article.id) {
        article.id = res.data.id
      }
      router.push('/admin/articles')
    }
  } catch {
    ElMessage.error('保存失败')
  }
}

const onUploadImg = async (_files: File[], _callback: (urls: string[]) => void) => {
  ElMessage.info('图片上传请使用外链 URL 或后续接入对象存储')
}
</script>

<style scoped>
.editor-page {
  height: 100%;
  min-height: calc(100vh - 128px);
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px 24px 24px;
  box-sizing: border-box;
  background: linear-gradient(180deg, #eef2f7 0%, #f8fafc 40%);
}

.editor-toolbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.06);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.toolbar-divider {
  height: 28px;
  margin: 0;
}

.title-input {
  flex: 1;
  min-width: 0;
}

.title-input :deep(.el-input__wrapper) {
  box-shadow: none;
  background: #f8fafc;
  border-radius: 8px;
}

.title-input :deep(.el-input__inner) {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.toolbar-right {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.editor-surface {
  flex: 1;
  min-height: 380px;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: #fff;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.07);
}

.md-wrap {
  flex: 1;
  min-height: 0;
}

.md-wrap :deep(.md-editor) {
  border: none !important;
  border-radius: 0 !important;
}

.settings-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.tag-field-hint {
  margin: 8px 0 4px;
  font-size: 12px;
  line-height: 1.55;
  color: #909399;
}

.tag-link {
  padding: 0;
  height: auto;
  font-size: 13px;
}

@media (max-width: 768px) {
  .editor-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-right {
    justify-content: flex-end;
  }
}
</style>
