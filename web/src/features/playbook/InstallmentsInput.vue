<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Installments } from './api'
import { formatMoney, parseMoney } from './money'

const props = defineProps<{ value: Installments | null }>()
const emit = defineEmits<{ commit: [value: Installments | null]; invalid: [] }>()

const count = ref('')
const amount = ref('')
// Observa o conteúdo, não a referência: uma gravação de outro campo que devolve o rascunho cria objetos
// novos com os mesmos valores, e reiniciar aqui apagaria o que a pessoa está digitando.
watch(
  () => (props.value ? `${props.value.count}|${props.value.value}` : ''),
  () => {
    const v = props.value
    count.value = v ? String(v.count) : ''
    amount.value = v ? formatMoney(v.value) : ''
  },
  { immediate: true },
)

// Valida o par só quando o foco sai dos dois campos, para não reclamar no meio do preenchimento.
function leave(event: FocusEvent) {
  const box = event.currentTarget as HTMLElement
  if (box.contains(event.relatedTarget as Node | null)) return
  const n = count.value.trim()
  const v = amount.value.trim()
  if (!n && !v) return emit('commit', null)
  const value = parseMoney(v, { capped: false })
  const parcels = Number(n)
  if (!/^\d+$/.test(n) || parcels < 2 || parcels > 60 || value === null) return emit('invalid')
  emit('commit', { count: parcels, value })
}
</script>

<template>
  <span class="flex items-center gap-1" @focusout="leave">
    <input v-model="count" class="f w-14 font-mono" inputmode="numeric" aria-label="Número de parcelas" />
    <span>x</span>
    <input v-model="amount" class="f font-mono" inputmode="decimal" aria-label="Valor da parcela" />
  </span>
</template>
