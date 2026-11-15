import { computed, inject, nextTick, ref, watch, type InjectionKey } from 'vue'
import { ApiError, type FieldError } from '@/lib/http'
import { importResultLines } from '@/features/importing/messages'
import { getPlaybook, publishVersion, saveDraft, type Draft, type PlaybookState } from './api'
import { playbookMessages as msg } from './messages'
import { toCents } from './money'

const POLL_MS = 2000

// JSON e não structuredClone: o rascunho é um proxy reativo do Vue, que structuredClone recusa.
const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value))
const same = (a: unknown, b: unknown) => JSON.stringify(a) === JSON.stringify(b)

export const fieldKey = (itemId: string | null, field: string) => `${itemId ?? 'priceTable'}.${field}`

export function isEmptyDraft(d: Draft) {
  return (
    !d.stages.length &&
    !d.discovery.length &&
    !d.objections.length &&
    !d.priceTable.plans.length &&
    !d.priceTable.extraColumnName &&
    d.priceTable.maxDiscount === null
  )
}

export function minCash(d: Draft): string | null {
  const values = d.priceTable.plans.flatMap((p) => (p.cash ? [p.cash] : []))
  if (!values.length) return null
  return values.reduce((min, v) => (toCents(v) < toCents(min) ? v : min))
}

export function createEditor(businessId: string) {
  const state = ref<PlaybookState | null>(null)
  const draft = ref<Draft | null>(null)
  let saved: Draft | null = null
  let revision = 0
  let queued = false
  let lastField: string | null = null
  let watchingImport = false
  let pollTimer: ReturnType<typeof setTimeout> | undefined

  const saving = ref(false)
  const saveFailed = ref(false)
  const failedField = ref<string | null>(null)
  const serverErrors = ref<Record<string, string>>({})
  const localErrors = ref<Record<string, string>>({})
  const conflict = ref(false)
  const editing = ref<string | null>(null)
  const incomplete = ref(new Set<string>())
  const publishError = ref('')
  const importResult = ref<string[]>([])

  const importActive = computed(() => {
    const status = state.value?.importJob?.status
    return status === 'queued' || status === 'running'
  })
  const readOnly = computed(() => importActive.value || conflict.value)
  // Importação ou conflito começando com uma linha aberta: a linha fecha, para nada ser digitado
  // num rascunho que não aceita mais gravação.
  watch(readOnly, (blocked) => {
    if (blocked) editing.value = null
  })
  const errors = computed(() => ({ ...serverErrors.value, ...localErrors.value }))
  const unsaved = computed(
    () => saving.value || saveFailed.value || Object.keys(errors.value).length > 0,
  )

  async function reload() {
    clearTimeout(pollTimer)
    const fresh = await getPlaybook(businessId)
    state.value = fresh
    draft.value = clone(fresh.draft)
    saved = clone(fresh.draft)
    revision = fresh.revision
    const job = fresh.importJob
    if (importActive.value) {
      watchingImport = true
      importResult.value = []
      pollTimer = setTimeout(reload, POLL_MS)
    } else if (job && watchingImport) {
      // Só mostra o resultado de uma importação que esta tela acompanhou.
      watchingImport = false
      importResult.value = importResultLines(job)
    }
  }

  function messageFor(error: FieldError): string {
    if (error.field === 'cash') return msg.cash
    if (error.field === 'installments') return msg.installments
    if (error.field === 'maxDiscount') return msg.discount(minCash(draft.value!) ?? '0.00')
    return msg.tooLong(error.field)
  }

  async function save(key?: string) {
    // Sem campo (acrescentar, remover, reordenar), a recuperação aparece no topo da tela.
    lastField = key ?? null
    if (saving.value) {
      queued = true
      return
    }
    if (readOnly.value || !draft.value || same(draft.value, saved)) return
    saving.value = true
    const sent = clone(draft.value)
    try {
      const result = await saveDraft(businessId, state.value!.playbookId, sent, revision)
      if (same(draft.value, sent)) draft.value = clone(result.draft)
      saved = result.draft
      revision = result.revision
      state.value!.hasUnpublishedChanges = result.hasUnpublishedChanges
      saveFailed.value = false
      failedField.value = null
      serverErrors.value = {}
    } catch (e) {
      if (!(e instanceof ApiError) && !(e instanceof TypeError)) throw e
      if (e instanceof ApiError && e.code === 'revision_conflict') conflict.value = true
      else if (e instanceof ApiError && e.code === 'import_running') await reload()
      else if (e instanceof ApiError && e.status === 422 && e.errors.length) {
        serverErrors.value = Object.fromEntries(e.errors.map((fe) => [fieldKey(fe.itemId, fe.field), messageFor(fe)]))
      } else {
        // Rede ou 5xx: o valor digitado fica na tela para "Tentar de novo" ou "Desfazer".
        saveFailed.value = true
        failedField.value = lastField
      }
    } finally {
      saving.value = false
      if (queued) {
        queued = false
        if (!saveFailed.value) void save()
      }
    }
  }

  function setLocalError(key: string, message: string | null) {
    const next = { ...localErrors.value }
    if (message) next[key] = message
    else delete next[key]
    localErrors.value = next
  }

  function retry() {
    saveFailed.value = false
    void save()
  }

  function undo() {
    draft.value = clone(saved)
    saveFailed.value = false
    failedField.value = null
    serverErrors.value = {}
    localErrors.value = {}
  }

  function focusRow(id: string) {
    void nextTick(() => document.querySelector<HTMLElement>(`[data-row="${id}"] input`)?.focus())
  }

  function startEdit(id: string) {
    if (readOnly.value || editing.value === id) return
    editing.value = id
    focusRow(id)
  }

  function leaveRow(event: FocusEvent, id: string) {
    const row = event.currentTarget as HTMLElement
    const target = event.relatedTarget as HTMLElement | null
    if (editing.value !== id || row.contains(target)) return
    // O foco foi para um campo de outra linha editável, como o título da coluna extra.
    editing.value = target?.closest('[data-row]')?.getAttribute('data-row') ?? null
  }

  /** Grava o texto do campo ao sair dele; `unquote` tira as aspas que a tela mostra em volta das falas. */
  function commitText<T extends object>(item: T, field: keyof T & string, key: string, unquote = false) {
    let text = String(item[field] ?? '').trim()
    if (unquote) text = text.replace(/^["“”]+|["“”]+$/g, '').trim()
    ;(item as Record<string, unknown>)[field] = text
    void save(key)
  }

  function addItem<T extends { id: string }>(list: T[], item: Omit<T, 'id'>) {
    const id = crypto.randomUUID()
    list.push({ ...item, id } as T)
    editing.value = id
    focusRow(id)
    void save()
  }

  function removeItem<T extends { id: string }>(list: T[], id: string) {
    list.splice(
      list.findIndex((i) => i.id === id),
      1,
    )
    // Erro de um campo da linha removida não pode continuar bloqueando Publicar e Importar.
    const ofOtherRows = (errors: Record<string, string>) =>
      Object.fromEntries(Object.entries(errors).filter(([key]) => !key.startsWith(`${id}.`)))
    localErrors.value = ofOtherRows(localErrors.value)
    serverErrors.value = ofOtherRows(serverErrors.value)
    void save()
  }

  async function publish() {
    if (unsaved.value || readOnly.value) return
    publishError.value = ''
    try {
      await publishVersion(businessId, state.value!.playbookId, revision)
    } catch (e) {
      if (!(e instanceof ApiError) && !(e instanceof TypeError)) throw e
      const code = e instanceof ApiError ? e.code : ''
      if (code === 'revision_conflict') conflict.value = true
      else if (code === 'nothing_to_publish' || code === 'import_running') await reload()
      else if (code === 'incomplete_rows' && e instanceof ApiError) {
        incomplete.value = new Set(e.errors.flatMap((fe) => (fe.itemId ? [fe.itemId] : [])))
        publishError.value = msg.incompleteRows
      } else if (code === 'publish_minimum') publishError.value = msg.publishMinimum
      else if (code === 'pending_review') publishError.value = msg.pendingReview
      else if (code === 'discount_invalid') publishError.value = msg.discount(minCash(draft.value!) ?? '0.00')
      else publishError.value = msg.publishFailed
      return
    }
    incomplete.value = new Set()
    await reload()
  }

  return {
    state,
    draft,
    saving,
    saveFailed,
    failedField,
    errors,
    conflict,
    editing,
    incomplete,
    publishError,
    importResult,
    importActive,
    readOnly,
    unsaved,
    reload,
    save,
    setLocalError,
    retry,
    undo,
    startEdit,
    leaveRow,
    commitText,
    addItem,
    removeItem,
    publish,
    dispose: () => clearTimeout(pollTimer),
  }
}

export type Editor = ReturnType<typeof createEditor>

export const editorKey: InjectionKey<Editor> = Symbol('editor')

export function useEditor(): Editor {
  const editor = inject(editorKey)
  if (!editor) throw new Error('useEditor fora da tela do playbook')
  return editor
}
