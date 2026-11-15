<script setup lang="ts">
import { computed } from 'vue'
import { fieldKey, useEditor } from './editor'
import FieldMessage from './FieldMessage.vue'
import { LIMITS, playbookMessages } from './messages'
import { quoted } from './quote'

const editor = useEditor()
const { editing, incomplete, readOnly } = editor
const items = computed(() => editor.draft.value!.discovery)
const full = computed(() => items.value.length >= LIMITS.discovery)
</script>

<template>
  <section class="card" aria-label="O que precisa ser descoberto na call">
    <h2 class="lbl">O que precisa ser descoberto na call</h2>
    <table class="tbl">
      <thead>
        <tr><th>Informação</th><th>Pergunta sugerida</th><th class="w-px"></th></tr>
      </thead>
      <tbody>
        <tr
          v-for="item in items"
          :key="item.id"
          :data-row="item.id"
          class="row cursor-pointer"
          :class="{ editing: editing === item.id, incomplete: incomplete.has(item.id) }"
          :data-incomplete="incomplete.has(item.id) || undefined"
          @click="editor.startEdit(item.id)"
          @focusout="editor.leaveRow($event, item.id)"
        >
          <template v-if="editing === item.id">
            <td>
              <input v-model="item.information" class="f" aria-label="Informação" @blur="editor.commitText(item, 'information', fieldKey(item.id, 'information'))" />
              <FieldMessage :field="fieldKey(item.id, 'information')" />
            </td>
            <td>
              <input v-model="item.question" class="f" aria-label="Pergunta sugerida" @blur="editor.commitText(item, 'question', fieldKey(item.id, 'question'), true)" />
              <FieldMessage :field="fieldKey(item.id, 'question')" />
            </td>
          </template>
          <template v-else>
            <td>{{ item.information }}<FieldMessage :field="fieldKey(item.id, 'information')" /></td>
            <td>{{ quoted(item.question) }}<FieldMessage :field="fieldKey(item.id, 'question')" /></td>
          </template>
          <td>
            <button type="button" class="row-tools btn xs" :disabled="readOnly" @click.stop="editor.removeItem(items, item.id)">Remover</button>
          </td>
        </tr>
      </tbody>
    </table>
    <button
      type="button"
      class="btn sm mt-2.5"
      :disabled="readOnly || full"
      @click="editor.addItem(items, { information: '', question: '' })"
    >
      + Informação
    </button>
    <p v-if="full" class="mt-1 text-[12.5px] text-muted-foreground">{{ playbookMessages.blockLimit(LIMITS.discovery) }}</p>
  </section>
</template>
