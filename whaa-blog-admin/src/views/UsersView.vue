<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, User } from '@element-plus/icons-vue'
import { apiDelete, apiGet, apiPut } from '@/api/client'

type UserStatus = 'ACTIVE' | 'DISABLED' | 'DELETED'
interface UserRow {
  id: number
  username: string
  nickname: string | null
  email: string | null
  role: string
  status: UserStatus
  createdAt: string
  deletedAt: string | null
}
interface UserPage {
  records: UserRow[]
  total: number
  page: number
  size: number
}

const rows = ref<UserRow[]>([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<'ALL' | UserStatus>('ALL')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const resetDialogVisible = ref(false)
const resetTarget = ref<UserRow | null>(null)
const resetting = ref(false)
const resetForm = reactive({ password: '', confirmPassword: '' })

const resetFormValid = computed(() => {
  const byteLength = new TextEncoder().encode(resetForm.password).length
  return resetForm.password.length >= 8
    && byteLength <= 72
    && resetForm.password === resetForm.confirmPassword
})

async function load() {
  loading.value = true
  try {
    const page = await apiGet<UserPage>('/admin/users', {
      page: currentPage.value,
      size: pageSize.value,
      q: keyword.value.trim() || undefined,
      status: statusFilter.value === 'ALL' ? undefined : statusFilter.value,
    })
    rows.value = page.records ?? []
    total.value = page.total ?? 0
    if (!rows.value.length && currentPage.value > 1) {
      currentPage.value -= 1
      await load()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '用户列表加载失败。')
  } finally {
    loading.value = false
  }
}

function applySearch() {
  currentPage.value = 1
  void load()
}

function openResetDialog(row: UserRow) {
  resetTarget.value = row
  resetForm.password = ''
  resetForm.confirmPassword = ''
  resetDialogVisible.value = true
}

async function submitPasswordReset() {
  if (!resetTarget.value || !resetFormValid.value || resetting.value) return
  resetting.value = true
  try {
    await apiPut(`/admin/users/${resetTarget.value.id}/password`, { password: resetForm.password })
    ElMessage.success(`已重置 ${resetTarget.value.username} 的密码；该用户需要使用新密码重新登录。`)
    resetDialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '密码重置失败。')
  } finally {
    resetting.value = false
  }
}

async function changeStatus(row: UserRow) {
  const nextStatus: UserStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const isDisabling = nextStatus === 'DISABLED'
  try {
    await ElMessageBox.confirm(
      isDisabling
        ? `禁用后，${row.username} 将立即退出并无法登录、刷新令牌或继续使用私信。`
        : `重新启用后，${row.username} 可以再次登录并使用站点。`,
      isDisabling ? '确认禁用用户' : '确认启用用户',
      {
        confirmButtonText: isDisabling ? '禁用用户' : '启用用户',
        cancelButtonText: '取消',
        type: isDisabling ? 'warning' : 'info',
      },
    )
    await apiPut(`/admin/users/${row.id}/status`, { status: nextStatus })
    ElMessage.success(isDisabling ? '用户已禁用。' : '用户已启用。')
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '用户状态更新失败。')
    }
  }
}

async function deleteUser(row: UserRow) {
  try {
    await ElMessageBox.confirm(
      '此操作会注销并匿名化账号、释放原用户名。文章、私信和媒体等关联内容会保留，账号不可恢复。',
      `删除用户 ${row.username}`,
      {
        confirmButtonText: '删除用户',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await apiDelete(`/admin/users/${row.id}`)
    ElMessage.success('用户已删除并匿名化，关联内容已保留。')
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '删除用户失败。')
    }
  }
}

function statusLabel(status: UserStatus) {
  return status === 'ACTIVE' ? '正常' : status === 'DISABLED' ? '已禁用' : '已注销'
}

function statusType(status: UserStatus) {
  return status === 'ACTIVE' ? 'success' : status === 'DISABLED' ? 'warning' : 'info'
}

onMounted(load)
</script>

<template>
  <div class="users-page">
    <div class="users-toolbar">
      <div class="users-filters">
        <el-input
          v-model="keyword"
          class="user-search"
          clearable
          placeholder="搜索用户名或昵称"
          :prefix-icon="Search"
          @keyup.enter="applySearch"
        />
        <el-select v-model="statusFilter" class="status-filter" aria-label="用户状态筛选" @change="applySearch">
          <el-option label="全部状态" value="ALL" />
          <el-option label="正常" value="ACTIVE" />
          <el-option label="已禁用" value="DISABLED" />
          <el-option label="已注销" value="DELETED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="applySearch">搜索</el-button>
      </div>
      <el-button class="refresh-button" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-card class="users-card" shadow="never">
      <div class="users-card-heading">
        <div class="users-title-block">
          <span class="users-title-icon"><el-icon><User /></el-icon></span>
          <div><strong>注册用户</strong><span>共 {{ total }} 个账号</span></div>
        </div>
        <el-tag effect="plain" round>{{ statusFilter === 'ALL' ? '全部状态' : statusLabel(statusFilter) }}</el-tag>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-primary-cell">
              <strong>{{ row.nickname || row.username }}</strong>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="112">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light" round>{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" min-width="170" />
        <el-table-column label="操作" min-width="240" align="right">
          <template #default="{ row }">
            <template v-if="row.status !== 'DELETED'">
              <el-button link type="primary" @click="openResetDialog(row)">重置密码</el-button>
              <el-button link :type="row.status === 'ACTIVE' ? 'warning' : 'success'" @click="changeStatus(row)">
                {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
              </el-button>
              <el-button link type="danger" @click="deleteUser(row)">删除</el-button>
            </template>
            <span v-else class="deleted-user-note">内容保留</span>
          </template>
        </el-table-column>
        <template #empty><el-empty description="没有找到用户" :image-size="82" /></template>
      </el-table>

      <div class="users-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="load"
          @size-change="applySearch"
        />
      </div>
    </el-card>

    <el-dialog v-model="resetDialogVisible" title="重置用户密码" width="440px" :close-on-click-modal="false" @closed="resetTarget = null">
      <el-form label-position="top" @submit.prevent="submitPasswordReset">
        <el-form-item label="用户">
          <el-input :model-value="resetTarget?.username || ''" disabled />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetForm.password" type="password" show-password autocomplete="new-password" placeholder="至少 8 位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="resetForm.confirmPassword" type="password" show-password autocomplete="new-password" placeholder="再次输入新密码" />
        </el-form-item>
        <p class="password-hint">至少 8 位，UTF-8 编码不超过 72 字节。重置后旧登录会话立即失效。</p>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!resetFormValid" :loading="resetting" @click="submitPasswordReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.users-page { display: flex; flex-direction: column; gap: 15px; }
.users-toolbar, .users-filters { display: flex; align-items: center; gap: 9px; }
.users-toolbar { justify-content: space-between; }
.user-search { width: min(320px, 34vw); }
.status-filter { width: 138px; }
.refresh-button { color: #56667b; }
.users-card { border: 1px solid #e7ecf3; }
.users-card :deep(.el-card__body) { padding: 0 21px 16px; }
.users-card-heading { min-height: 72px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.users-title-block { display: flex; align-items: center; gap: 11px; }
.users-title-icon { width: 34px; height: 34px; display: grid; place-items: center; color: #4c78b1; font-size: 16px; border-radius: 10px; background: #eff5fc; }
.users-title-block > div { display: flex; flex-direction: column; gap: 3px; }
.users-title-block strong { color: #29394e; font-size: 13px; }
.users-title-block span { color: #96a1af; font-size: 11px; }
.user-primary-cell { display: flex; flex-direction: column; gap: 4px; }
.user-primary-cell strong { color: #2b3b51; font-size: 13px; font-weight: 650; }
.user-primary-cell span, .deleted-user-note { color: #9aa5b2; font-size: 11px; }
.users-pagination { display: flex; justify-content: flex-end; padding-top: 17px; }
.password-hint { margin: -5px 0 0; color: #768396; font-size: 12px; line-height: 1.6; }
@media (max-width: 900px) {
  .users-toolbar { align-items: stretch; flex-direction: column; }
  .users-filters { flex-wrap: wrap; }
  .user-search { flex: 1; width: auto; }
  .refresh-button { align-self: flex-end; }
  .users-pagination { justify-content: center; overflow-x: auto; }
}
@media (max-width: 560px) {
  .users-card :deep(.el-card__body) { padding: 0 12px 12px; }
  .status-filter { width: 118px; }
  .users-filters > .el-button { margin-left: auto; }
  .users-card :deep(.el-pagination__sizes), .users-card :deep(.el-pagination__jump) { display: none; }
}
</style>
