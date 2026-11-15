<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { logout, me, playbookPath } from '@/features/auth/session'

const props = defineProps<{ businessId: string; topText: string }>()
const router = useRouter()

// Ícones copiados do protótipo, para a barra lateral bater com a captura.
const soon = [
  { label: 'Nova call', paths: ['M3 10h18M8 3v4M16 3v4'], rect: { x: 3, y: 5, width: 18, height: 16 } },
  { label: 'Ao vivo', paths: ['M5 12a7 7 0 0 1 14 0M2 12a10 10 0 0 1 20 0'], circle: true },
  { label: 'Calls', paths: ['M6 3h9l4 4v14H6z', 'M9 12h7M9 16h5'] },
]
const coach = { label: 'Coach', paths: ['M2 9l10-5 10 5-10 5z', 'M6 11v5c3 2 9 2 12 0v-5'] }
const playbookIcon = [
  'M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1',
]
const canSwitch = computed(() => (me.value?.businesses.length ?? 0) > 1)

async function signOut() {
  await logout()
  await router.push('/entrar')
}
</script>

<template>
  <div class="grid min-h-screen grid-cols-[88px_1fr]">
    <nav class="side" aria-label="Menu">
      <div class="logo"><span>S</span></div>
      <a v-for="item in soon" :key="item.label" aria-disabled="true" title="Disponível em breve">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect v-if="item.rect" v-bind="item.rect" rx="3" />
          <circle v-if="item.circle" cx="12" cy="12" r="3" />
          <path v-for="d in item.paths" :key="d" :d="d" />
        </svg>
        <span>{{ item.label }}</span>
      </a>
      <div class="sep"></div>
      <a aria-disabled="true" title="Disponível em breve">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path v-for="d in coach.paths" :key="d" :d="d" /></svg>
        <span>{{ coach.label }}</span>
      </a>
      <RouterLink :to="playbookPath(props.businessId)" class="on" aria-current="page">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="12" cy="12" r="3" />
          <path v-for="d in playbookIcon" :key="d" :d="d" />
        </svg>
        <span>Playbook</span>
      </RouterLink>
      <div class="mt-auto grid gap-1">
        <RouterLink v-if="canSwitch" to="/negocios" class="text">Trocar de negócio</RouterLink>
        <button type="button" class="text" @click="signOut">Sair</button>
      </div>
    </nav>
    <div class="min-w-0">
      <header class="top">
        <span class="text-[13px] text-ink2">{{ topText }}</span>
      </header>
      <slot />
    </div>
  </div>
</template>

<style scoped>
.side { background: var(--card); border-right: 1px solid var(--border); padding: 14px 8px; display: flex; flex-direction: column; gap: 4px; position: sticky; top: 0; height: 100vh; }
.logo { display: flex; align-items: center; justify-content: center; height: 40px; margin-bottom: 10px; }
.logo span { width: 34px; height: 34px; border-radius: 50%; background: radial-gradient(circle at 35% 30%, var(--pur2), var(--pur) 70%); color: #fff; display: grid; place-items: center; font-weight: 700; font-size: 15px; }
.side a, .side .text { display: flex; flex-direction: column; align-items: center; gap: 3px; padding: 9px 4px; border-radius: 10px; color: var(--muted-foreground); text-decoration: none; font-size: 11.5px; text-align: center; background: none; border: 0; font-family: inherit; cursor: pointer; }
.side a[aria-disabled="true"] { cursor: default; }
.side a svg { width: 20px; height: 20px; stroke: currentColor; fill: none; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.side a:hover, .side .text:hover { background: var(--line2); color: var(--ink2); }
.side a.on { background: var(--pur-bg); color: var(--pur); font-weight: 600; }
.sep { height: 1px; background: var(--border); margin: 6px 10px; }
.top { height: 52px; background: var(--card); border-bottom: 1px solid var(--border); display: flex; align-items: center; gap: 14px; padding: 0 22px; position: sticky; top: 0; z-index: 2; }
</style>
