<template>
  <div class="article-manage">
    <div class="table-header">
      <div class="left">
        <el-input
          v-model="keyword"
          placeholder="搜索文章标题..."
          class="search-input"
          clearable
          @clear="fetchArticles"
          @keyup.enter="fetchArticles"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="fetchArticles">查询</el-button>
      </div>
      <div class="right">
        <el-button type="primary" icon="Plus" @click="$router.push('/admin/article/edit')">新建文章</el-button>
      </div>
    </div>

    <el-table :data="articles" v-loading="loading" border stripe class="article-table">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column label="封面" width="120" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.cover"
            :src="row.cover"
            fit="cover"
            class="table-cover"
            :preview-src-list="[row.cover]"
            preview-teleported
          />
          <span v-else class="no-cover">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="分类" width="120" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.category" size="small">{{ row.category.name }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isDraft ? 'info' : 'success'" size="small">
            {{ row.isDraft ? '草稿' : '已发布' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="views" label="阅读量" width="100" align="center" sortable />
      <el-table-column label="创建时间" width="180" align="center">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="$router.push(`/admin/article/edit/${row.id}`)">编辑</el-button>
          <el-button
            v-if="row.isDraft"
            link
            type="success"
            :loading="actionId === row.id"
            @click="handlePublish(row)"
          >
            发布
          </el-button>
          <el-button
            v-else
            link
            type="warning"
            :loading="actionId === row.id"
            @click="handleUnpublish(row)"
          >
            下架
          </el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="fetchArticles"
        @current-change="fetchArticles"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  getAdminArticles,
  deleteArticle,
  publishArticleAdmin,
  unpublishArticleAdmin
} from '../api/article'
import { ElMessage, ElMessageBox } from 'element-plus'

const articles = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
/** 发布/下架请求中的行 id，用于按钮 loading */
const actionId = ref<number | null>(null)

const fetchArticles = async () => {
  loading.value = true
  try {
    const res: any = await getAdminArticles({ 
      page: page.value - 1, 
      size: size.value,
      keyword: keyword.value 
    })
    if (res.code === 200) {
      articles.value = res.data.content
      total.value = res.data.totalElements
    }
  } catch (error) {
    ElMessage.error('加载文章列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchArticles)

const handlePublish = async (row: { id: number; title?: string }) => {
  actionId.value = row.id
  try {
    const res: any = await publishArticleAdmin(row.id)
    if (res?.code === 200) {
      ElMessage.success('已发布，前台可见')
      await fetchArticles()
    }
  } catch {
    /* axios 已提示 */
  } finally {
    actionId.value = null
  }
}

const handleUnpublish = async (row: { id: number; title?: string }) => {
  try {
    await ElMessageBox.confirm(
      `确定将「${row.title || '该文章'}」下架为草稿？前台将不再展示，可随时在列表中再次「发布」。`,
      '下架确认',
      { type: 'warning', confirmButtonText: '下架', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  actionId.value = row.id
  try {
    const res: any = await unpublishArticleAdmin(row.id)
    if (res?.code === 200) {
      ElMessage.success('已改为草稿')
      await fetchArticles()
    }
  } catch {
    /* */
  } finally {
    actionId.value = null
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      '将执行逻辑删除：文章会从本列表中消失，前台也不再展示。确定继续？',
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }
  try {
    const res: any = await deleteArticle(id)
    if (res?.code === 200) {
      ElMessage.success('已删除')
      await fetchArticles()
    } else {
      ElMessage.error(res?.message || '删除失败')
    }
  } catch {
    /* 401/403/404/500 等由 axios 拦截器提示 */
  }
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString()
}
</script>

<style scoped>
.article-manage {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px;
  background-color: #fff;
  min-height: 100%;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.left {
  display: flex;
  gap: 12px;
}

.search-input {
  width: 240px;
}

.article-table {
  width: 100%;
}

.table-cover {
  width: 80px;
  height: 50px;
  border-radius: 4px;
}

.no-cover {
  color: #909399;
  font-size: 12px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
}
</style>
