import { cleanup, screen, waitFor, within } from '@testing-library/vue'
import { describe, expect, it, vi } from 'vitest'
import { fakeApi, ok, problem, renderApp, type Call, type Reply } from '@/__tests__/harness'
import type { Draft, ImportJob, PlaybookState } from '@/features/playbook/api'
import { PLAYBOOK_URL, playbookServer, playbookState, sampleDraft } from '@/features/playbook/__tests__/fixtures'

const IMPORTS = `POST ${PLAYBOOK_URL}/imports`

const job = (over: Partial<ImportJob>): ImportJob => ({
  id: 'j1',
  status: 'running',
  counts: null,
  skipped: 0,
  notices: [],
  errorCode: null,
  ...over,
})

type Route = Reply | ((call: Call) => Reply)

async function openPlaybook(state: PlaybookState, extra: Record<string, Route> = {}, applyAccept = true) {
  const { server, routes } = playbookServer(state)
  const calls = fakeApi({ ...routes, ...extra })
  const app = await renderApp('/n/b1/playbook', { applyAccept })
  await screen.findByText(/^Playbook: /)
  return { ...app, server, calls }
}

/** Abre a tela com a importação rodando e avança o relógio até a consulta seguinte, que já vê `finished`. */
async function finishImport(finished: ImportJob) {
  vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'], shouldAdvanceTime: true })
  const { server } = await openPlaybook(playbookState({ importJob: job({ status: 'running' }) }))
  expect(screen.getByText('Importando…')).toBeTruthy()
  server.state.importJob = finished
  await vi.advanceTimersByTimeAsync(2000)
  await waitFor(() => expect(screen.queryByText('Importando…')).toBeNull())
}

const button = (name: string) => screen.getByRole('button', { name }) as HTMLButtonElement

describe('importação', () => {
  it('[C65] Importar documento oferece arquivo, texto colado e link do Google Docs', async () => {
    const { user } = await openPlaybook(playbookState())

    await user.click(button('Importar documento'))
    const dialog = within(await screen.findByRole('dialog'))

    expect(dialog.getByLabelText('Arquivo').getAttribute('type')).toBe('radio')
    expect(dialog.getByLabelText('Arquivo .docx, .pdf, .txt ou .md').getAttribute('accept')).toBe(
      '.docx,.pdf,.txt,.md',
    )
    await user.click(dialog.getByLabelText('Texto colado'))
    expect(dialog.getByLabelText('Texto do playbook').tagName).toBe('TEXTAREA')
    await user.click(dialog.getByLabelText('Link do Google Docs', { selector: '[type=radio]' }))
    expect(dialog.getByLabelText('Link do Google Docs', { selector: '[type=url]' })).toBeTruthy()
  })

  it('[C66b] envio mostra Importando com rascunho só leitura e botões desabilitados', async () => {
    const holder: { server?: { state: PlaybookState } } = {}
    const { user, server, calls } = await openPlaybook(playbookState(), {
      [IMPORTS]: () => {
        holder.server!.state.importJob = job({ status: 'queued' })
        return ok({ jobId: 'j1' }, 202)
      },
    })
    holder.server = server

    await user.click(button('Importar documento'))
    const dialog = within(await screen.findByRole('dialog'))
    await user.click(dialog.getByLabelText('Texto colado'))
    await user.type(dialog.getByLabelText('Texto do playbook'), 'Etapa 1: abertura. Kit 3 kWp por R$ 4.500.')
    await user.click(dialog.getByRole('button', { name: 'Importar' }))

    expect(await screen.findByText('Importando…')).toBeTruthy()
    const post = calls.find((c) => c.method === 'POST')!
    expect(post.body).toEqual({ playbookId: 'pb1', text: 'Etapa 1: abertura. Kit 3 kWp por R$ 4.500.' })
    expect(button('Importar documento').disabled).toBe(true)
    expect(button('Publicar').disabled).toBe(true)
    expect(button('+ Etapa').disabled).toBe(true)
    await user.click(screen.getByText('Abertura'))
    expect(screen.queryByLabelText('Nome da etapa')).toBeNull()
  })

  it('[C67b] tela aberta com tarefa ativa mostra Importando', async () => {
    await openPlaybook(playbookState({ importJob: job({ status: 'queued' }) }))

    expect(screen.getByText('Importando…')).toBeTruthy()
    expect(button('Publicar').disabled).toBe(true)
  })

  it('[C68b] importação concluída mostra Importado com as contagens', async () => {
    await finishImport(job({ status: 'done', counts: { stages: 2, discovery: 3, objections: 1, plans: 1 } }))

    expect(screen.getByText('Importado: 2 etapas, 3 informações, 1 objeções, 1 planos.')).toBeTruthy()
  })

  it('[C69b] valor marcado mostra Importado · conferir e o trecho', async () => {
    const draft: Draft = sampleDraft()
    draft.priceTable.plans[0]!.review = { status: 'pending', excerpt: 'Kit 3 kWp: R$ 4.500 à vista' }
    draft.priceTable.maxDiscount = '300.00'
    draft.priceTable.maxDiscountReview = { status: 'pending', excerpt: 'desconto de até R$ 300' }
    await openPlaybook(playbookState({ draft }))

    expect(screen.getAllByText('Importado · conferir')).toHaveLength(2)
    expect(screen.getByText('Kit 3 kWp: R$ 4.500 à vista')).toBeTruthy()
    expect(screen.getByText('desconto de até R$ 300')).toBeTruthy()
    const kit5 = screen.getByText('Kit 5 kWp').closest('tr')!
    expect(within(kit5).queryByText('Importado · conferir')).toBeNull()
  })

  it('[C70] Conferido ou edição tira a marca do item', async () => {
    const draft: Draft = sampleDraft()
    draft.priceTable.plans[0]!.review = { status: 'pending', excerpt: 'Kit 3 kWp: R$ 4.500 à vista' }
    draft.priceTable.maxDiscount = '300.00'
    draft.priceTable.maxDiscountReview = { status: 'pending', excerpt: 'desconto de até R$ 300' }
    const { user, server } = await openPlaybook(playbookState({ draft }))

    const kit3 = screen.getByText('Kit 3 kWp').closest('tr')!
    await user.click(within(kit3).getByRole('button', { name: 'Conferido' }))
    await waitFor(() => expect(server.state.draft.priceTable.plans[0]!.review).toBeNull())
    expect(within(kit3).queryByText('Importado · conferir')).toBeNull()

    await user.click(screen.getByText('R$ 300'))
    await user.clear(screen.getByLabelText('Desconto máximo na call'))
    await user.type(screen.getByLabelText('Desconto máximo na call'), '250')
    await user.click(document.body)
    await waitFor(() => expect(server.state.draft.priceTable.maxDiscountReview).toBeNull())
    expect(server.state.draft.priceTable.maxDiscount).toBe('250.00')
    expect(screen.queryByText('Importado · conferir')).toBeNull()
  })

  it('[C73b] avisos da importação aparecem na mensagem do resultado', async () => {
    const notice = 'O documento traz desconto máximo de R$ 500; o atual (R$ 300) foi mantido.'
    await finishImport(
      job({ status: 'done', counts: { stages: 1, discovery: 0, objections: 0, plans: 2 }, notices: [notice], skipped: 2 }),
    )

    expect(screen.getByText('Importado: 1 etapas, 0 informações, 0 objeções, 2 planos.')).toBeTruthy()
    expect(screen.getByText(notice)).toBeTruthy()
    expect(screen.getByText('2 itens ficaram de fora pelo limite do bloco.')).toBeTruthy()
  })

  it('[C74b] mensagens da importação por código de erro', async () => {
    const failed: Record<string, string> = {
      text_too_long: 'O documento é longo demais para importar (máximo de 100 mil caracteres).',
      pdf_without_text: 'Este PDF não tem texto selecionável. Envie o documento em .docx ou cole o texto.',
      google_docs_unavailable:
        'Não conseguimos abrir o documento. No Google Docs, compartilhe como "Qualquer pessoa com o link" e tente de novo.',
      nothing_found: 'Não encontramos etapas, perguntas, objeções nem preços neste documento.',
      model_failed: 'A importação falhou. Nada foi alterado no rascunho. Tente de novo.',
    }
    for (const [errorCode, text] of Object.entries(failed)) {
      await finishImport(job({ status: 'failed', errorCode }))
      expect(screen.getByText(text)).toBeTruthy()
      cleanup()
      vi.useRealTimers()
    }

    const sent: Record<string, [string, Reply, string]> = {
      unsupported_format: ['notas.txt', problem(415, 'unsupported_format'), 'Formato não aceito. Envie .docx, .pdf, .txt ou .md.'],
      file_too_large: ['playbook.pdf', problem(413, 'file_too_large'), 'O arquivo passa de 10 MB.'],
      client_format: ['playbook.odt', ok({ jobId: 'j1' }, 202), 'Formato não aceito. Envie .docx, .pdf, .txt ou .md.'],
    }
    for (const [fileName, reply, text] of Object.values(sent)) {
      const { user, calls } = await openPlaybook(playbookState(), { [IMPORTS]: reply }, false)
      await user.click(button('Importar documento'))
      const dialog = within(await screen.findByRole('dialog'))
      await user.upload(dialog.getByLabelText('Arquivo .docx, .pdf, .txt ou .md'), new File(['conteúdo'], fileName))
      await user.click(dialog.getByRole('button', { name: 'Importar' }))

      expect(await dialog.findByText(text)).toBeTruthy()
      const posted = calls.filter((c) => c.method === 'POST')
      expect(posted).toHaveLength(fileName.endsWith('.odt') ? 0 : 1)
      cleanup()
    }
  })
})
