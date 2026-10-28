<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { apiGet, apiPost } from '@/api/client'

interface Contact { id: number; username: string; nickname: string | null; role: string }
interface Conversation { id: number; peerUserId: number; peerUsername: string; peerNickname: string | null; peerRole: string; title: string; canSend: boolean; lastMessage: string | null; lastMessageAt: string | null; unreadCount: number }
interface Message { id: number; conversationId: number; senderId: number; senderUsername: string; senderNickname: string | null; content: string; createdAt: string }

const adminId = Number(localStorage.getItem('whaa_user_id'))
const adminName = localStorage.getItem('whaa_nickname') || localStorage.getItem('whaa_username') || '管理员'
const conversations = ref<Conversation[]>([])
const contacts = ref<Contact[]>([])
const activeId = ref<number | null>(null)
const messages = ref<Message[]>([])
const searchText = ref('')
const draft = ref('')
const busy = ref(false)
const socketState = ref<'connecting' | 'online' | 'offline'>('connecting')
const notice = ref('')
const messagePane = ref<HTMLElement | null>(null)
const activeConversation = computed(() => conversations.value.find((item) => item.id === activeId.value) || null)
const socketLabel = computed(() => ({ connecting: '正在连接', online: '实时连接', offline: '等待重连' })[socketState.value])

let socket: WebSocket | null = null
let reconnectTimer: number | undefined
let disposed = false

async function loadConversations() { conversations.value = await apiGet<Conversation[]>('/chat/conversations') }

async function activateConversation(id: number) {
  activeId.value = id
  notice.value = ''
  try {
    messages.value = await apiGet<Message[]>(`/chat/conversations/${id}/messages`, { size: 50 })
    if (activeConversation.value?.canSend) await apiPost(`/chat/conversations/${id}/read`)
    await loadConversations()
    await scrollToBottom()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '暂时无法加载这段对话。'
  }
}

async function searchContacts() {
  try { contacts.value = await apiGet<Contact[]>('/chat/contacts', { q: searchText.value.trim() }) }
  catch (error) { notice.value = error instanceof Error ? error.message : '暂时无法搜索用户。' }
}

async function markActiveConversationRead(conversationId: number) {
  if (!activeConversation.value?.canSend) return
  try { await apiPost(`/chat/conversations/${conversationId}/read`); await loadConversations() }
  catch { /* The next refresh will reconcile unread state. */ }
}

async function startConversation(contact: Contact) {
  busy.value = true
  try {
    const conversation = await apiPost<Conversation>('/chat/conversations', { peerUserId: contact.id })
    contacts.value = []
    searchText.value = ''
    await loadConversations()
    await activateConversation(conversation.id)
  } catch (error) { notice.value = error instanceof Error ? error.message : '无法开启对话。' }
  finally { busy.value = false }
}

async function connectSocket() {
  if (disposed || socket?.readyState === WebSocket.OPEN || socket?.readyState === WebSocket.CONNECTING) return
  socketState.value = 'connecting'
  try {
    const { ticket } = await apiPost<{ ticket: string }>('/chat/ws-ticket')
    if (disposed) return
    const scheme = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    socket = new WebSocket(`${scheme}//${window.location.host}/ws/chat`, [`whaa.ticket.${ticket}`, 'whaa.chat.v1'])
    socket.onopen = () => {
      if (socket?.protocol !== 'whaa.chat.v1') { socket?.close(); return }
      socketState.value = 'online'
    }
    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data) as { type: string; message?: Message; messageText?: string }
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
        } else if (payload.type === 'ERROR') notice.value = (payload as unknown as { message: string }).message || '消息发送失败。'
      } catch { notice.value = '收到无法识别的消息。' }
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

function sendMessage() {
  const content = draft.value.trim()
  if (!content || content.length > 2000 || !activeConversation.value?.canSend || socket?.readyState !== WebSocket.OPEN) return
  socket.send(JSON.stringify({ type: 'SEND', conversationId: activeId.value, content }))
  draft.value = ''
  notice.value = ''
}

async function scrollToBottom() { await nextTick(); if (messagePane.value) messagePane.value.scrollTop = messagePane.value.scrollHeight }
function displayName(user: { nickname: string | null; username: string }) { return user.nickname?.trim() || user.username }
function roleName(role: string) { return role === 'ADMIN' ? '管理员' : '读者' }
function initial(name: string) { return name.slice(0, 1).toUpperCase() }
function formatTime(value: string | null) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(date)
}

onMounted(async () => {
  try { await loadConversations(); if (conversations.value.length) await activateConversation(conversations.value[0].id) }
  catch (error) { notice.value = error instanceof Error ? error.message : '无法载入消息列表。' }
  void connectSocket()
})

onBeforeUnmount(() => { disposed = true; if (reconnectTimer) window.clearTimeout(reconnectTimer); socket?.close() })
</script>

<template>
  <section class="admin-chat">
    <div class="chat-toolbar">
      <div><strong>读者私信</strong><span>一对一实时交流 · 私信仅对参与双方可见</span></div>
      <span class="chat-status" :class="`is-${socketState}`"><i></i>{{ socketLabel }}</span>
    </div>
    <div class="chat-workspace">
      <aside class="chat-rail">
        <div class="rail-title"><strong>全部对话</strong><span>{{ conversations.length }}</span></div>
        <form class="contact-search" @submit.prevent="searchContacts"><input v-model="searchText" aria-label="搜索用户" placeholder="搜索用户或昵称" /><button type="submit" aria-label="搜索">⌕</button></form>
        <div v-if="contacts.length" class="contact-results">
          <button v-for="contact in contacts" :key="contact.id" type="button" :disabled="busy" @click="startConversation(contact)"><span class="avatar small">{{ initial(displayName(contact)) }}</span><span class="contact-copy"><strong>{{ displayName(contact) }}</strong><small>{{ roleName(contact.role) }}</small></span><span class="contact-arrow">↗</span></button>
        </div>
        <div class="conversation-list">
          <button v-for="item in conversations" :key="item.id" type="button" class="conversation-row" :class="{ active: item.id === activeId }" @click="activateConversation(item.id)">
            <span class="avatar">{{ initial(displayName({ nickname: item.peerNickname, username: item.peerUsername })) }}</span>
            <span class="conversation-copy"><span class="conversation-top"><strong>{{ item.title }}</strong><time>{{ formatTime(item.lastMessageAt) }}</time></span><span class="conversation-bottom"><small>{{ item.lastMessage || roleName(item.peerRole) }}</small><i v-if="item.unreadCount" class="unread-count">{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</i></span></span>
          </button>
          <div v-if="!conversations.length" class="conversation-empty"><span>✦</span><strong>还没有对话</strong><small>搜索一位用户，开始第一段交流。</small></div>
        </div>
        <div class="chat-self"><span class="avatar self-avatar">{{ initial(adminName) }}</span><span><strong>{{ adminName }}</strong><small>管理员</small></span></div>
      </aside>

      <div class="chat-thread">
        <header v-if="activeConversation" class="thread-header"><span class="avatar">{{ initial(displayName({ nickname: activeConversation.peerNickname, username: activeConversation.peerUsername })) }}</span><div><strong>{{ activeConversation.title }}</strong><small>{{ activeConversation.canSend ? `${roleName(activeConversation.peerRole)} · 一对一私信` : '管理员只读查看 · 会话双方可见' }}</small></div><span class="thread-lock">{{ activeConversation.canSend ? '仅参与者可见' : '管理员只读' }}</span></header>
        <header v-else class="thread-header thread-header-empty">选择一段对话，或搜索用户开始私信</header>
        <div ref="messagePane" class="message-pane">
          <template v-if="activeConversation">
            <div class="thread-note">{{ activeConversation.title }}</div>
            <article v-for="message in messages" :key="message.id" class="message-row" :class="{ mine: message.senderId === adminId }">
              <span v-if="message.senderId !== adminId" class="avatar message-avatar">{{ initial(message.senderNickname || message.senderUsername) }}</span>
              <div class="message-content"><div class="message-meta"><strong>{{ message.senderId === adminId ? '我' : displayName({ nickname: message.senderNickname, username: message.senderUsername }) }}</strong><time>{{ formatTime(message.createdAt) }}</time></div><p>{{ message.content }}</p></div>
              <span v-if="message.senderId === adminId" class="avatar message-avatar mine-avatar">{{ initial(adminName) }}</span>
            </article>
            <p v-if="!messages.length" class="first-message">打个招呼，开启你们的交流吧。</p>
          </template>
          <div v-else class="thread-welcome"><span class="welcome-mark">W</span><strong>读者的来信，在这里相遇</strong><p>你也可以搜索用户，主动开启一段对话。</p></div>
        </div>
        <div v-if="notice" class="chat-notice" role="status">{{ notice }}</div>
        <form class="composer" @submit.prevent="sendMessage"><textarea v-model="draft" :disabled="!activeConversation?.canSend || socketState !== 'online'" maxlength="2000" rows="2" :placeholder="activeConversation?.canSend ? '写下想说的话…（Enter 发送，Shift + Enter 换行）' : '管理员只读查看此对话'" @keydown.enter.exact.prevent="sendMessage"></textarea><div class="composer-footer"><span>消息最长 2000 字。</span><button type="submit" :disabled="!activeConversation?.canSend || socketState !== 'online' || !draft.trim()">发送 <b>↗</b></button></div></form>
      </div>
    </div>
  </section>
</template>

<style scoped>
.admin-chat { overflow: hidden; border: 1px solid #e5eaf1; border-radius: 15px; background: #fff; box-shadow: 0 8px 26px rgba(26,52,85,.035); }
.chat-toolbar { min-height: 66px; display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 13px 19px; border-bottom: 1px solid #e8edf3; background: linear-gradient(105deg,#fff,#f8fbff); }
.chat-toolbar > div { display: grid; gap: 5px; }.chat-toolbar strong { color: #20324b; font-size: 13px; }.chat-toolbar > div span { color: #8492a5; font-size: 10px; }
.chat-status { display: inline-flex; align-items: center; gap: 7px; padding: 7px 10px; border: 1px solid #e4eaf1; border-radius: 999px; color: #7f8da0; background: #fff; font-size: 10px; white-space: nowrap; }.chat-status i { width: 7px; height: 7px; border-radius: 50%; background: #e5ad5e; }.chat-status.is-online i { background: #56b886; box-shadow: 0 0 0 3px #e6f7ee; }.chat-status.is-offline i { background: #d88383; }
.chat-workspace { height: min(68vh, 690px); min-height: 565px; display: grid; grid-template-columns: minmax(235px, 29%) minmax(0,1fr); }
.chat-rail { min-width: 0; display: flex; flex-direction: column; border-right: 1px solid #e8edf3; background: #f8fafd; }.rail-title { display: flex; justify-content: space-between; padding: 17px 16px 11px; color: #2a3a50; font-size: 12px; }.rail-title span { color: #8a97a7; font-size: 10px; }
.contact-search { display: flex; margin: 0 12px 9px; padding: 4px 5px 4px 10px; border: 1px solid #e4eaf1; border-radius: 9px; background: #fff; }.contact-search:focus-within { border-color: #b9d2f3; box-shadow: 0 0 0 3px rgba(54,124,224,.08); }.contact-search input { width: 100%; min-width: 0; border: 0; outline: 0; color: #26364d; background: transparent; font-size: 10px; }.contact-search button { width: 28px; height: 27px; border: 0; border-radius: 7px; color: #367ce0; background: #edf4fd; font-size: 17px; cursor: pointer; }
.contact-results { max-height: 150px; margin: 0 8px 8px; overflow-y: auto; border: 1px solid #e5eaf1; border-radius: 10px; background: #fff; }.contact-results button { width: 100%; display: flex; align-items: center; gap: 8px; padding: 9px; border: 0; color: #26364d; background: transparent; text-align: left; cursor: pointer; }.contact-results button + button { border-top: 1px solid #f0f2f6; }.contact-results button:hover { background: #f3f8ff; }.contact-copy { flex: 1; display: grid; gap: 3px; }.contact-copy strong { font-size: 10px; }.contact-copy small,.contact-arrow { color: #8492a5; font-size: 9px; }
.conversation-list { flex: 1; overflow-y: auto; padding: 0 7px 9px; }.conversation-row { width: 100%; display: flex; align-items: center; gap: 9px; padding: 10px 8px; border: 0; border-radius: 10px; color: #26364d; background: transparent; text-align: left; cursor: pointer; }.conversation-row:hover { background: #f0f5fc; }.conversation-row.active { background: #eaf2fd; }
.avatar { width: 36px; height: 36px; flex: 0 0 auto; display: grid; place-items: center; border: 1px solid #d9e8fa; border-radius: 12px; color: #3474c8; background: linear-gradient(145deg,#fff,#eef5ff); font-family: Georgia,serif; font-size: 13px; font-weight: 700; }.avatar.small { width: 29px; height: 29px; border-radius: 10px; font-size: 10px; }
.conversation-copy { min-width: 0; flex: 1; display: grid; gap: 6px; }.conversation-top,.conversation-bottom { min-width: 0; display: flex; align-items: center; justify-content: space-between; gap: 6px; }.conversation-top strong { overflow: hidden; color: #26364d; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.conversation-top time,.conversation-bottom small { color: #8794a5; font-size: 8px; }.conversation-bottom small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.unread-count { min-width: 17px; padding: 2px 4px; border-radius: 99px; color: #fff; background: #367ce0; font-size: 8px; font-style: normal; text-align: center; }
.conversation-empty { display: grid; justify-items: center; gap: 7px; padding: 29px 8px; color: #8492a5; text-align: center; }.conversation-empty span { color: #aa88cf; font-size: 20px; }.conversation-empty strong { color: #30445e; font-size: 10px; }.conversation-empty small { max-width: 150px; font-size: 9px; line-height: 1.65; }
.chat-self { display: flex; align-items: center; gap: 9px; padding: 11px; border-top: 1px solid #e7ecf2; background: #fff; }.self-avatar { width: 31px; height: 31px; color: #8059a6; border-color: #e7dcf2; background: #f8f3ff; }.chat-self > span:last-child { min-width: 0; display: grid; gap: 3px; }.chat-self strong { overflow: hidden; color: #2a3b51; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }.chat-self small { color: #8492a5; font-size: 8px; }
.chat-thread { min-width: 0; display: flex; flex-direction: column; background: #fff; }.thread-header { height: 65px; flex: 0 0 auto; display: flex; align-items: center; gap: 10px; padding: 0 18px; border-bottom: 1px solid #edf0f4; }.thread-header > div { display: grid; gap: 4px; }.thread-header > div strong { color: #23364f; font-size: 11px; }.thread-header > div small,.thread-header-empty { color: #8492a5; font-size: 9px; }.thread-lock { margin-left: auto; color: #758da6; font-size: 9px; }.thread-header-empty { justify-content: center; }
.message-pane { flex: 1; min-height: 0; overflow-y: auto; padding: 18px clamp(14px,3vw,31px); background: radial-gradient(circle at 50% 0,#f6f9fe,transparent 52%); }.thread-note { margin: 2px 0 22px; color: #8996a7; font-size: 9px; text-align: center; }.message-row { display: flex; align-items: flex-end; gap: 9px; margin: 0 0 16px; }.message-row.mine { justify-content: flex-end; }.message-avatar { width: 27px; height: 27px; border-radius: 9px; font-size: 9px; }.mine-avatar { color: #8059a6; border-color: #e7dcf2; background: #f8f3ff; }.message-content { max-width: min(78%,520px); }.message-meta { display: flex; align-items: center; gap: 8px; margin: 0 0 5px 2px; }.message-meta strong { color: #53647a; font-size: 9px; font-weight: 600; }.message-meta time { color: #9ba7b5; font-size: 8px; }.mine .message-meta { justify-content: flex-end; margin-right: 2px; }.message-content p { margin: 0; padding: 10px 13px; border: 1px solid #e6ebf2; border-radius: 4px 13px 13px 13px; color: #304158; background: #fff; box-shadow: 0 3px 10px rgba(40,65,100,.035); font-size: 11px; line-height: 1.75; white-space: pre-wrap; overflow-wrap: anywhere; }.mine .message-content p { border-color: transparent; border-radius: 13px 4px 13px 13px; color: #fff; background: linear-gradient(120deg,#5596e8,#407fd5); }.first-message { margin: 60px auto; color: #8492a5; font-size: 10px; text-align: center; }.thread-welcome { height: 100%; display: grid; align-content: center; justify-items: center; gap: 9px; color: #8492a5; text-align: center; }.welcome-mark { width: 53px; height: 53px; display: grid; place-items: center; margin-bottom: 5px; border: 1px solid #d8e9fb; border-radius: 18px; color: #367ce0; background: #edf5ff; font-family: Georgia,serif; font-size: 19px; font-weight: 700; }.thread-welcome strong { color: #263950; font-family: Georgia,"Microsoft YaHei",serif; font-size: 15px; }.thread-welcome p { margin: 0; font-size: 10px; }.chat-notice { padding: 8px 16px; border-top: 1px solid #f2d4d2; color: #a14848; background: #fff7f6; font-size: 9px; }.composer { flex: 0 0 auto; padding: 11px 15px 13px; border-top: 1px solid #e8edf3; background: #fff; }.composer textarea { width: 100%; resize: none; border: 0; outline: 0; color: #293c54; background: transparent; font-size: 11px; line-height: 1.65; }.composer textarea:disabled { opacity: .55; }.composer textarea::placeholder { color: #91a0b2; }.composer-footer { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: 6px; }.composer-footer > span { color: #91a0b2; font-size: 8px; }.composer-footer button { height: 30px; display: inline-flex; align-items: center; gap: 11px; padding: 0 11px; border: 0; border-radius: 8px; color: #fff; background: #367ce0; font-size: 9px; cursor: pointer; }.composer-footer button:disabled { opacity: .48; cursor: not-allowed; }.composer-footer button b { font-size: 12px; font-weight: 400; }
@media (max-width: 680px) { .chat-toolbar { align-items: flex-start; padding: 13px; }.chat-toolbar > div span { max-width: 235px; line-height: 1.5; }.chat-workspace { height: min(70vh,680px); min-height: 550px; grid-template-columns: 42% minmax(0,1fr); }.rail-title { padding-right: 10px; padding-left: 10px; }.conversation-list { padding-right: 4px; padding-left: 4px; }.avatar { width: 30px; height: 30px; border-radius: 10px; }.conversation-row { gap: 6px; padding: 9px 5px; }.thread-header { height: 57px; gap: 7px; padding: 0 9px; }.thread-header > div strong { max-width: 115px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.thread-lock { font-size: 8px; }.message-pane { padding-right: 10px; padding-left: 10px; }.composer { padding-right: 10px; padding-left: 10px; } }
@media (max-width: 420px) { .chat-workspace { grid-template-columns: 112px minmax(0,1fr); }.contact-search { margin-right: 6px; margin-left: 6px; padding-left: 6px; }.contact-search input { font-size: 9px; }.chat-self { padding: 8px 5px; }.chat-self strong { max-width: 75px; }.conversation-top time { display: none; }.conversation-bottom small { max-width: 58px; }.conversation-row .avatar { width: 26px; height: 26px; }.message-content { max-width: 86%; } }
</style>
