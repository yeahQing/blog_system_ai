<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside width="240px" class="aside">
      <div class="aside-header" @click="$router.push('/')">
        <el-icon class="logo-icon"><Platform /></el-icon>
        <span class="logo-text">博客管理系统</span>
      </div>
      
      <el-menu
        :key="menuRenderKey"
        :default-active="activeMenu"
        router
        class="admin-menu"
        background-color="#001529"
        text-color="rgba(255, 255, 255, 0.65)"
        active-text-color="#fff"
      >
        <el-menu-item v-if="adminMenu.isVisible('dashboard')" index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item v-if="adminMenu.isVisible('site-settings')" index="/admin/site-settings">
          <el-icon><Setting /></el-icon>
          <span>站点设置</span>
        </el-menu-item>
        <el-menu-item v-if="adminMenu.isVisible('articles')" index="/admin/articles">
          <el-icon><Document /></el-icon>
          <span>文章管理</span>
        </el-menu-item>
        <el-menu-item v-if="adminMenu.isVisible('categories')" index="/admin/categories">
          <el-icon><Collection /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item v-if="adminMenu.isVisible('tags')" index="/admin/tags">
          <el-icon><PriceTag /></el-icon>
          <span>标签管理</span>
        </el-menu-item>
        <el-menu-item v-if="adminMenu.isVisible('friend-links')" index="/admin/friend-links">
          <el-icon><Link /></el-icon>
          <span>常用链接</span>
        </el-menu-item>
        <el-menu-item index="/admin/system">
          <el-icon><Key /></el-icon>
          <span>系统管理</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-footer">
        <div class="beian-row">
          <img src="/beian-logo.png" alt="公安备案" class="beian-icon" />
          <a href="https://beian.mps.gov.cn/#/query/webSearch?code=37011202002747" rel="noreferrer noopener" target="_blank" class="beian-link">
            鲁公网安备37011202002747号
          </a>
        </div>
        <div class="beian-row">
          <a href="https://beian.miit.gov.cn/#/Integrated/index" rel="noreferrer noopener" target="_blank" class="beian-link">
            鲁ICP备2026016667号-1
          </a>
        </div>
      </div>
    </el-aside>

    <el-container class="main-container">
      <!-- 顶部 Header -->
      <el-header class="admin-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">后台管理</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="admin-profile">
              <el-avatar :size="32" :src="authStore.userInfo.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
              <span class="nickname">{{ authStore.userInfo.nickname || authStore.userInfo.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="system" icon="Key">系统管理</el-dropdown-item>
                <el-dropdown-item command="portal" icon="HomeFilled">返回首页</el-dropdown-item>
                <el-dropdown-item command="logout" icon="SwitchButton" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useAdminMenuStore, ADMIN_MENU_KEYS } from '../stores/adminMenu'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const adminMenu = useAdminMenuStore()

onMounted(() => {
  adminMenu.load()
})

const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/admin/article/edit')) return '/admin/articles'
  return p
})

/** 菜单显隐变化时重挂载，避免 el-menu 高亮与项不同步 */
const menuRenderKey = computed(() =>
  ADMIN_MENU_KEYS.map((k) => `${k}:${adminMenu.isVisible(k) ? 1 : 0}`).join('|')
)
const currentPageTitle = computed(() => (route.meta.title as string) || '工作台')

const handleCommand = (command: string) => {
  if (command === 'logout') {
    authStore.logout()
    router.push('/')
  } else if (command === 'portal') {
    router.push('/')
  } else if (command === 'system') {
    router.push('/admin/system')
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  width: 100%;
  max-width: 100%;
  margin: 0;
  padding: 0;
  display: flex;
  overflow-x: clip;
}

.aside {
  background-color: #001529;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  height: 100vh;
}

.aside-header {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  cursor: pointer;
  background-color: #002140;
}
.logo-icon {
  font-size: 24px;
  color: #1890ff;
}
.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
}

.admin-menu {
  border-right: none;
  flex: 1;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background-color: #1890ff !important;
}

.main-container {
  background-color: #f0f2f5;
  flex: 1;
  min-width: 0;
  max-width: 100%;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.admin-header {
  background-color: #fff;
  height: 64px !important;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;
}

.admin-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 4px;
  transition: background-color 0.2s;
}
.admin-profile:hover {
  background-color: #f5f5f5;
}
.nickname {
  font-size: 14px;
  color: #333;
}

.admin-main {
  padding: 0;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
}

.aside-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background-color: #001529;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.beian-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.beian-icon {
  width: 14px;
  height: 14px;
  object-fit: contain;
}

.beian-link {
  color: rgba(255, 255, 255, 0.45);
  text-decoration: none;
  font-size: 11px;
  transition: color 0.2s;
}

.beian-link:hover {
  color: #fff;
}
</style>
