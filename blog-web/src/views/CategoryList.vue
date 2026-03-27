<template>
  <div class="category-manage">
    <div class="table-header">
      <div class="left">
        <h2 class="page-title">分类管理</h2>
      </div>
      <div class="right">
        <el-button type="primary" icon="Plus" @click="handleAdd">添加分类</el-button>
      </div>
    </div>

    <el-table :data="categories" v-loading="loading" border stripe class="category-table">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="分类名称" width="200" align="center" />
      <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
      <el-table-column label="创建时间" width="200" align="center">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑分类' : '添加分类'"
      width="500px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入分类描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const categories = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  id: null as number | null,
  name: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const fetchCategories = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/category/list')
    if (res.code === 200) {
      categories.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载分类失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchCategories)

const handleAdd = () => {
  form.id = null
  form.name = ''
  form.description = ''
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  form.id = row.id
  form.name = row.name
  form.description = row.description
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitting.value = true
      try {
        const res: any = await request.post('/category/save', form)
        if (res.code === 200) {
          ElMessage.success('保存成功')
          dialogVisible.value = false
          fetchCategories()
        }
      } catch (error) {
        // 错误已由拦截器处理
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该分类吗？', '警告', { type: 'warning' })
    const res: any = await request.delete(`/category/${id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchCategories()
    }
  } catch (error) {}
}
</script>

<style scoped>
.category-manage {
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
.page-title {
  margin: 0;
  font-size: 20px;
}
</style>
