<script setup lang="ts">
import { computed, ref } from 'vue'
import { ApiError } from '@/lib/http'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { startImport, type ImportSource } from './api'
import { importErrorMessages, importSendFailed } from './messages'

const props = defineProps<{ businessId: string; playbookId: string }>()
const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{ started: [] }>()

const ACCEPTED = ['.docx', '.pdf', '.txt', '.md']
const MAX_BYTES = 10 * 1024 * 1024

type Kind = 'file' | 'text' | 'googleDocsUrl'
const kinds: { kind: Kind; label: string }[] = [
  { kind: 'file', label: 'Arquivo' },
  { kind: 'text', label: 'Texto colado' },
  { kind: 'googleDocsUrl', label: 'Link do Google Docs' },
]
const kind = ref<Kind>('file')
const file = ref<File | null>(null)
const text = ref('')
const url = ref('')
const error = ref('')
const sending = ref(false)

function setOpen(value: boolean) {
  if (value || !sending.value) open.value = value
}

const ready = computed(() =>
  kind.value === 'file' ? !!file.value : kind.value === 'text' ? !!text.value.trim() : !!url.value.trim(),
)

function pick(event: Event) {
  file.value = (event.target as HTMLInputElement).files?.[0] ?? null
  error.value = ''
}

function clientError(f: File): string | null {
  const name = f.name.toLowerCase()
  if (!ACCEPTED.some((ext) => name.endsWith(ext))) return importErrorMessages.unsupported_format!
  if (f.size > MAX_BYTES) return importErrorMessages.file_too_large!
  return null
}

async function submit() {
  error.value = ''
  let source: ImportSource
  if (kind.value === 'file') {
    const problem = clientError(file.value!)
    if (problem) {
      error.value = problem
      return
    }
    source = { file: file.value! }
  } else source = kind.value === 'text' ? { text: text.value } : { googleDocsUrl: url.value.trim() }
  sending.value = true
  try {
    await startImport(props.businessId, props.playbookId, source)
  } catch (e) {
    if (!(e instanceof ApiError) && !(e instanceof TypeError)) throw e
    // Outra importação já começou: a tela passa a mostrar "Importando…".
    if (!(e instanceof ApiError && e.code === 'import_running')) {
      error.value = (e instanceof ApiError && importErrorMessages[e.code]) || importSendFailed
      return
    }
  } finally {
    sending.value = false
  }
  open.value = false
  emit('started')
}
</script>

<template>
  <!-- Com o envio pendente o diálogo não fecha (botão, Esc ou clique fora): fechado, o rascunho voltaria a
       aceitar edição, e o 202 que chega depois recarregaria o rascunho por cima dela. -->
  <Dialog :open="open" @update:open="setOpen">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>Importar documento</DialogTitle>
        <DialogDescription>
          O que o documento trouxer entra no fim de cada bloco do rascunho.
        </DialogDescription>
      </DialogHeader>
      <form class="grid gap-3" @submit.prevent="submit">
        <div class="flex flex-wrap gap-2" role="radiogroup" aria-label="Origem">
          <label v-for="k in kinds" :key="k.kind" class="btn sm" :class="{ pri: kind === k.kind }">
            <input v-model="kind" class="sr-only" type="radio" name="origem" :value="k.kind" />
            {{ k.label }}
          </label>
        </div>
        <label v-if="kind === 'file'" class="grid gap-1 text-[13px]">
          Arquivo .docx, .pdf, .txt ou .md
          <input type="file" :accept="ACCEPTED.join(',')" @change="pick" />
        </label>
        <label v-else-if="kind === 'text'" class="grid gap-1 text-[13px]">
          Texto do playbook
          <textarea v-model="text" class="f min-h-40"></textarea>
        </label>
        <label v-else class="grid gap-1 text-[13px]">
          Link do Google Docs
          <input v-model="url" class="f" type="url" />
        </label>
        <p v-if="error" class="err" role="alert">{{ error }}</p>
        <button class="btn pri justify-self-end" type="submit" :disabled="!ready || sending">Importar</button>
      </form>
    </DialogContent>
  </Dialog>
</template>
