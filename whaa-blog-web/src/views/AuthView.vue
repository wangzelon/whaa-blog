<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postData } from '@/api/client'
import { saveAuth, type AuthUser } from '@/stores/auth'

interface TokenPair extends AuthUser {
  accessToken: string
  refreshToken: string
}

const route = useRoute()
const router = useRouter()
const isRegister = computed(() => route.path === '/register')
const form = reactive({ username: '', password: '', nickname: '' })
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const endpoint = isRegister.value ? '/auth/register' : '/auth/login'
    const user = await postData<TokenPair>(endpoint, form)
    saveAuth(user, user.accessToken, user.refreshToken)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? route.query.redirect
      : '/messages'
    await router.replace(redirect)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '操作失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <RouterLink class="auth-brand" to="/">
      <span class="auth-brand-mark">W</span>
      <span><strong>WhaaBlog</strong><small>个人记录与交流空间</small></span>
    </RouterLink>

    <section class="auth-card">
      <div class="auth-card-mark" aria-hidden="true">{{ isRegister ? '✦' : 'W' }}</div>
      <h1>{{ isRegister ? '创建你的账号' : '欢迎回来' }}</h1>
      <p>{{ isRegister ? '加入这里，和大家分享想法。' : '登录后继续阅读与交流。' }}</p>

      <form class="auth-form" @submit.prevent="submit">
        <label>
          <span>用户名</span>
          <input v-model.trim="form.username" required minlength="3" maxlength="32" autocomplete="username" placeholder="3–32 位字母、数字或符号" />
        </label>
        <label v-if="isRegister">
          <span>昵称 <em>可选</em></span>
          <input v-model.trim="form.nickname" maxlength="64" autocomplete="nickname" placeholder="别人会这样称呼你" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="form.password" required :minlength="isRegister ? 12 : 1" :maxlength="isRegister ? 72 : 128" type="password" :autocomplete="isRegister ? 'new-password' : 'current-password'" :placeholder="isRegister ? '至少 12 位' : '输入你的密码'" />
        </label>
        <p v-if="error" class="auth-error" role="alert">{{ error }}</p>
        <button class="auth-submit" type="submit" :disabled="loading">
          {{ loading ? '请稍候…' : isRegister ? '注册并进入' : '登录' }}
          <span aria-hidden="true">→</span>
        </button>
      </form>

      <div class="auth-switch">
        {{ isRegister ? '已经有账号？' : '还没有账号？' }}
        <RouterLink :to="isRegister ? '/login' : '/register'">{{ isRegister ? '去登录' : '创建账号' }}</RouterLink>
      </div>
      <div class="auth-note">注册账号默认获得普通用户权限。</div>
    </section>

    <footer class="auth-footer">© {{ new Date().getFullYear() }} WhaaBlog · 让每一次记录都有自己的位置。</footer>
  </main>
</template>

<style scoped>
.auth-page { min-height: calc(100vh - 72px); display: grid; place-items: center; padding: 38px 18px 22px; }
.auth-brand { position: absolute; top: 28px; left: max(24px, calc((100% - 930px) / 2)); display: flex; align-items: center; gap: 10px; color: var(--text); }
.auth-brand:hover { color: var(--text); }
.auth-brand-mark { width: 38px; height: 38px; display: grid; place-items: center; border: 1px solid var(--accent-border); border-radius: 12px; color: var(--accent-strong); background: var(--surface); font-family: Georgia, serif; font-weight: 700; }
.auth-brand > span:last-child { display: grid; gap: 2px; }
.auth-brand strong { font-size: 13px; }
.auth-brand small { color: var(--muted); font-size: 10px; }
.auth-card { width: min(100%, 410px); padding: 34px 38px 28px; border: 1px solid var(--border); border-radius: 18px; background: color-mix(in srgb, var(--surface) 94%, transparent); box-shadow: 0 20px 55px rgb(41 74 116 / 8%); }
.auth-card-mark { width: 46px; height: 46px; display: grid; place-items: center; margin-bottom: 22px; border: 1px solid var(--accent-border); border-radius: 15px; color: var(--accent-strong); background: var(--accent-soft); font-family: Georgia, serif; font-size: 18px; }
.auth-card h1 { margin: 0; color: var(--text); font-family: "Songti SC", "Noto Serif SC", Georgia, serif; font-size: 27px; font-weight: 650; }
.auth-card > p { margin: 9px 0 25px; color: var(--muted); font-size: 12px; }
.auth-form { display: grid; gap: 17px; }
.auth-form label { display: grid; gap: 7px; }
.auth-form label > span { color: var(--text); font-size: 11px; font-weight: 650; }
.auth-form em { margin-left: 4px; color: var(--muted); font-size: 10px; font-style: normal; font-weight: 400; }
.auth-form input { width: 100%; height: 43px; padding: 0 12px; border: 1px solid var(--border); border-radius: 10px; outline: none; color: var(--text); background: var(--surface-soft); font-size: 12px; transition: border-color .16s, box-shadow .16s; }
.auth-form input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent) 14%, transparent); }
.auth-form input::placeholder { color: color-mix(in srgb, var(--muted) 70%, transparent); }
.auth-submit { height: 44px; display: flex; align-items: center; justify-content: space-between; margin-top: 3px; padding: 0 14px; border: 0; border-radius: 10px; color: #fff; background: linear-gradient(105deg, #4c8fe8, #3679d4); box-shadow: 0 7px 16px rgb(62 126 214 / 20%); font-size: 12px; font-weight: 650; cursor: pointer; }
.auth-submit:disabled { opacity: .65; cursor: wait; }
.auth-submit span { font-size: 17px; font-weight: 400; }
.auth-error { margin: -5px 0 0; color: #c84b54; font-size: 11px; line-height: 1.6; }
.auth-switch { margin-top: 20px; color: var(--muted); font-size: 11px; text-align: center; }
.auth-switch a { margin-left: 5px; font-weight: 650; }
.auth-note { margin-top: 20px; padding-top: 15px; border-top: 1px solid var(--border-soft); color: var(--muted); font-size: 10px; text-align: center; }
.auth-footer { position: absolute; bottom: 18px; color: var(--muted); font-size: 10px; }
@media (max-width: 520px) { .auth-page { min-height: calc(100vh - 65px); align-content: center; padding: 70px 14px 54px; } .auth-brand { top: 16px; left: 18px; } .auth-card { padding: 28px 23px 24px; border-radius: 15px; } .auth-footer { max-width: calc(100% - 36px); text-align: center; line-height: 1.6; } }
</style>
