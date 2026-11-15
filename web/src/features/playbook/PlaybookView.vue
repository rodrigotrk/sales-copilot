<script setup lang="ts">
import { computed, onUnmounted, provide, ref } from 'vue'
import AppShell from '@/components/AppShell.vue'
import ImportDialog from '@/features/importing/ImportDialog.vue'
import { createEditor, editorKey, isEmptyDraft } from './editor'
import { playbookMessages as msg } from './messages'
import DiscoveryBlock from './DiscoveryBlock.vue'
import ObjectionsBlock from './ObjectionsBlock.vue'
import PriceTableBlock from './PriceTableBlock.vue'
import StagesBlock from './StagesBlock.vue'

const props = defineProps<{ businessId: string }>()
const editor = createEditor(props.businessId)
provide(editorKey, editor)
onUnmounted(editor.dispose)
void editor.reload()

const { state, draft, conflict, unsaved, importActive, publishError, importResult } = editor
const writing = ref(false)
const importOpen = ref(false)

const topText = computed(() => {
  if (!state.value) return ''
  const version = state.value.latestVersion
  return `Playbook: ${state.value.name} · ${version ? `versão ${version.number}` : 'rascunho'}`
})

const showStart = computed(
  () =>
    !!state.value &&
    !writing.value &&
    !state.value.latestVersion &&
    !importActive.value &&
    isEmptyDraft(draft.value!),
)

const canPublish = computed(
  () => !!state.value?.hasUnpublishedChanges && !unsaved.value && !importActive.value && !conflict.value,
)
</script>

<template>
  <AppShell :business-id="businessId" :top-text="topText">
    <main class="max-w-[1320px] px-[26px] pt-[22px] pb-10">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 class="m-0 text-[21px] font-bold tracking-[-.01em]">Playbook</h1>
          <p class="mt-1 mb-4 text-muted-foreground">Cada negócio cadastra o próprio jeito de vender, sem programar.</p>
        </div>
        <div v-if="state && !showStart" class="grid justify-items-end gap-1">
          <div class="flex items-center gap-2">
            <span v-if="state.hasUnpublishedChanges" class="text-[12.5px] text-muted-foreground">Alterações não publicadas</span>
            <button type="button" class="btn" :disabled="importActive || conflict" @click="importOpen = true">
              Importar documento
            </button>
            <button type="button" class="btn pri" :disabled="!canPublish" @click="editor.publish()">Publicar</button>
          </div>
          <p v-if="unsaved" class="m-0 text-[12.5px] text-muted-foreground">{{ msg.unsaved }}</p>
        </div>
      </div>

      <div class="mb-4 grid gap-2 empty:hidden">
        <p v-if="conflict" class="notice m-0" role="alert">{{ msg.conflict }}</p>
        <p v-if="importActive" class="notice m-0" role="status">{{ msg.importing }}</p>
        <p v-if="publishError" class="notice m-0" role="alert">{{ publishError }}</p>
        <div v-if="importResult.length" class="notice" role="status">
          <p v-for="line in importResult" :key="line" class="m-0">{{ line }}</p>
        </div>
      </div>

      <div v-if="showStart" class="flex gap-3">
        <button type="button" class="btn pri" @click="writing = true">Escrever o playbook</button>
        <button type="button" class="btn" @click="importOpen = true">Importar um documento</button>
      </div>
      <div v-else-if="draft" class="grid gap-3.5 xl:grid-cols-[1fr_1.4fr]">
        <StagesBlock />
        <DiscoveryBlock />
        <ObjectionsBlock />
        <PriceTableBlock />
      </div>

      <ImportDialog
        v-if="state"
        v-model:open="importOpen"
        :business-id="businessId"
        :playbook-id="state.playbookId"
        @started="editor.reload()"
      />
    </main>
  </AppShell>
</template>
