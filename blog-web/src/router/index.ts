import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'

import PortalLayout from '../layout/PortalLayout.vue'
import AdminLayout from '../layout/AdminLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: PortalLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'article/:id',
        name: 'ArticleDetail',
        component: () => import('../views/ArticleDetail.vue'),
        meta: { title: '文章详情' }
      },
      {
        path: 'guestbook',
        name: 'Guestbook',
        component: () => import('../views/Guestbook.vue'),
        meta: { title: '留言板' }
      },
      {
        path: 'tools',
        name: 'NavTools',
        component: () => import('../views/FriendLinks.vue'),
        meta: { title: '常用工具' }
      },
      {
        path: 'friend-links',
        redirect: { name: 'NavTools' }
      },
      {
        path: 'author',
        name: 'Author',
        component: () => import('../views/Author.vue'),
        meta: { title: '博主主页' }
      },
      {
        path: 'me',
        name: 'MyCenter',
        component: () => import('../views/MyCenter.vue'),
        meta: { title: '我的空间' }
      },
      {
        path: ':pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('../views/NotFound.vue'),
        meta: { title: '页面不存在' }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录', guestOnly: true }
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('../views/AdminDashboard.vue'),
        meta: { title: '数据概览' }
      },
      {
        path: 'site-settings',
        name: 'AdminSiteSettings',
        component: () => import('../views/SiteSettings.vue'),
        meta: { title: '站点设置' }
      },
      {
        path: 'articles',
        name: 'AdminArticles',
        component: () => import('../views/ArticleList.vue'),
        meta: { title: '文章管理' }
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('../views/CategoryList.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'tags',
        name: 'AdminTags',
        component: () => import('../views/TagManage.vue'),
        meta: { title: '标签管理' }
      },
      {
        path: 'friend-links',
        name: 'AdminFriendLinks',
        component: () => import('../views/FriendLinkManage.vue'),
        meta: { title: '常用链接管理' }
      },
      {
        path: 'system',
        name: 'AdminSystem',
        component: () => import('../views/AdminSystem.vue'),
        meta: { title: '系统管理' }
      },
      {
        path: 'article/edit/:id?',
        name: 'ArticleEdit',
        component: () => import('../views/ArticleEdit.vue'),
        meta: { title: '编辑文章' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  if (to.meta.title) {
    document.title = `${to.meta.title} - 博客系统`
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.guestOnly && authStore.isAuthenticated) {
    next('/admin/dashboard')
    return
  }
  next()
})

export default router
