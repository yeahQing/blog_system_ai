<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在或已移动</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.not-found {
  min-height: 50vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 48px 16px;
  color: var(--portal-text, #1e293b);
}
.not-found h1 {
  margin: 0;
  font-size: 4rem;
  font-weight: 800;
  color: #3b82f6;
}
.not-found p {
  margin: 0 0 16px;
  color: var(--portal-muted, #64748b);
}
</style>
