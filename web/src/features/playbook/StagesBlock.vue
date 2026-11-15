<script setup lang="ts">
import { computed } from 'vue'
import type { Stage } from './api'
import { fieldKey, useEditor } from './editor'
import FieldMessage from './FieldMessage.vue'
import { LIMITS, playbookMessages } from './messages'

const editor = useEditor()
const { editing, incomplete, readOnly } = editor
const stages = computed(() => editor.draft.value!.stages)
const full = computed(() => stages.value.length >= LIMITS.stages)

function move(index: number, by: -1 | 1) {
  const list = stages.value
  const [stage] = list.splice(index, 1) as [Stage]
  list.splice(index + by, 0, stage)
  void editor.save()
}
</script>

<template>
  <section class="card" aria-label="Etapas da call">
    <h2 class="lbl">Etapas da call</h2>
    <ol class="m-0 list-none p-0">
      <li
        v-for="(stage, i) in stages"
        :key="stage.id"
        :data-row="stage.id"
        class="row stage"
        :class="{ editing: editing === stage.id, incomplete: incomplete.has(stage.id) }"
        :data-incomplete="incomplete.has(stage.id) || undefined"
        @click="editor.startEdit(stage.id)"
        @focusout="editor.leaveRow($event, stage.id)"
      >
        <span class="num">{{ i + 1 }}</span>
        <div class="min-w-0 flex-1">
          <input
            v-if="editing === stage.id"
            v-model="stage.name"
            class="f"
            aria-label="Nome da etapa"
            @blur="editor.commitText(stage, 'name', fieldKey(stage.id, 'name'))"
          />
          <span v-else>{{ stage.name }}</span>
          <FieldMessage :field="fieldKey(stage.id, 'name')" />
        </div>
        <span class="row-tools flex gap-1">
          <button type="button" class="btn xs" aria-label="Subir etapa" :disabled="readOnly || i === 0" @click.stop="move(i, -1)">↑</button>
          <button type="button" class="btn xs" aria-label="Descer etapa" :disabled="readOnly || i === stages.length - 1" @click.stop="move(i, 1)">↓</button>
          <button type="button" class="btn xs" :disabled="readOnly" @click.stop="editor.removeItem(stages, stage.id)">Remover</button>
        </span>
      </li>
    </ol>
    <button
      type="button"
      class="btn sm mt-2.5"
      :disabled="readOnly || full"
      @click="editor.addItem(stages, { name: '' })"
    >
      + Etapa
    </button>
    <p v-if="full" class="mt-1 text-[12.5px] text-muted-foreground">{{ playbookMessages.blockLimit(LIMITS.stages) }}</p>
  </section>
</template>

<style scoped>
.stage { display: flex; gap: 10px; align-items: center; padding: 8px 0; border-bottom: 1px solid var(--line2); font-size: 13px; cursor: pointer; }
.num { width: 22px; height: 22px; border-radius: 50%; background: var(--pur-bg); color: var(--pur); font-size: 11.5px; font-weight: 600; display: grid; place-items: center; flex: none; }
</style>
