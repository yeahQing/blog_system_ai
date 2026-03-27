<template>
  <el-config-provider>
    <div class="portal-layout" :class="{ 'is-dark': isDark }">
      <el-header class="navbar">
        <div class="nav-content">
          <div class="nav-left">
            <div class="logo-area" @click="$router.push('/')">
              <el-image v-if="site.navLogoUrl" :src="site.navLogoUrl" class="logo-img" fit="contain" />
              <el-icon v-else class="logo-icon"><IceTea /></el-icon>
              <span class="logo-text">{{ site.navTitle }}</span>
            </div>

            <nav class="nav-links" aria-label="主导航">
              <el-button link class="nav-link" @click="$router.push('/')">首页</el-button>
              <el-button link class="nav-link" @click="$router.push('/author')">博主</el-button>
              <el-button link class="nav-link" @click="$router.push('/guestbook')">留言</el-button>
              <el-button link class="nav-link" @click="$router.push('/tools')">常用工具</el-button>
              <el-button link class="nav-link" @click="$router.push('/me')">我的</el-button>
            </nav>
          </div>

          <div class="nav-actions">
            <el-button
              :icon="isDark ? Sunny : Moon"
              circle
              aria-label="切换明暗主题"
              @click="toggleDark"
            />
          </div>
        </div>
      </el-header>

      <el-main class="main-container">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>

      <el-footer class="footer">
        <div class="footer-content">
          <p>{{ footerLine }}</p>
        </div>
      </el-footer>

      <el-backtop :right="40" :bottom="40" />
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, provide } from 'vue'
import { Moon, Sunny } from '@element-plus/icons-vue'
import { getSiteConfig } from '../api/siteBranding'

const year = computed(() => new Date().getFullYear())

const defaultFooterTpl = '© {year} AI BLOG · Powered by Vue 3 & Spring Boot'

const site = ref({
  navTitle: 'AI BLOG',
  navLogoUrl: '',
  heroTitle: '探索代码与生活',
  heroSubtitle: '分享技术洞见，记录成长瞬间',
  authorName: '',
  authorBio: '',
  authorAvatar: '',
  footerText: defaultFooterTpl
})

const footerLine = computed(() => {
  const tpl = (site.value.footerText || '').trim() || defaultFooterTpl
  return tpl.replace(/\{year\}/g, String(year.value))
})

const isDark = ref(false)

function applyHtmlClass(dark: boolean) {
  document.documentElement.classList.toggle('dark', dark)
  localStorage.setItem('portal-theme', dark ? 'dark' : 'light')
}

onMounted(async () => {
  const saved = localStorage.getItem('portal-theme')
  if (saved === 'dark' || saved === 'light') {
    isDark.value = saved === 'dark'
  } else {
    isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  applyHtmlClass(isDark.value)
  try {
    const res: any = await getSiteConfig()
    if (res.code === 200 && res.data) {
      site.value = {
        navTitle: res.data.navTitle || site.value.navTitle,
        navLogoUrl: res.data.navLogoUrl || '',
        heroTitle: res.data.heroTitle || site.value.heroTitle,
        heroSubtitle: res.data.heroSubtitle || site.value.heroSubtitle,
        authorName: res.data.authorName || '',
        authorBio: res.data.authorBio || '',
        authorAvatar: res.data.authorAvatar || '',
        footerText: res.data.footerText || site.value.footerText
      }
    }
  } catch {
    /* 使用默认 */
  }
})

function toggleDark() {
  isDark.value = !isDark.value
  applyHtmlClass(isDark.value)
}

provide('portalIsDark', isDark)
provide('portalSite', site)
</script>

<style scoped>
.portal-layout {
  /* md-editor-v3 预览/代码块浮层默认 z-index 约 20000，顶栏需更高以免被遮挡 */
  --portal-navbar-z: 25000;
  /* 顶栏 64px + 与正文间距，供侧栏/目录 position:sticky */
  --portal-sticky-offset: 76px;

  min-height: 100vh;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background-color: var(--portal-bg, #f8fafc);
  color: var(--portal-text, #1e293b);
  /* 不要用 overflow:clip/hidden，否则会形成滚动容器并破坏子页面 position:sticky（相对视口吸顶） */
  overflow-x: visible;
  overflow-y: visible;
  transition: background-color 0.25s ease, color 0.2s ease;
}

.portal-layout.is-dark {
  --portal-bg: #0f172a;
  --portal-text: #e2e8f0;
  --portal-muted: #94a3b8;
  --portal-border: #334155;
  --portal-card: #1e293b;
}

.navbar {
  background-color: color-mix(in srgb, var(--portal-card, #fff) 88%, transparent);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--portal-border, #e2e8f0);
  position: sticky;
  top: 0;
  z-index: var(--portal-navbar-z, 25000);
  height: 64px !important;
  display: flex;
  align-items: center;
}

.nav-content {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  display: flex;
  align-items: center;
  padding: 0 clamp(16px, 4vw, 40px);
  gap: 16px;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: clamp(12px, 2vw, 28px);
  min-width: 0;
  flex: 1;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: opacity 0.2s;
  flex-shrink: 0;
}
.logo-area:hover {
  opacity: 0.8;
}
.logo-img {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  flex-shrink: 0;
}

.logo-icon {
  font-size: 28px;
  color: #3b82f6;
}
.logo-text {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
  color: var(--portal-text, #1e293b);
}

.nav-links {
  display: flex;
  gap: 2px;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: center;
  min-width: 0;
}

.nav-link {
  font-weight: 500 !important;
  color: var(--portal-muted, #64748b) !important;
}

.nav-actions {
  flex-shrink: 0;
  margin-left: auto;
}

/* Element Plus 默认 .el-main { overflow: auto }，滚动发生在 main 内，sticky 无法相对顶栏吸顶 */
.main-container.el-main {
  flex: 1 0 auto;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  padding: 0 !important;
  overflow: visible !important;
}

.footer-content {
  width: 100%;
  max-width: 100%;
  padding: 0 clamp(16px, 4vw, 40px);
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100%;
}

.footer-content p {
  margin: 0;
  line-height: 1.5;
}

/* 覆盖 el-footer 默认固定高度，使文案在页脚条带内垂直居中 */
.footer {
  --el-footer-height: auto;
  height: auto !important;
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  padding: 20px 0;
  box-sizing: border-box;
  background-color: var(--portal-card, #fff);
  border-top: 1px solid var(--portal-border, #e2e8f0);
  color: var(--portal-muted, #94a3b8);
  text-align: center;
  font-size: 14px;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-15px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(15px);
}

@media (max-width: 640px) {
  .nav-left {
    flex-wrap: wrap;
    row-gap: 6px;
  }
  .nav-links {
    flex-basis: 100%;
    overflow-x: auto;
    flex-wrap: nowrap;
    padding-bottom: 2px;
    -webkit-overflow-scrolling: touch;
    mask-image: linear-gradient(to right, transparent, #000 10px, #000 calc(100% - 10px), transparent);
  }
}
</style>
