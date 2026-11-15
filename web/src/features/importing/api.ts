import { sendJson } from '@/lib/http'
import { playbookUrl } from '@/features/playbook/api'

export type ImportSource = { file: File } | { text: string } | { googleDocsUrl: string }

export function startImport(businessId: string, playbookId: string, source: ImportSource) {
  const url = `${playbookUrl(businessId)}/imports`
  if ('file' in source) {
    const form = new FormData()
    form.append('playbookId', playbookId)
    form.append('file', source.file)
    return sendJson<{ jobId: string }>('POST', url, form)
  }
  return sendJson<{ jobId: string }>('POST', url, { playbookId, ...source })
}
