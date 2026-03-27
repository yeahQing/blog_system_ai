<template>
  <router-view />
</template>

<script setup lang="ts">
// 全局入口组件
</script>

<style>
/* 与 style.css 一致：顶栏通栏、主内容由各页自己限制可读宽度 */
#app {
  margin: 0;
  padding: 0;
  width: 100%;
  max-width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  /* 避免 clip/hidden 形成滚动容器，导致侧栏/目录 position:sticky 失效 */
  overflow-x: visible;
  overflow-y: visible;
}
</style>
