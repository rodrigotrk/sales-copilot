<script setup lang="ts">
import { computed } from 'vue'
import { fieldKey, useEditor } from './editor'
import FieldMessage from './FieldMessage.vue'
import { LIMITS, playbookMessages } from './messages'
import { quoted } from './quote'

const editor = useEditor()
const { editing, incomplete, readOnly } = editor
const items = computed(() => editor.draft.value!.objections)
const full = computed(() => items.value.length >= LIMITS.objections)
const hasCalls = computed(() => editor.state.value!.hasCalls)
</script>

<template>
  <section class="card xl:col-span-2" aria-label="Objeções e sugestões de fala">
    <div class="flex items-center justify-between">
      <h2 class="lbl m-0">Objeções e sugestões de fala</h2>
      <button
        type="button"
        class="btn sm"
        :disabled="readOnly || full"
        @click="editor.addItem(items, { name: '', leadSays: '', approvedResponse: '' })"
      >
        + Objeção
      </button>
    </div>
    <p v-if="full" class="mt-1 text-right text-[12.5px] text-muted-foreground">{{ playbookMessages.blockLimit(LIMITS.objections) }}</p>
    <!-- Entre 1024 e 1279 px a tabela pode não caber e rola dentro do bloco. -->
    <div class="overflow-x-auto">
      <table class="tbl">
        <thead>
          <tr>
            <th>Objeção</th>
            <th>Como o lead costuma dizer</th>
            <th>Sugestão de fala</th>
            <th v-if="hasCalls">Nas calls</th>
            <th class="w-px"></th>
          </tr>
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
                <input v-model="item.name" class="f" aria-label="Objeção" @blur="editor.commitText(item, 'name', fieldKey(item.id, 'name'))" />
                <FieldMessage :field="fieldKey(item.id, 'name')" />
              </td>
              <td>
                <input v-model="item.leadSays" class="f" aria-label="Como o lead costuma dizer" @blur="editor.commitText(item, 'leadSays', fieldKey(item.id, 'leadSays'), true)" />
                <FieldMessage :field="fieldKey(item.id, 'leadSays')" />
              </td>
              <td>
                <input v-model="item.approvedResponse" class="f" aria-label="Sugestão de fala" @blur="editor.commitText(item, 'approvedResponse', fieldKey(item.id, 'approvedResponse'), true)" />
                <FieldMessage :field="fieldKey(item.id, 'approvedResponse')" />
              </td>
            </template>
            <template v-else>
              <td class="font-bold">{{ item.name }}<FieldMessage :field="fieldKey(item.id, 'name')" /></td>
              <td class="text-muted-foreground">{{ quoted(item.leadSays) }}<FieldMessage :field="fieldKey(item.id, 'leadSays')" /></td>
              <td>{{ quoted(item.approvedResponse) }}<FieldMessage :field="fieldKey(item.id, 'approvedResponse')" /></td>
            </template>
            <td v-if="hasCalls"></td>
            <td>
              <button type="button" class="row-tools btn xs" :disabled="readOnly" @click.stop="editor.removeItem(items, item.id)">Remover</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
