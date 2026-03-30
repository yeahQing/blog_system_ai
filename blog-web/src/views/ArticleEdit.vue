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
        v-if="editorMountReady"
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
          <el-select
            v-model="article.category"
            placeholder="选择分类"
            value-key="id"
            clearable
            style="width: 100%"
            popper-class="article-edit-select-popper"
          >
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图 URL">
          <el-input v-model="article.cover" placeholder="https://..." type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="article.summary" type="textarea" :rows="5" placeholder="列表页与 SEO 摘要" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="article.pinned" active-text="前台列表优先展示" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="article.tags"
            multiple
            filterable
            placeholder="多选标签；无标签请先到「标签管理」添加"
            value-key="id"
            style="width: 100%"
            popper-class="article-edit-select-popper"
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
import { ref, reactive, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { ElMessage, ElMessageBox } from 'element-plus'
import { saveArticle, getAdminArticleDetail } from '../api/article'
import { listCategories } from '../api/category'
import { listAdminTags } from '../api/tag'

/** md-editor-v3 默认 editorConfig.zIndex 为 20000，需高于它以免代码块等浮层盖住抽屉 */
const DRAWER_Z_INDEX = 40000

const route = useRoute()
const router = useRouter()
const showSettings = ref(false)
const loading = ref(false)
/** 避免 md-editor-v3 在首帧 ref 未就绪时 MutationObserver.observe(null) */
const editorMountReady = ref(false)
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
  pinned: false,
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

watch(
  () => loading.value,
  async (isLoading) => {
    if (isLoading) {
      editorMountReady.value = false
      return
    }
    await nextTick()
    requestAnimationFrame(() => {
      editorMountReady.value = true
    })
  }
)

/** 与 el-option 使用同一批选项对象，避免 value-key 匹配后多选仍不显示 */
function assignTagsFromApi(raw: unknown) {
  if (!Array.isArray(raw) || raw.length === 0) {
    article.tags = []
    return
  }
  const fromList = new Map(allTags.value.map((t) => [t.id, t]))
  article.tags = raw
    .map((t: any) => {
      if (t == null || t.id == null) return null
      return fromList.get(t.id) ?? ({ id: t.id, name: t.name ?? '' } as { id: number; name: string })
    })
    .filter((x): x is { id: number; name: string } => x != null)
}

function assignCategoryFromApi(cat: unknown) {
  if (cat == null || typeof cat !== 'object' || (cat as any).id == null) {
    article.category = null
    return
  }
  const c = cat as { id: number; name: string }
  const hit = categories.value.find((x) => x.id === c.id)
  article.category = hit ?? { id: c.id, name: c.name ?? '' }
}

async function loadArticleForEdit() {
  const id = route.params.id
  if (!id) {
    article.id = null
    article.title = ''
    article.content = ''
    article.summary = ''
    article.cover = ''
    article.isDraft = true
    article.category = null
    article.tags = []
    article.pinned = false
    return
  }
  const res: any = await getAdminArticleDetail(id as string)
  if (res.code === 200 && res.data) {
    const d = res.data
    article.id = d.id
    article.title = d.title ?? ''
    article.content = d.content ?? ''
    article.summary = d.summary ?? ''
    article.cover = d.cover ?? ''
    article.isDraft = d.isDraft !== false
    article.pinned = !!d.pinned
    assignCategoryFromApi(d.category)
    assignTagsFromApi(d.tags)
  } else {
    ElMessage.error(res.message || '获取文章失败')
  }
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
    await loadArticleForEdit()
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
})

watch(
  () => route.params.id,
  async () => {
    loading.value = true
    try {
      await loadArticleForEdit()
    } catch {
      ElMessage.error('加载失败')
    } finally {
      loading.value = false
    }
  }
)

onUnmounted(() => window.removeEventListener('resize', onResize))

const goBack = () => router.push('/admin/articles')

/** 显式构造请求体；用 categoryId / tagIds 与后端 DTO 对齐，避免实体反序列化时 tags 未绑定 */
function buildArticleSavePayload(isDraft: boolean) {
  return {
    id: article.id,
    title: article.title,
    content: article.content,
    summary: article.summary,
    cover: article.cover,
    isDraft,
    pinned: article.pinned,
    categoryId: article.category?.id ?? null,
    tagIds: article.tags.map((t) => t.id)
  }
}

const handleSave = async (isDraft: boolean) => {
  if (!article.title?.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (isDraft && article.id != null && article.isDraft === false) {
    try {
      await ElMessageBox.confirm(
        '保存草稿将把文章下架为草稿，前台将不再展示该文，是否继续？',
        '保存草稿',
        { type: 'warning', confirmButtonText: '继续保存草稿', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }
  article.isDraft = isDraft
  try {
    const res: any = await saveArticle(buildArticleSavePayload(isDraft))
    if (res.code === 200) {
      ElMessage.success(isDraft ? '已保存草稿' : '已发布')
      if (res.data?.id && !article.id) {
        article.id = res.data.id
      }
      router.push('/admin/articles')
    }
  } catch {
    /* 400 等由 axios 拦截器提示 */
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

<!-- 抽屉 z-index 40000，Select 下拉默认较低会被挡住，表现为空白 -->
<style>
.article-edit-select-popper {
  z-index: 40100 !important;
}
</style>
