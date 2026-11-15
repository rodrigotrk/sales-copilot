<script setup lang="ts">
import { useEditor } from './editor'
import { playbookMessages } from './messages'

defineProps<{ field: string }>()
const editor = useEditor()
const { errors, saveFailed, failedField } = editor
</script>

<template>
  <p v-if="errors[field]" class="err" role="alert">{{ errors[field] }}</p>
  <div v-else-if="saveFailed && failedField === field" class="err flex flex-wrap items-center gap-2" role="alert">
    {{ playbookMessages.saveFailed }}
    <button type="button" class="btn xs" @click.stop="editor.retry()">Tentar de novo</button>
    <button type="button" class="btn xs" :disabled="editor.saving.value" @click.stop="editor.undo()">Desfazer</button>
  </div>
</template>
