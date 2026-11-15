import type { ImportJob } from '@/features/playbook/api'

export const importErrorMessages: Record<string, string> = {
  unsupported_format: 'Formato não aceito. Envie .docx, .pdf, .txt ou .md.',
  file_too_large: 'O arquivo passa de 10 MB.',
  text_too_long: 'O documento é longo demais para importar (máximo de 100 mil caracteres).',
  pdf_without_text: 'Este PDF não tem texto selecionável. Envie o documento em .docx ou cole o texto.',
  google_docs_unavailable:
    'Não conseguimos abrir o documento. No Google Docs, compartilhe como "Qualquer pessoa com o link" e tente de novo.',
  nothing_found: 'Não encontramos etapas, perguntas, objeções nem preços neste documento.',
  model_failed: 'A importação falhou. Nada foi alterado no rascunho. Tente de novo.',
}

export const importSendFailed = 'Não foi possível enviar a importação. Confira a origem e tente de novo.'

export function importResultLines(job: ImportJob): string[] {
  if (job.status === 'failed') {
    return [importErrorMessages[job.errorCode ?? ''] ?? importErrorMessages.model_failed!]
  }
  const c = job.counts ?? { stages: 0, discovery: 0, objections: 0, plans: 0 }
  return [
    `Importado: ${c.stages} etapas, ${c.discovery} informações, ${c.objections} objeções, ${c.plans} planos.`,
    ...job.notices,
    ...(job.skipped > 0 ? [`${job.skipped} itens ficaram de fora pelo limite do bloco.`] : []),
  ]
}
