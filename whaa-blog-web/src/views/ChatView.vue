<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { deleteData, downloadFile, getData, postData, uploadFile } from '@/api/client'
import { currentUser } from '@/stores/auth'

interface Contact { id: number; username: string; nickname: string | null; role: string }
interface Conversation { id: number; peerUserId: number; peerUsername: string; peerNickname: string | null; peerRole: string; title: string; canSend: boolean; lastMessage: string | null; lastMessageAt: string | null; unreadCount: number }
interface Attachment { id: number; originalName: string; mimeType: string; sizeBytes: number }
interface Message { id: number; conversationId: number; senderId: number; senderUsername: string; senderNickname: string | null; content: string; createdAt: string; attachments: Attachment[] }
interface SelectedFile { file: File; previewUrl: string | null; uploaded: Attachment | null }

const conversations = ref<Conversation[]>([])
const contacts = ref<Contact[]>([])
const activeId = ref<number | null>(null)
const messages = ref<Message[]>([])
const searchText = ref('')
const draft = ref('')
const busy = ref(false)
const sending = ref(false)
const clearing = ref(false)
const socketState = ref<'connecting' | 'online' | 'offline'>('connecting')
const notice = ref('')
const messagePane = ref<HTMLElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const selectedFiles = ref<SelectedFile[]>([])
const imagePreviews = ref<Record<number, string>>({})
const previewLoading = ref<Record<number, boolean>>({})
const previewFailed = ref<Record<number, boolean>>({})
const previewTargets = new Map<number, Element>()
const previewAttachmentByTarget = new WeakMap<Element, Attachment>()
let previewObserver: IntersectionObserver | null = null
let previewObserverReady = false
let previewGeneration = 0
const activeConversation = computed(() => conversations.value.find((item) => item.id === activeId.value) || null)
const socketLabel = computed(() => ({ connecting: '正在连接', online: '实时连接', offline: '等待重连' })[socketState.value])
const maxAttachments = 5
const maxFileSize = 20 * 1024 * 1024
const allowedExtensions = new Set(['.jpg', '.jpeg', '.png', '.gif', '.webp', '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.txt', '.csv', '.zip'])
const acceptedFiles = 'image/jpeg,image/png,image/gif,image/webp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv,.zip'

let socket: WebSocket | null = null
let reconnectTimer: number | undefined
let disposed = false

async function loadConversations() {
  conversations.value = await getData<Conversation[]>('/chat/conversations')
}

async function activateConversation(id: number) {
  activeId.value = id
  notice.value = ''
  try {
    messages.value = await getData<Message[]>(`/chat/conversations/${id}/messages`, { size: 50 })
    if (activeConversation.value?.canSend) await postData(`/chat/conversations/${id}/read`)
    await loadConversations()
    await scrollToBottom()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '暂时无法加载这段对话。'
  }
}

async function searchContacts() {
  try {
    contacts.value = await getData<Contact[]>('/chat/contacts', { q: searchText.value.trim() })
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '暂时无法搜索用户。'
  }
}

function chooseFiles() {
  fileInput.value?.click()
}

function addFiles(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  queueFiles(files)
}

function queueFiles(files: File[]) {
  if (!files.length) return
  if (selectedFiles.value.length + files.length > maxAttachments) {
    notice.value = `每条消息最多添加 ${maxAttachments} 个附件。`
    return
  }

  for (const file of files) {
    if (file.size > maxFileSize) {
      notice.value = `${file.name} 超过 20 MB，未添加。`
      continue
    }
    const extension = file.name.slice(file.name.lastIndexOf('.')).toLowerCase()
    if (!allowedExtensions.has(extension)) {
      notice.value = `${file.name} 的格式暂不支持。`
      continue
    }
    selectedFiles.value.push({
      file,
      previewUrl: file.type.startsWith('image/') ? URL.createObjectURL(file) : null,
      uploaded: null,
    })
  }
}

function pasteImages(event: ClipboardEvent) {
  const clipboard = event.clipboardData
  if (!clipboard) return

  const imageFiles = Array.from(clipboard.files).filter((file) => file.type.startsWith('image/'))
  const imageItems = Array.from(clipboard.items).filter((item) => item.kind === 'file' && item.type.startsWith('image/'))
  if (!imageFiles.length && !imageItems.length) return

  event.preventDefault()
  const extensionByMime: Record<string, string> = {
    'image/jpeg': '.jpg',
    'image/jpg': '.jpg',
    'image/png': '.png',
    'image/gif': '.gif',
    'image/webp': '.webp',
  }
  const files = imageFiles.length
    ? imageFiles
    : imageItems.map((item) => item.getAsFile()).filter((file): file is File => file !== null)
  const pastedImages: File[] = []

  for (const [index, file] of files.entries()) {
    const mimeType = file.type.toLowerCase()
    const extension = extensionByMime[mimeType]
    if (!extension) {
      notice.value = '粘贴图片仅支持 JPG、PNG、GIF 和 WebP。'
      continue
    }
    const name = file.name && /\.(jpe?g|png|gif|webp)$/i.test(file.name)
      ? file.name
      : `pasted-image-${Date.now()}-${index + 1}${extension}`
    pastedImages.push(new File([file], name, { type: mimeType === 'image/jpg' ? 'image/jpeg' : mimeType }))
  }

  queueFiles(pastedImages)
}

function removeSelectedFile(index: number) {
  const [removed] = selectedFiles.value.splice(index, 1)
  if (removed?.previewUrl) URL.revokeObjectURL(removed.previewUrl)
}

function formatFileSize(size: number) {
  if (size < 1024 * 1024) return `${Math.max(1, Math.round(size / 1024))} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function isImageAttachment(attachment: Attachment) {
  return attachment.mimeType.startsWith('image/')
}

function setImagePreviewTarget(element: unknown, attachment: Attachment) {
  if (!(element instanceof Element)) {
    const previous = previewTargets.get(attachment.id)
    if (previous) {
      previewObserver?.unobserve(previous)
      previewTargets.delete(attachment.id)
    }
    return
  }

  previewTargets.set(attachment.id, element)
  previewAttachmentByTarget.set(element, attachment)
  if (imagePreviews.value[attachment.id] || previewLoading.value[attachment.id] || previewFailed.value[attachment.id]) return
  if (previewObserver) previewObserver.observe(element)
  else if (previewObserverReady) void previewAttachment(attachment)
}

function initializePreviewObserver() {
  previewObserverReady = true
  if (typeof IntersectionObserver === 'undefined') {
    previewTargets.forEach((_, attachmentId) => {
      const element = previewTargets.get(attachmentId)
      const attachment = element && previewAttachmentByTarget.get(element)
      if (attachment) void previewAttachment(attachment)
    })
    return
  }

  previewObserver = new IntersectionObserver((entries, observer) => {
    for (const entry of entries) {
      if (!entry.isIntersecting) continue
      observer.unobserve(entry.target)
      const attachment = previewAttachmentByTarget.get(entry.target)
      if (attachment) void previewAttachment(attachment)
    }
  }, { root: messagePane.value, rootMargin: '100px' })
  previewTargets.forEach((element) => previewObserver?.observe(element))
}

async function previewAttachment(attachment: Attachment) {
  if (imagePreviews.value[attachment.id] || previewLoading.value[attachment.id]) return
  const generation = previewGeneration
  previewLoading.value[attachment.id] = true
  try {
    const blob = await downloadFile(`/chat/attachments/${attachment.id}/content`)
    if (generation !== previewGeneration) return
    imagePreviews.value[attachment.id] = URL.createObjectURL(blob)
  } catch {
    if (generation === previewGeneration) previewFailed.value[attachment.id] = true
  } finally {
    if (generation === previewGeneration) previewLoading.value[attachment.id] = false
  }
}

function clearLoadedPreviews() {
  previewGeneration += 1
  Object.values(imagePreviews.value).forEach((url) => URL.revokeObjectURL(url))
  imagePreviews.value = {}
  previewLoading.value = {}
  previewFailed.value = {}
}

function discardSelectedFiles() {
  selectedFiles.value.forEach((selected) => {
    if (selected.previewUrl) URL.revokeObjectURL(selected.previewUrl)
  })
  selectedFiles.value = []
}

function discardUploadedSelectedFiles() {
  selectedFiles.value = selectedFiles.value.filter((selected) => {
    if (!selected.uploaded) return true
    if (selected.previewUrl) URL.revokeObjectURL(selected.previewUrl)
    return false
  })
}

async function downloadAttachment(attachment: Attachment) {
  try {
    const blob = await downloadFile(`/chat/attachments/${attachment.id}/content`)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = attachment.originalName
    link.click()
    window.setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '附件暂时无法下载。'
  }
}

async function markActiveConversationRead(conversationId: number) {
  if (!activeConversation.value?.canSend) return
  try {
    await postData(`/chat/conversations/${conversationId}/read`)
    await loadConversations()
  } catch {
    // The message remains visible; the next conversation refresh will reconcile unread state.
  }
}

async function startConversation(contact: Contact) {
  busy.value = true
  try {
    const conversation = await postData<Conversation>('/chat/conversations', { peerUserId: contact.id })
    contacts.value = []
    searchText.value = ''
    await loadConversations()
    await activateConversation(conversation.id)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '无法开启对话。'
  } finally {
    busy.value = false
  }
}

async function clearConversation() {
  const conversation = activeConversation.value
  if (!conversation?.canSend || clearing.value || sending.value) return
  const confirmed = window.confirm(`确定清空与“${conversation.title}”的全部会话内容吗？此操作对双方生效且无法恢复。`)
  if (!confirmed) return

  clearing.value = true
  notice.value = ''
  try {
    await deleteData(`/chat/conversations/${conversation.id}/messages`)
    messages.value = []
    clearLoadedPreviews()
    discardSelectedFiles()
    draft.value = ''
    await loadConversations()
    notice.value = '会话内容已清空。'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '清空会话内容失败，请稍后重试。'
  } finally {
    clearing.value = false
  }
}

async function connectSocket() {
  if (disposed || socket?.readyState === WebSocket.OPEN || socket?.readyState === WebSocket.CONNECTING) return
  socketState.value = 'connecting'
  try {
    const { ticket } = await postData<{ ticket: string }>('/chat/ws-ticket')
    if (disposed) return
    const scheme = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    socket = new WebSocket(`${scheme}//${window.location.host}/ws/chat`, [`whaa.ticket.${ticket}`, 'whaa.chat.v1'])
    socket.onopen = () => {
      if (socket?.protocol !== 'whaa.chat.v1') {
        socket?.close()
        return
      }
      socketState.value = 'online'
    }
    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data) as { type: string; message?: Message; messageText?: string; conversationId?: number }
        if (payload.type === 'MESSAGE' && payload.message) {
          const incoming = payload.message
          if (incoming.conversationId === activeId.value && !messages.value.some((item) => item.id === incoming.id)) {
            messages.value.push(incoming)
            if (activeConversation.value?.canSend) void markActiveConversationRead(incoming.conversationId)
            else void loadConversations()
            void scrollToBottom()
          } else {
            void loadConversations()
          }
        } else if (payload.type === 'CONVERSATION_CLEARED' && payload.conversationId === activeId.value) {
          messages.value = []
          clearLoadedPreviews()
          discardUploadedSelectedFiles()
          void loadConversations()
          notice.value = '会话内容已清空。'
        } else if (payload.type === 'ERROR') {
          notice.value = (payload as unknown as { message: string }).message || '消息发送失败。'
        }
      } catch {
        notice.value = '收到无法识别的消息。'
      }
    }
    socket.onclose = () => {
      socketState.value = 'offline'
      if (!disposed) reconnectTimer = window.setTimeout(() => void connectSocket(), 2500)
    }
    socket.onerror = () => socket?.close()
  } catch {
    socketState.value = 'offline'
    if (!disposed) reconnectTimer = window.setTimeout(() => void connectSocket(), 3000)
  }
}

async function sendMessage() {
  const content = draft.value.trim()
  const conversation = activeConversation.value
  if ((!content && !selectedFiles.value.length) || content.length > 2000
      || !conversation?.canSend || socket?.readyState !== WebSocket.OPEN || sending.value) return

  sending.value = true
  notice.value = ''
  try {
    const attachments: Attachment[] = []
    for (const selected of selectedFiles.value) {
      if (!selected.uploaded) {
        selected.uploaded = await uploadFile<Attachment>(`/chat/conversations/${conversation.id}/attachments`, selected.file)
      }
      attachments.push(selected.uploaded)
    }
    if (socket?.readyState !== WebSocket.OPEN) throw new Error('连接已断开，请稍后重试。')
    socket.send(JSON.stringify({
      type: 'SEND',
      conversationId: conversation.id,
      content,
      attachmentIds: attachments.map((attachment) => attachment.id),
    }))
    draft.value = ''
    selectedFiles.value.forEach((selected) => {
      if (selected.previewUrl) URL.revokeObjectURL(selected.previewUrl)
    })
    selectedFiles.value = []
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '消息发送失败，请重试。'
  } finally {
    sending.value = false
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messagePane.value) messagePane.value.scrollTop = messagePane.value.scrollHeight
}

function displayName(user: { nickname: string | null; username: string }) {
  return user.nickname?.trim() || user.username
}

function roleName(role: string) { return role === 'ADMIN' ? '管理员' : '读者' }
function initial(name: string) { return name.slice(0, 1).toUpperCase() }
function formatTime(value: string | null) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(date)
}

onMounted(async () => {
  initializePreviewObserver()
  try {
    await loadConversations()
    if (conversations.value.length) await activateConversation(conversations.value[0].id)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '无法载入消息列表。'
  }
  void connectSocket()
})

onBeforeUnmount(() => {
  disposed = true
  if (reconnectTimer) window.clearTimeout(reconnectTimer)
  socket?.close()
  previewObserver?.disconnect()
  Object.values(imagePreviews.value).forEach((url) => URL.revokeObjectURL(url))
  discardSelectedFiles()
})
</script>

<template>
  <div class="chat-page">
    <div class="chat-intro">
      <div><h1>站内私信</h1><p>和博主、读者聊聊想法。每段对话仅对参与双方可见。</p></div>
      <span class="chat-connection" :class="`is-${socketState}`"><i></i>{{ socketLabel }}</span>
    </div>

    <div class="chat-workspace">
      <aside class="chat-rail">
        <div class="chat-rail-heading"><strong>对话</strong><span>{{ conversations.length }}</span></div>
        <form class="contact-search" @submit.prevent="searchContacts">
          <input v-model="searchText" aria-label="搜索用户" placeholder="搜索用户或昵称" />
          <button type="submit" aria-label="搜索">⌕</button>
        </form>
        <div v-if="contacts.length" class="contact-results">
          <button v-for="contact in contacts" :key="contact.id" type="button" :disabled="busy" @click="startConversation(contact)">
            <span class="chat-avatar small">{{ initial(displayName(contact)) }}</span>
            <span class="contact-result-copy"><strong>{{ displayName(contact) }}</strong><small>{{ roleName(contact.role) }}</small></span>
            <span class="contact-arrow">↗</span>
          </button>
        </div>
        <p v-else class="search-hint">输入名字后按回车，查找可以私信的用户。</p>

        <div class="conversation-list">
          <button v-for="item in conversations" :key="item.id" type="button" class="conversation-row" :class="{ active: item.id === activeId }" @click="activateConversation(item.id)">
            <span class="chat-avatar">{{ initial(displayName({ nickname: item.peerNickname, username: item.peerUsername })) }}</span>
            <span class="conversation-copy">
              <span class="conversation-top"><strong>{{ item.title }}</strong><time>{{ formatTime(item.lastMessageAt) }}</time></span>
              <span class="conversation-bottom"><small>{{ item.lastMessage || roleName(item.peerRole) }}</small><i v-if="item.unreadCount" class="unread-count">{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</i></span>
            </span>
          </button>
          <div v-if="!conversations.length" class="conversation-empty"><span>✦</span><strong>还没有对话</strong><small>搜索一位用户，开始第一段交流。</small></div>
        </div>
        <div class="chat-self"><span class="chat-avatar self-avatar">{{ initial(currentUser?.nickname || currentUser?.username || '我') }}</span><span><strong>{{ currentUser?.nickname || currentUser?.username }}</strong><small>{{ currentUser?.role === 'ADMIN' ? '管理员' : '已登录' }}</small></span></div>
      </aside>

      <div class="chat-thread">
        <header v-if="activeConversation" class="thread-header">
          <span class="chat-avatar">{{ initial(displayName({ nickname: activeConversation.peerNickname, username: activeConversation.peerUsername })) }}</span>
          <div><strong>{{ activeConversation.title }}</strong><small>{{ activeConversation.canSend ? `${roleName(activeConversation.peerRole)} · 一对一私信` : '管理员只读查看 · 会话双方可见' }}</small></div>
          <button v-if="activeConversation.canSend" type="button" class="clear-conversation-button" :disabled="clearing || sending" title="清空双方可见的全部聊天内容" @click="clearConversation">{{ clearing ? '清空中…' : '清空内容' }}</button>
          <span class="thread-lock" title="仅会话参与者可见">⌑</span>
        </header>
        <div v-else class="thread-header thread-header-empty"><span>选择一段对话，或搜索用户开始私信</span></div>

        <div ref="messagePane" class="message-pane">
          <template v-if="activeConversation">
            <div class="thread-note"><span>—</span> {{ activeConversation.title }} <span>—</span></div>
            <article v-for="message in messages" :key="message.id" class="message-row" :class="{ mine: message.senderId === currentUser?.userId }">
              <span v-if="message.senderId !== currentUser?.userId" class="chat-avatar message-avatar">{{ initial(message.senderNickname || message.senderUsername) }}</span>
              <div class="message-content">
                <div class="message-meta"><strong>{{ message.senderId === currentUser?.userId ? '我' : displayName({ nickname: message.senderNickname, username: message.senderUsername }) }}</strong><time>{{ formatTime(message.createdAt) }}</time></div>
                <p v-if="message.content">{{ message.content }}</p>
                <div v-if="message.attachments?.length" class="message-attachments">
                  <div v-for="attachment in message.attachments" :key="attachment.id" class="message-attachment">
                    <div v-if="isImageAttachment(attachment)" class="attachment-preview-wrap" :ref="(element) => setImagePreviewTarget(element, attachment)">
                      <img v-if="imagePreviews[attachment.id]" class="attachment-preview" :src="imagePreviews[attachment.id]" :alt="attachment.originalName" />
                      <span v-else class="attachment-preview-status">{{ previewLoading[attachment.id] ? '图片加载中…' : previewFailed[attachment.id] ? '图片预览暂不可用' : '图片预览' }}</span>
                    </div>
                    <div class="attachment-file">
                      <span class="attachment-kind">{{ isImageAttachment(attachment) ? '图片' : '文件' }}</span>
                      <span class="attachment-details"><strong>{{ attachment.originalName }}</strong><small>{{ formatFileSize(attachment.sizeBytes) }}</small></span>
                      <button v-if="!isImageAttachment(attachment)" type="button" @click="downloadAttachment(attachment)">下载</button>
                    </div>
                  </div>
                </div>
              </div>
              <span v-if="message.senderId === currentUser?.userId" class="chat-avatar message-avatar mine-avatar">{{ initial(currentUser?.nickname || currentUser?.username || '我') }}</span>
            </article>
            <p v-if="!messages.length" class="first-message">打个招呼，开启你们的交流吧。</p>
          </template>
          <div v-else class="thread-welcome"><span class="welcome-mark">W</span><strong>让想法继续流动</strong><p>私信仅对这段对话的参与者开放。</p></div>
        </div>

        <div v-if="notice" class="chat-notice" role="status">{{ notice }}</div>
        <form class="composer" @submit.prevent="sendMessage">
          <input ref="fileInput" class="attachment-input" type="file" :accept="acceptedFiles" multiple @change="addFiles" />
          <div v-if="selectedFiles.length" class="selected-attachments">
            <div v-for="(selected, index) in selectedFiles" :key="`${selected.file.name}-${selected.file.lastModified}`" class="selected-attachment">
              <img v-if="selected.previewUrl" :src="selected.previewUrl" :alt="selected.file.name" />
              <span class="selected-file-name">{{ selected.file.name }}<small>{{ formatFileSize(selected.file.size) }}{{ selected.uploaded ? ' · 已上传' : '' }}</small></span>
              <button type="button" :disabled="sending" :aria-label="`移除 ${selected.file.name}`" @click="removeSelectedFile(index)">×</button>
            </div>
          </div>
          <textarea v-model="draft" :disabled="!activeConversation?.canSend || socketState !== 'online' || sending" maxlength="2000" rows="2" :placeholder="activeConversation?.canSend ? '写下想说的话…（Enter 发送，Shift + Enter 换行）' : '管理员只读查看此对话'" @keydown.enter.exact.prevent="sendMessage" @paste="pasteImages"></textarea>
          <div class="composer-footer">
            <div class="composer-tools">
              <button type="button" class="attach-button" :disabled="!activeConversation?.canSend || socketState !== 'online' || sending || selectedFiles.length >= maxAttachments" @click="chooseFiles">＋ 图片/文件</button>
              <span>可 Ctrl+V 粘贴图片；单个不超过 20 MB，最多 5 个。</span>
            </div>
            <button type="submit" :disabled="!activeConversation?.canSend || socketState !== 'online' || sending || (!draft.trim() && !selectedFiles.length)">{{ sending ? '发送中…' : '发送' }} <span>↗</span></button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-page { --chat-blue: #4c8fe8; overflow: hidden; border: 1px solid var(--border); border-radius: 16px; background: var(--surface); box-shadow: var(--card-shadow); }
.chat-intro { display: flex; align-items: center; justify-content: space-between; gap: 15px; padding: 20px 23px; border-bottom: 1px solid var(--border); background: color-mix(in srgb, var(--surface) 90%, var(--accent-soft)); }
.chat-intro h1 { margin: 0; color: var(--text); font-family: "Songti SC", "Noto Serif SC", Georgia, serif; font-size: 21px; font-weight: 650; }
.chat-intro p { margin: 6px 0 0; color: var(--muted); font-size: 11px; line-height: 1.6; }
.chat-connection { display: inline-flex; align-items: center; gap: 7px; padding: 7px 10px; border: 1px solid var(--border); border-radius: 999px; color: var(--muted); background: var(--surface); font-size: 10px; white-space: nowrap; }
.chat-connection i { width: 7px; height: 7px; border-radius: 50%; background: #e6ae60; }
.chat-connection.is-online i { background: #59bc8b; box-shadow: 0 0 0 3px rgb(89 188 139 / 14%); }
.chat-connection.is-offline i { background: #d88383; }
.chat-workspace { min-height: 555px; height: min(68vh, 690px); display: grid; grid-template-columns: minmax(230px, 30%) minmax(0, 1fr); }
.chat-rail { min-width: 0; display: flex; flex-direction: column; border-right: 1px solid var(--border); background: var(--surface-soft); }
.chat-rail-heading { display: flex; align-items: center; justify-content: space-between; padding: 17px 17px 11px; color: var(--text); font-size: 12px; }
.chat-rail-heading span { color: var(--muted); font-size: 10px; }
.contact-search { display: flex; margin: 0 13px 8px; padding: 4px 5px 4px 10px; border: 1px solid var(--border); border-radius: 9px; background: var(--surface); }
.contact-search:focus-within { border-color: var(--accent-border); box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent) 9%, transparent); }
.contact-search input { width: 100%; min-width: 0; border: 0; outline: none; color: var(--text); background: transparent; font-size: 10px; }
.contact-search button { width: 28px; height: 27px; flex: 0 0 auto; border: 0; border-radius: 7px; color: var(--accent-strong); background: var(--accent-soft); font-size: 17px; cursor: pointer; }
.search-hint { margin: 1px 14px 10px; color: var(--muted); font-size: 9px; line-height: 1.6; }
.contact-results { max-height: 152px; margin: 0 8px 8px; overflow-y: auto; border: 1px solid var(--border); border-radius: 10px; background: var(--surface); }
.contact-results button { width: 100%; display: flex; align-items: center; gap: 9px; padding: 9px; border: 0; color: var(--text); background: transparent; text-align: left; cursor: pointer; }
.contact-results button:hover { background: var(--accent-soft); }
.contact-results button + button { border-top: 1px solid var(--border-soft); }
.contact-result-copy { display: grid; flex: 1; gap: 3px; }
.contact-result-copy strong { font-size: 10px; }
.contact-result-copy small, .contact-arrow { color: var(--muted); font-size: 9px; }
.conversation-list { flex: 1; overflow-y: auto; padding: 0 8px 10px; }
.conversation-row { width: 100%; display: flex; align-items: center; gap: 10px; padding: 10px 8px; border: 0; border-radius: 10px; color: var(--text); background: transparent; text-align: left; cursor: pointer; }
.conversation-row:hover { background: color-mix(in srgb, var(--accent-soft) 65%, transparent); }
.conversation-row.active { background: var(--accent-soft); }
.chat-avatar { width: 37px; height: 37px; flex: 0 0 auto; display: grid; place-items: center; border: 1px solid color-mix(in srgb, var(--accent-border) 80%, transparent); border-radius: 13px; color: var(--accent-strong); background: linear-gradient(145deg, var(--surface), var(--accent-soft)); font-family: Georgia, serif; font-size: 14px; font-weight: 700; }
.chat-avatar.small { width: 30px; height: 30px; border-radius: 10px; font-size: 11px; }
.conversation-copy { min-width: 0; flex: 1; display: grid; gap: 6px; }
.conversation-top, .conversation-bottom { min-width: 0; display: flex; align-items: center; justify-content: space-between; gap: 7px; }
.conversation-top strong { overflow: hidden; color: var(--text); font-size: 10px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.conversation-top time, .conversation-bottom small { color: var(--muted); font-size: 8px; }
.conversation-bottom small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.unread-count { min-width: 17px; padding: 2px 4px; border-radius: 999px; color: #fff; background: var(--accent); font-size: 8px; font-style: normal; text-align: center; }
.conversation-empty { display: grid; justify-items: center; gap: 7px; padding: 30px 9px; color: var(--muted); text-align: center; }
.conversation-empty span { color: #b998d7; font-size: 20px; }
.conversation-empty strong { color: var(--text); font-size: 10px; }
.conversation-empty small { max-width: 145px; font-size: 9px; line-height: 1.7; }
.chat-self { display: flex; align-items: center; gap: 9px; padding: 12px; border-top: 1px solid var(--border); background: var(--surface); }
.self-avatar { width: 31px; height: 31px; border-radius: 11px; color: #8e67b9; background: linear-gradient(145deg, #fff, #f4ecff); }
.chat-self > span:last-child { min-width: 0; display: grid; gap: 3px; }
.chat-self strong { overflow: hidden; color: var(--text); font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.chat-self small { color: var(--muted); font-size: 8px; }
.chat-thread { min-width: 0; display: flex; flex-direction: column; background: var(--surface); }
.thread-header { height: 67px; flex: 0 0 auto; display: flex; align-items: center; gap: 11px; padding: 0 20px; border-bottom: 1px solid var(--border-soft); }
.thread-header > div { min-width: 0; flex: 1; display: grid; gap: 4px; }
.thread-header > div strong { color: var(--text); font-size: 11px; }
.thread-header > div small, .thread-header-empty { color: var(--muted); font-size: 9px; }
.clear-conversation-button { flex: 0 0 auto; padding: 6px 9px; border: 1px solid var(--border); border-radius: 7px; color: #a14848; background: var(--surface); font-size: 9px; cursor: pointer; }
.clear-conversation-button:disabled { opacity: .55; cursor: wait; }
.thread-lock { flex: 0 0 auto; color: #9cb4a8; font-size: 18px; }
.thread-header-empty { justify-content: center; }
.message-pane { flex: 1; min-height: 0; overflow-y: auto; padding: 18px clamp(14px, 3vw, 32px); background: radial-gradient(circle at 50% 0, color-mix(in srgb, var(--accent-soft) 48%, transparent), transparent 50%); }
.thread-note { display: flex; align-items: center; justify-content: center; gap: 10px; margin: 3px 0 23px; color: var(--muted); font-size: 9px; }
.thread-note span { color: color-mix(in srgb, var(--muted) 36%, transparent); }
.message-row { display: flex; align-items: flex-end; gap: 9px; margin: 0 0 16px; }
.message-row.mine { justify-content: flex-end; }
.message-avatar { width: 28px; height: 28px; border-radius: 10px; font-size: 10px; }
.mine-avatar { color: #8e67b9; background: linear-gradient(145deg, #fff, #f4ecff); }
.message-content { max-width: min(78%, 520px); }
.message-attachments { display: grid; gap: 7px; margin-top: 7px; }
.message-attachment { overflow: hidden; border: 1px solid var(--border); border-radius: 10px; background: var(--surface); }
.attachment-preview-wrap { max-width: min(100%, 340px); min-height: 86px; display: grid; place-items: center; background: var(--surface-soft); }
.attachment-preview { display: block; max-width: min(100%, 340px); max-height: 260px; object-fit: contain; background: var(--surface-soft); }
.attachment-preview-status { padding: 22px; color: var(--muted); font-size: 9px; }
.attachment-file { min-width: 210px; display: flex; align-items: center; gap: 8px; padding: 8px 10px; }
.attachment-kind { padding: 4px 6px; border-radius: 6px; color: var(--accent-strong); background: var(--accent-soft); font-size: 8px; }
.attachment-details { min-width: 0; flex: 1; display: grid; gap: 3px; }
.attachment-details strong { overflow: hidden; color: var(--text); font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.attachment-details small { color: var(--muted); font-size: 8px; }
.attachment-file button, .selected-attachment button { flex: 0 0 auto; padding: 5px 7px; border: 1px solid var(--border); border-radius: 6px; color: var(--accent-strong); background: var(--surface); font-size: 8px; cursor: pointer; }
.attachment-file button:disabled, .selected-attachment button:disabled { opacity: .5; cursor: wait; }
.message-meta { display: flex; align-items: center; gap: 8px; margin: 0 0 5px 2px; }
.message-meta strong { color: var(--text); font-size: 9px; font-weight: 650; }
.message-meta time { color: var(--muted); font-size: 8px; }
.mine .message-meta { justify-content: flex-end; margin-right: 2px; }
.message-content p { margin: 0; padding: 10px 13px; border: 1px solid var(--border); border-radius: 4px 13px 13px 13px; color: var(--text); background: var(--surface); box-shadow: 0 3px 10px rgb(41 74 116 / 3%); font-size: 11px; line-height: 1.75; white-space: pre-wrap; overflow-wrap: anywhere; }
.mine .message-content p { border-color: transparent; border-radius: 13px 4px 13px 13px; color: #fff; background: linear-gradient(120deg, #5596e8, #407fd5); box-shadow: 0 4px 12px rgb(62 126 214 / 14%); }
.first-message { margin: 60px auto; color: var(--muted); font-size: 10px; text-align: center; }
.thread-welcome { height: 100%; display: grid; align-content: center; justify-items: center; gap: 9px; color: var(--muted); text-align: center; }
.welcome-mark { width: 54px; height: 54px; display: grid; place-items: center; margin-bottom: 6px; border: 1px solid var(--accent-border); border-radius: 19px; color: var(--accent-strong); background: var(--accent-soft); font-family: Georgia, serif; font-size: 20px; font-weight: 700; }
.thread-welcome strong { color: var(--text); font-family: "Songti SC", "Noto Serif SC", Georgia, serif; font-size: 16px; }
.thread-welcome p { margin: 0; font-size: 10px; }
.chat-notice { padding: 8px 17px; border-top: 1px solid #f2d4d2; color: #a14848; background: #fff7f6; font-size: 9px; }
.composer { flex: 0 0 auto; padding: 11px 15px 13px; border-top: 1px solid var(--border); background: var(--surface); }
.attachment-input { display: none; }
.selected-attachments { display: flex; flex-wrap: wrap; gap: 7px; margin: 0 0 9px; }
.selected-attachment { max-width: min(100%, 260px); display: flex; align-items: center; gap: 7px; padding: 5px 7px; border: 1px solid var(--border); border-radius: 8px; background: var(--surface-soft); }
.selected-attachment img { width: 34px; height: 34px; flex: 0 0 auto; border-radius: 5px; object-fit: cover; }
.selected-file-name { min-width: 0; display: grid; gap: 3px; overflow: hidden; color: var(--text); font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.selected-file-name small { color: var(--muted); font-size: 8px; }
.composer textarea { width: 100%; resize: none; border: 0; outline: none; color: var(--text); background: transparent; font-size: 11px; line-height: 1.65; }
.composer textarea::placeholder { color: var(--muted); }
.composer textarea:disabled { opacity: .55; }
.composer-footer { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: 6px; }
.composer-tools { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.composer-tools > span { color: var(--muted); font-size: 8px; }
.attach-button { height: 28px; padding: 0 9px; border: 1px solid var(--border); border-radius: 8px; color: var(--accent-strong); background: var(--surface); font-size: 9px; cursor: pointer; }
.attach-button:disabled { opacity: .5; cursor: not-allowed; }
.composer-footer > button[type="submit"] { height: 30px; display: inline-flex; align-items: center; gap: 11px; padding: 0 11px; border: 0; border-radius: 8px; color: #fff; background: #4c8fe8; font-size: 9px; cursor: pointer; }
.composer-footer > button[type="submit"]:disabled { opacity: .48; cursor: not-allowed; }
.composer-footer > button[type="submit"] span { font-size: 13px; }
@media (max-width: 680px) { .chat-intro { align-items: flex-start; padding: 16px; } .chat-intro h1 { font-size: 19px; } .chat-intro p { max-width: 245px; } .chat-connection { gap: 5px; padding: 6px 7px; font-size: 8px; } .chat-workspace { height: min(70vh, 680px); min-height: 550px; grid-template-columns: 42% minmax(0, 1fr); } .chat-rail-heading { padding-right: 11px; padding-left: 11px; } .conversation-list { padding-right: 5px; padding-left: 5px; } .chat-avatar { width: 31px; height: 31px; border-radius: 11px; font-size: 12px; } .conversation-row { gap: 7px; padding: 9px 5px; } .thread-header { height: 58px; gap: 8px; padding: 0 10px; } .thread-header .chat-avatar { width: 29px; height: 29px; } .thread-header > div strong { max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .clear-conversation-button { padding: 5px 6px; font-size: 8px; } .message-pane { padding-right: 10px; padding-left: 10px; } .composer { padding-right: 10px; padding-left: 10px; } .composer-tools { max-width: calc(100% - 75px); } .attachment-file { min-width: 0; } }
@media (max-width: 420px) { .chat-workspace { grid-template-columns: 112px minmax(0, 1fr); } .chat-rail-heading { font-size: 10px; } .contact-search { margin-right: 6px; margin-left: 6px; padding-left: 6px; } .contact-search input { font-size: 9px; } .chat-self { padding: 8px 6px; } .chat-self strong { max-width: 75px; } .conversation-top time { display: none; } .conversation-bottom small { max-width: 60px; } .conversation-row { gap: 5px; } .conversation-row .chat-avatar { width: 27px; height: 27px; } .message-content { max-width: 86%; } }
@media (prefers-reduced-motion: reduce) { .chat-page * { scroll-behavior: auto !important; } }
</style>
