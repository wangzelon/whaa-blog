<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getData, postData } from '@/api/client'

const props = defineProps<{ targetType: string; targetId: number }>()

interface Comment {
  id: number
  nickname: string
  website?: string
  content: string
  createdAt: string
  replies?: Comment[]
}

const comments = ref<Comment[]>([])
const form = ref({ nickname: '', email: '', website: '', content: '' })
const message = ref('')

async function load() {
  comments.value = await getData<Comment[]>('/comments', {
    targetType: props.targetType,
    targetId: props.targetId,
  })
}

async function submit() {
  try {
    await postData('/comments', {
      ...form.value,
      targetType: props.targetType,
      targetId: props.targetId,
    })
    message.value = '评论已提交，审核通过后显示'
    form.value.content = ''
  } catch (e) {
    message.value = e instanceof Error ? e.message : '提交失败'
  }
}

onMounted(load)
</script>

<template>
  <section class="comments">
    <h3>评论</h3>
    <ul>
      <li v-for="c in comments" :key="c.id">
        <strong>{{ c.nickname }}</strong>
        <span class="time">{{ c.createdAt?.slice(0, 16) }}</span>
        <p>{{ c.content }}</p>
        <ul v-if="c.replies?.length">
          <li v-for="r in c.replies" :key="r.id">
            <strong>{{ r.nickname }}</strong>
            <p>{{ r.content }}</p>
          </li>
        </ul>
      </li>
    </ul>
    <form @submit.prevent="submit">
      <input v-model="form.nickname" placeholder="昵称" required />
      <input v-model="form.email" type="email" placeholder="邮箱（可选）" />
      <input v-model="form.website" placeholder="网址（可选）" />
      <textarea v-model="form.content" rows="4" placeholder="留言..." required />
      <button type="submit">提交</button>
      <p v-if="message" class="msg">{{ message }}</p>
    </form>
  </section>
</template>

<style scoped>
.comments {
  margin-top: 2rem;
  border-top: 1px solid var(--border);
  padding-top: 1rem;
}
form {
  display: grid;
  gap: 0.5rem;
  margin-top: 1rem;
}
input,
textarea,
button {
  font: inherit;
  padding: 0.5rem;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--card);
  color: var(--text);
}
button {
  width: fit-content;
  background: var(--accent);
  color: #fff;
  border: none;
  cursor: pointer;
}
.time {
  margin-left: 0.5rem;
  color: var(--muted);
  font-size: 0.8rem;
}
.msg {
  color: var(--muted);
}
</style>
