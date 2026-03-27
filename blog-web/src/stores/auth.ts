import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref({
    username: localStorage.getItem('username') || '',
    nickname: localStorage.getItem('nickname') || '',
    avatar: localStorage.getItem('avatar') || ''
  })

  const isAuthenticated = computed(() => !!token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(newUserInfo: any) {
    userInfo.value = newUserInfo
    localStorage.setItem('username', newUserInfo.username)
    localStorage.setItem('nickname', newUserInfo.nickname)
    localStorage.setItem('avatar', newUserInfo.avatar)
  }

  function logout() {
    token.value = ''
    userInfo.value = {
      username: '',
      nickname: '',
      avatar: ''
    }
    localStorage.clear()
  }

  return {
    token,
    userInfo,
    isAuthenticated,
    setToken,
    setUserInfo,
    logout
  }
})
