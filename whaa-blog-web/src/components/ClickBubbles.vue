<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

interface Bubble {
  id: number
  dx: number
  dy: number
  size: number
  delay: number
  hue: number
}

interface Burst {
  id: number
  x: number
  y: number
  bubbles: Bubble[]
}

const bursts = ref<Burst[]>([])
let nextId = 0

function handleClick(event: MouseEvent) {
  if (event.button !== 0 || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

  const target = event.target
  if (!(target instanceof HTMLElement)) return
  if (target.closest('a, button, input, textarea, select, summary, [role="button"], [contenteditable="true"]')) return

  // Fire only on the page canvas and empty layout regions, not on copy or content cards.
  const isCanvas = target === document.body || target === document.documentElement
    || target.classList.contains('site-shell')
    || target.classList.contains('site-header')
    || target.classList.contains('header-inner')
    || target.classList.contains('hero')
    || target.classList.contains('main-content')
    || target.classList.contains('page-content')
    || target.classList.contains('mobile-nav')
    || target.classList.contains('main-nav')
  if (!isCanvas) return

  const id = ++nextId
  const bubbles = Array.from({ length: 6 }, (_, index) => {
    const angle = (Math.PI * 2 * index) / 6 + (Math.random() - 0.5) * 0.4
    const distance = 20 + Math.random() * 25
    return {
      id: index,
      dx: Math.cos(angle) * distance,
      dy: Math.sin(angle) * distance - 14,
      size: 5 + Math.random() * 7,
      delay: Math.random() * 90,
      hue: Math.random() > 0.78 ? 265 : 198 + Math.random() * 28,
    }
  })

  bursts.value.push({ id, x: event.clientX, y: event.clientY, bubbles })
  if (bursts.value.length > 10) bursts.value.splice(0, bursts.value.length - 10)
  window.setTimeout(() => {
    bursts.value = bursts.value.filter((burst) => burst.id !== id)
  }, 1000)
}

onMounted(() => document.addEventListener('click', handleClick, { passive: true }))
onUnmounted(() => document.removeEventListener('click', handleClick))
</script>

<template>
  <div class="click-bubbles" aria-hidden="true">
    <span v-for="burst in bursts" :key="burst.id" class="bubble-burst" :style="{ left: `${burst.x}px`, top: `${burst.y}px` }">
      <i
        v-for="bubble in burst.bubbles"
        :key="bubble.id"
        class="bubble-particle"
        :style="{
          '--dx': `${bubble.dx}px`,
          '--dy': `${bubble.dy}px`,
          '--size': `${bubble.size}px`,
          '--delay': `${bubble.delay}ms`,
          '--hue': bubble.hue,
        }"
      />
    </span>
  </div>
</template>

<style scoped>
.click-bubbles { position: fixed; z-index: 100; inset: 0; overflow: hidden; pointer-events: none; }
.bubble-burst { position: absolute; width: 1px; height: 1px; }
.bubble-particle {
  position: absolute;
  top: 0;
  left: 0;
  width: var(--size);
  height: var(--size);
  border: 1px solid hsl(var(--hue) 76% 72% / 72%);
  border-radius: 50%;
  background: radial-gradient(circle at 34% 28%, rgb(255 255 255 / 85%), hsl(var(--hue) 82% 79% / 35%) 58%, hsl(var(--hue) 85% 68% / 14%));
  box-shadow: 0 1px 8px hsl(var(--hue) 80% 70% / 22%), inset -1px -1px 2px rgb(255 255 255 / 65%);
  animation: bubble-float 760ms cubic-bezier(.16,.7,.3,1) both;
  animation-delay: var(--delay);
}
@keyframes bubble-float {
  0% { opacity: 0; transform: translate(-50%, -50%) scale(.35); }
  18% { opacity: .9; transform: translate(-50%, -50%) scale(1); }
  100% { opacity: 0; transform: translate(calc(-50% + var(--dx)), calc(-50% + var(--dy) - 18px)) scale(.72); }
}
@media (prefers-reduced-motion: reduce) { .click-bubbles { display: none; } }
</style>
