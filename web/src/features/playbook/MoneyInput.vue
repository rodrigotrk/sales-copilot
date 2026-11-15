<script setup lang="ts">
import { ref, watch } from 'vue'
import { formatMoney, parseMoney } from './money'

const props = defineProps<{ value: string | null; label: string; allowZero?: boolean }>()
const emit = defineEmits<{ commit: [value: string | null]; invalid: [] }>()

const text = ref('')
watch(
  () => props.value,
  (v) => (text.value = v ? formatMoney(v) : ''),
  { immediate: true },
)

function leave() {
  const typed = text.value.trim()
  if (!typed) return emit('commit', null)
  const value = parseMoney(typed, { allowZero: props.allowZero })
  if (value === null) emit('invalid')
  else emit('commit', value)
}
</script>

<template>
  <input v-model="text" class="f font-mono" inputmode="decimal" :aria-label="label" @blur="leave" />
</template>
