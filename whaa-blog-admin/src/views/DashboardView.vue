<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet } from '@/api/client'
import {
  ArrowRight,
  ChatDotRound,
  CollectionTag,
  Document,
  EditPen,
  Files,
  Link,
  PriceTag,
  Setting,
  TrendCharts,
} from '@element-plus/icons-vue'

interface DashboardStats {
  postCount?: number
  draftCount?: number
  pendingComments?: number
  pv7d?: number
}

const router = useRouter()
const stats = ref<DashboardStats>({})
const loading = ref(true)
const loadError = ref(false)

const metrics = computed(() => [
  { label: '已发布文章', value: stats.value.postCount || 0, note: '当前公开内容', icon: Document, tone: 'blue' },
  { label: '草稿', value: stats.value.draftCount || 0, note: '等待继续编辑', icon: EditPen, tone: 'violet' },
  { label: '待审评论', value: stats.value.pendingComments || 0, note: '需要处理的评论', icon: ChatDotRound, tone: 'amber' },
  { label: '近 7 日访问', value: stats.value.pv7d || 0, note: '页面浏览估算', icon: TrendCharts, tone: 'green' },
])

const contentLinks = [
  { label: '文章管理', description: '维护文章、草稿与发布状态', path: '/posts', icon: Document },
  { label: '页面管理', description: '更新关于页和自定义页面', path: '/pages', icon: Files },
  { label: '评论审核', description: '查看读者留言并完成审核', path: '/comments', icon: ChatDotRound },
]

const siteLinks = [
  { label: '分类', path: '/categories', icon: CollectionTag },
  { label: '标签', path: '/tags', icon: PriceTag },
  { label: '友链', path: '/links', icon: Link },
  { label: '站点配置', path: '/settings', icon: Setting },
]

async function load() {
  loading.value = true
  loadError.value = false
  try {
    stats.value = await apiGet<DashboardStats>('/admin/dashboard')
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="dashboard-page">
    <section class="dashboard-welcome">
      <div class="welcome-copy">
        <div class="welcome-mark"><el-icon><EditPen /></el-icon></div>
        <h2>让想法，慢慢长成内容。</h2>
        <p>从这里继续写作、整理页面，或看看读者留下了什么。</p>
        <div class="welcome-actions">
          <el-button type="primary" @click="router.push('/posts/edit')">
            <el-icon><EditPen /></el-icon>
            写一篇文章
          </el-button>
          <el-button class="welcome-secondary" @click="router.push('/comments')">查看评论</el-button>
        </div>
      </div>
      <div class="welcome-art" aria-hidden="true">
        <div class="art-orbit orbit-one"></div>
        <div class="art-orbit orbit-two"></div>
        <div class="art-paper paper-back"></div>
        <div class="art-paper paper-front">
          <span class="paper-line line-short"></span>
          <span class="paper-line"></span>
          <span class="paper-line line-mid"></span>
          <span class="paper-line"></span>
          <span class="paper-line line-short"></span>
        </div>
        <span class="art-star star-one">✦</span>
        <span class="art-star star-two">✦</span>
      </div>
    </section>

    <div v-if="loadError" class="dashboard-error">
      <span>暂时无法读取仪表盘数据。</span>
      <el-button link type="primary" @click="load">重新加载</el-button>
    </div>

    <section class="metrics-grid" aria-label="内容统计">
      <el-card v-for="metric in metrics" :key="metric.label" class="metric-card" shadow="never">
        <div class="metric-topline">
          <span class="metric-icon" :class="'tone-' + metric.tone"><el-icon><component :is="metric.icon" /></el-icon></span>
          <span class="metric-context">{{ metric.note }}</span>
        </div>
        <el-skeleton :loading="loading" animated>
          <template #template>
            <el-skeleton-item variant="h3" class="metric-skeleton" />
          </template>
          <template #default>
            <el-statistic :value="metric.value" group-separator="," />
          </template>
        </el-skeleton>
        <div class="metric-label">{{ metric.label }}</div>
      </el-card>
    </section>

    <section class="dashboard-lower">
      <el-card class="dashboard-panel content-panel" shadow="never">
        <div class="panel-heading">
          <div>
            <h3>内容管理</h3>
            <p>常用内容入口</p>
          </div>
          <span class="panel-heading-icon"><el-icon><Document /></el-icon></span>
        </div>
        <button
          v-for="item in contentLinks"
          :key="item.path"
          type="button"
          class="dashboard-link-row"
          @click="router.push(item.path)"
        >
          <span class="link-row-icon"><el-icon><component :is="item.icon" /></el-icon></span>
          <span class="link-row-copy"><strong>{{ item.label }}</strong><small>{{ item.description }}</small></span>
          <el-icon class="link-row-arrow"><ArrowRight /></el-icon>
        </button>
      </el-card>

      <el-card class="dashboard-panel site-panel" shadow="never">
        <div class="panel-heading">
          <div>
            <h3>站点维护</h3>
            <p>整理博客的结构与展示</p>
          </div>
          <span class="panel-heading-icon"><el-icon><Setting /></el-icon></span>
        </div>
        <div class="site-link-grid">
          <button
            v-for="item in siteLinks"
            :key="item.path"
            type="button"
            class="site-link-tile"
            @click="router.push(item.path)"
          >
            <span class="site-tile-icon"><el-icon><component :is="item.icon" /></el-icon></span>
            <span>{{ item.label }}</span>
            <el-icon class="site-tile-arrow"><ArrowRight /></el-icon>
          </button>
        </div>
        <div class="site-panel-foot"><el-icon><TrendCharts /></el-icon><span>浏览量统计区间：近 7 日</span></div>
      </el-card>
    </section>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.dashboard-welcome {
  position: relative;
  min-height: 212px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  overflow: hidden;
  padding: 28px 34px;
  color: #fff;
  border-radius: 18px;
  background:
    radial-gradient(circle at 82% 5%, rgba(92, 151, 232, 0.26), transparent 28%),
    linear-gradient(115deg, #152b45 0%, #193a5e 55%, #225186 100%);
  box-shadow: 0 14px 30px rgba(26, 58, 95, 0.13);
}

.welcome-copy {
  position: relative;
  z-index: 1;
  max-width: 530px;
}

.welcome-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  margin-bottom: 13px;
  color: #b9d9ff;
  font-size: 18px;
  border: 1px solid rgba(190, 220, 255, 0.25);
  border-radius: 11px;
  background: rgba(166, 204, 247, 0.13);
}

.welcome-copy h2 {
  margin: 0;
  color: #fff;
  font-size: clamp(22px, 2.2vw, 28px);
  font-weight: 700;
  letter-spacing: -0.04em;
}

.welcome-copy p {
  margin: 9px 0 18px;
  color: #c1d3e8;
  font-size: 13px;
  line-height: 1.65;
}

.welcome-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
}

.welcome-actions .el-button--primary {
  color: #1a4b83;
  border: 0;
  background: #e6f1ff;
  box-shadow: none;
}

.welcome-actions .el-button--primary:hover {
  color: #133c6b;
  background: #fff;
}

.welcome-secondary {
  color: #e5f0fc;
  border-color: rgba(205, 225, 246, 0.35);
  background: rgba(255, 255, 255, 0.06);
}

.welcome-secondary:hover {
  color: #fff;
  border-color: rgba(222, 237, 252, 0.62);
  background: rgba(255, 255, 255, 0.12);
}

.welcome-art {
  position: absolute;
  top: 0;
  right: 20px;
  width: 340px;
  height: 100%;
  opacity: 0.92;
}

.art-orbit {
  position: absolute;
  border: 1px solid rgba(199, 223, 251, 0.16);
  border-radius: 50%;
}

.orbit-one {
  width: 275px;
  height: 275px;
  top: -56px;
  right: 3px;
}

.orbit-two {
  width: 205px;
  height: 205px;
  top: -20px;
  right: 38px;
}

.art-paper {
  position: absolute;
  width: 135px;
  height: 157px;
  top: 27px;
  right: 84px;
  border: 1px solid rgba(224, 238, 255, 0.48);
  border-radius: 11px;
  background: rgba(226, 239, 255, 0.08);
  transform: rotate(9deg);
}

.paper-back {
  right: 104px;
  top: 38px;
  background: rgba(174, 210, 251, 0.1);
  transform: rotate(-8deg);
}

.paper-front {
  display: flex;
  flex-direction: column;
  gap: 11px;
  padding: 28px 19px;
  background: linear-gradient(150deg, rgba(241, 248, 255, 0.2), rgba(213, 232, 255, 0.08));
  box-shadow: 0 16px 40px rgba(6, 25, 47, 0.12);
}

.paper-line {
  display: block;
  height: 4px;
  border-radius: 4px;
  background: rgba(219, 236, 255, 0.72);
}

.line-short { width: 52%; }
.line-mid { width: 78%; }

.art-star {
  position: absolute;
  color: #abd0fa;
  font-size: 16px;
}

.star-one { top: 40px; right: 45px; }
.star-two { top: 153px; right: 40px; color: #7fb1ec; font-size: 12px; }

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  min-width: 0;
  border: 1px solid #e7ecf3;
  background: #fff;
}

.metric-card :deep(.el-card__body) {
  padding: 18px 19px 16px;
}

.metric-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 13px;
}

.metric-icon {
  width: 37px;
  height: 37px;
  display: grid;
  place-items: center;
  flex: none;
  font-size: 17px;
  border-radius: 12px;
}

.tone-blue { color: #367ce0; background: #eaf2ff; }
.tone-violet { color: #7a66d6; background: #f0edff; }
.tone-amber { color: #c2822b; background: #fff4e4; }
.tone-green { color: #278a72; background: #e7f7f1; }

.metric-context {
  overflow: hidden;
  color: #9aa5b3;
  font-size: 11px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-card :deep(.el-statistic__head) { display: none; }
.metric-card :deep(.el-statistic__content) {
  color: #1c2d43;
  font-size: 27px;
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.1;
}

.metric-label {
  margin-top: 6px;
  color: #738196;
  font-size: 12px;
}

.metric-skeleton { width: 90px; height: 26px; margin: 0; }

.dashboard-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 15px;
  color: #9d6232;
  border: 1px solid #f0d8b7;
  border-radius: 10px;
  background: #fff8ee;
  font-size: 12px;
}

.dashboard-lower {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(300px, 0.9fr);
  gap: 16px;
}

.dashboard-panel {
  min-width: 0;
  border: 1px solid #e7ecf3;
  background: #fff;
}

.dashboard-panel :deep(.el-card__body) { padding: 21px 22px; }

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 15px;
}

.panel-heading h3 {
  margin: 0;
  color: #23344b;
  font-size: 15px;
  font-weight: 700;
}

.panel-heading p {
  margin: 5px 0 0;
  color: #8b97a7;
  font-size: 12px;
}

.panel-heading-icon {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  color: #6684ab;
  font-size: 16px;
  border-radius: 11px;
  background: #f1f5fa;
}

.dashboard-link-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 3px;
  text-align: left;
  border: 0;
  border-top: 1px solid #eff2f6;
  background: transparent;
  cursor: pointer;
}

.dashboard-link-row:hover .link-row-copy strong { color: #367ce0; }
.dashboard-link-row:hover .link-row-arrow { color: #367ce0; transform: translateX(2px); }

.link-row-icon {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  flex: none;
  color: #547aa8;
  font-size: 16px;
  border-radius: 11px;
  background: #f1f5fa;
}

.link-row-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.link-row-copy strong { color: #26364b; font-size: 13px; font-weight: 650; transition: color 150ms ease; }
.link-row-copy small { color: #8d99a8; font-size: 11px; }
.link-row-arrow { color: #a7b1bf; font-size: 15px; transition: color 150ms ease, transform 150ms ease; }

.site-link-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
}

.site-link-tile {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 11px 10px;
  color: #35465d;
  text-align: left;
  border: 1px solid #e9edf3;
  border-radius: 11px;
  background: #fff;
  cursor: pointer;
  transition: border-color 150ms ease, background 150ms ease;
}

.site-link-tile:hover { border-color: #b7d0f2; background: #f8fbff; }
.site-tile-icon { color: #5f84b0; font-size: 15px; }
.site-tile-arrow { margin-left: auto; color: #a9b4c2; font-size: 13px; }
.site-link-tile > span:nth-child(2) { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }

.site-panel-foot {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 17px;
  padding-top: 14px;
  color: #8a96a7;
  border-top: 1px solid #eff2f6;
  font-size: 11px;
}

.site-panel-foot .el-icon { color: #448e7c; }

@media (max-width: 1120px) {
  .metrics-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .welcome-art { right: -46px; opacity: 0.65; }
}

@media (max-width: 780px) {
  .dashboard-lower { grid-template-columns: 1fr; }
}

@media (max-width: 560px) {
  .dashboard-page { gap: 15px; }
  .dashboard-welcome { min-height: 226px; padding: 22px; }
  .welcome-art { right: -145px; opacity: 0.36; }
  .welcome-copy { max-width: 100%; }
  .welcome-copy h2 { max-width: 280px; }
  .welcome-copy p { max-width: 290px; }
  .metrics-grid { gap: 10px; }
  .metric-card :deep(.el-card__body) { padding: 14px; }
  .metric-context { max-width: 85px; }
  .metric-card :deep(.el-statistic__content) { font-size: 23px; }
  .dashboard-panel :deep(.el-card__body) { padding: 17px; }
}
</style>

