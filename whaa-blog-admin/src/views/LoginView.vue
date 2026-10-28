<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, EditPen, Lock, User } from '@element-plus/icons-vue'
import { apiPost } from '@/api/client'

const router = useRouter()
const form = ref({ username: '', password: '' })
const loading = ref(false)

async function login() {
  loading.value = true
  try {
    const data = await apiPost<{ accessToken: string }>('/auth/login', form.value)
    localStorage.setItem('whaa_token', data.accessToken)
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-shell">
    <section class="login-story">
      <div class="login-brand">
        <span class="login-brand-mark"><el-icon><EditPen /></el-icon></span>
        <span>WhaaBlog</span>
      </div>

      <div class="story-content">
        <div class="story-illustration" aria-hidden="true">
          <div class="story-sun"></div>
          <div class="story-ring ring-large"></div>
          <div class="story-ring ring-small"></div>
          <div class="story-wave wave-back"></div>
          <div class="story-wave wave-front"></div>
          <div class="story-page">
            <span></span><span></span><span></span>
          </div>
          <div class="story-shine shine-one"></div>
          <div class="story-shine shine-two"></div>
        </div>
        <h1>把日常写下来，<br />让想法有迹可循。</h1>
        <p>在一个安静的地方，整理文章、页面和读者的来信。</p>
      </div>

      <div class="story-footer">
        <span>个人博客管理工作台</span>
        <span class="story-footer-dot"></span>
        <span>WhaaBlog</span>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <span class="login-symbol"><el-icon><EditPen /></el-icon></span>
        <h2>欢迎回来</h2>
        <p class="login-intro">登录管理你的博客内容。</p>

        <el-form class="login-form" @submit.prevent="login">
          <el-form-item label="用户名">
            <el-input v-model="form.username" size="large" :prefix-icon="User" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              :prefix-icon="Lock"
              autocomplete="current-password"
              @keyup.enter="login"
            />
          </el-form-item>
          <el-button class="login-submit" type="primary" size="large" :loading="loading" @click="login">
            <span>登录管理台</span>
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <div class="login-footnote"><el-icon><Lock /></el-icon><span>仅限授权管理员使用</span></div>
      </div>
      <div class="login-panel-footer">© WhaaBlog · Personal publishing desk</div>
    </section>
  </main>
</template>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(380px, 0.95fr) minmax(460px, 1.05fr);
  background: #fff;
}

.login-story {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 34px clamp(32px, 5vw, 74px) 27px;
  color: #edf5ff;
  background:
    radial-gradient(circle at 84% 14%, rgba(95, 154, 220, 0.25), transparent 24%),
    linear-gradient(152deg, #10243a 0%, #153554 62%, #1c4771 100%);
}

.login-brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 11px;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.login-brand-mark {
  width: 37px;
  height: 37px;
  display: grid;
  place-items: center;
  color: #e9f4ff;
  font-size: 17px;
  border: 1px solid rgba(209, 231, 254, 0.25);
  border-radius: 12px;
  background: rgba(187, 216, 247, 0.12);
}

.story-content {
  position: relative;
  z-index: 1;
  max-width: 500px;
  margin: auto 0;
  padding: 54px 0 72px;
}

.story-content h1 {
  margin: 0;
  color: #fff;
  font-size: clamp(34px, 4.2vw, 52px);
  font-weight: 700;
  letter-spacing: -0.055em;
  line-height: 1.24;
}

.story-content p {
  max-width: 360px;
  margin: 18px 0 0;
  color: #b7cbe0;
  font-size: 14px;
  line-height: 1.9;
}

.story-illustration {
  position: relative;
  width: min(100%, 370px);
  height: 220px;
  margin: 0 0 35px 7px;
}

.story-sun {
  position: absolute;
  top: 20px;
  left: 118px;
  width: 112px;
  height: 112px;
  border-radius: 50%;
  background: linear-gradient(150deg, rgba(146, 192, 238, 0.5), rgba(98, 155, 216, 0.1));
}

.story-ring {
  position: absolute;
  border: 1px solid rgba(207, 230, 255, 0.23);
  border-radius: 50%;
}

.ring-large { width: 222px; height: 222px; top: -34px; left: 62px; }
.ring-small { width: 170px; height: 170px; top: -8px; left: 88px; }

.story-wave {
  position: absolute;
  left: 0;
  width: 310px;
  height: 92px;
  border: 1px solid rgba(207, 230, 255, 0.2);
  border-radius: 50%;
  transform: rotate(-9deg);
}

.wave-back { top: 102px; background: rgba(126, 172, 218, 0.08); }
.wave-front { top: 126px; left: 30px; background: linear-gradient(90deg, rgba(171, 209, 246, 0.18), rgba(86, 140, 199, 0.06)); }

.story-page {
  position: absolute;
  top: 41px;
  left: 154px;
  width: 78px;
  height: 99px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 23px 12px;
  border: 1px solid rgba(232, 243, 255, 0.55);
  border-radius: 8px;
  background: linear-gradient(145deg, rgba(246, 251, 255, 0.22), rgba(200, 225, 249, 0.09));
  box-shadow: 0 12px 32px rgba(4, 19, 36, 0.2);
  transform: rotate(7deg);
}

.story-page span { height: 3px; border-radius: 3px; background: rgba(225, 240, 255, 0.72); }
.story-page span:first-child { width: 65%; }
.story-page span:last-child { width: 76%; }

.story-shine {
  position: absolute;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #c9e3ff;
  box-shadow: 0 0 16px rgba(201, 227, 255, 0.85);
}

.shine-one { top: 39px; left: 268px; }
.shine-two { top: 155px; left: 83px; width: 5px; height: 5px; }

.story-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #90aac4;
  font-size: 11px;
}

.story-footer-dot { width: 3px; height: 3px; border-radius: 50%; background: #7896b4; }

.login-panel {
  min-height: 100vh;
  position: relative;
  display: grid;
  place-items: center;
  padding: 42px 26px 66px;
  background: #fff;
}

.login-card {
  width: min(100%, 380px);
}

.login-symbol {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  margin-bottom: 23px;
  color: #367ce0;
  font-size: 20px;
  border: 1px solid #deebfb;
  border-radius: 15px;
  background: #f2f7ff;
}

.login-card h2 {
  margin: 0;
  color: #17253b;
  font-size: 27px;
  font-weight: 700;
  letter-spacing: -0.045em;
}

.login-intro {
  margin: 9px 0 28px;
  color: #778498;
  font-size: 13px;
}

.login-form :deep(.el-form-item) { margin-bottom: 20px; }
.login-form :deep(.el-form-item__label) {
  margin-bottom: 7px;
  color: #3b4b61;
  font-size: 12px;
  font-weight: 650;
}
.login-form :deep(.el-input__wrapper) {
  min-height: 46px;
  padding: 0 13px;
  border-radius: 10px;
  background: #fcfdff;
}
.login-form :deep(.el-input__prefix) { color: #9aa8b9; }
.login-form :deep(.el-input__inner) { color: #27384e; font-size: 13px; }

.login-submit {
  width: 100%;
  height: 46px;
  justify-content: space-between;
  margin-top: 4px;
  padding: 0 15px;
  border-radius: 10px;
}

.login-footnote {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  margin-top: 22px;
  color: #9aa5b3;
  font-size: 11px;
}

.login-footnote .el-icon { color: #8195ad; }

.login-panel-footer {
  position: absolute;
  right: 26px;
  bottom: 23px;
  left: 26px;
  color: #a2acb9;
  font-size: 10px;
  text-align: center;
}

@media (max-width: 820px) {
  .login-shell { grid-template-columns: minmax(0, 0.85fr) minmax(420px, 1.15fr); }
  .login-story { padding-right: 35px; padding-left: 35px; }
  .story-illustration { transform: scale(0.84); transform-origin: left center; margin-bottom: 12px; }
  .story-content h1 { font-size: 37px; }
}

@media (max-width: 680px) {
  .login-shell { display: block; }
  .login-story { min-height: 260px; padding: 23px 24px 25px; }
  .story-content { margin: 24px 0 0; padding: 0; }
  .story-illustration { position: absolute; right: -120px; top: 45px; width: 280px; height: 190px; margin: 0; opacity: 0.34; transform: scale(0.78); }
  .story-content h1 { position: relative; max-width: 350px; font-size: 30px; }
  .story-content p { max-width: 310px; margin-top: 9px; font-size: 12px; }
  .story-footer { display: none; }
  .login-panel { min-height: calc(100vh - 260px); padding: 40px 24px 72px; }
  .login-card { max-width: 420px; }
}
</style>
