import { cleanup, screen, waitFor, within } from '@testing-library/vue'
import { describe, expect, it, vi } from 'vitest'
import { fakeApi, ok, problem, renderApp, type Call, type Reply } from '@/__tests__/harness'
import type { Draft, PlaybookState } from '../api'
import { formatCash, formatInstallments, parseMoney } from '../money'
import { emptyDraft, PLAYBOOK_URL, playbookServer, playbookState, sampleDraft } from './fixtures'

type Route = Reply | ((call: Call) => Reply | Promise<Reply>)

async function openPlaybook(state: PlaybookState = playbookState(), extra: Record<string, Route> = {}) {
  const { server, routes } = playbookServer(state)
  const calls = fakeApi({ ...routes, ...extra })
  const app = await renderApp('/n/b1/playbook')
  await screen.findByText(/^Playbook: /)
  return { ...app, server, calls }
}

const puts = (calls: Call[]) =>
  calls.filter((c) => c.method === 'PUT').map((c) => c.body as { playbookId: string; draft: Draft; revision: number })
const last = <T>(items: T[]): T => items[items.length - 1]!
const lastDraft = (calls: Call[]) => last(puts(calls)).draft
const block = (name: string) => within(screen.getByRole('region', { name }))
const stageNames = () =>
  block('Etapas da call')
    .getAllByRole('listitem')
    .map((li) => li.textContent!.replace(/↑↓Remover$/, ''))
const headers = (region: string) => block(region).getAllByRole('columnheader').map((th) => th.textContent!.trim())
const discountLine = () => screen.getByText(/Desconto máximo na call:/).closest('[data-row="discount"]')!.textContent!
const withDraft = (change: (d: Draft) => void, over: Partial<PlaybookState> = {}) => {
  const draft = sampleDraft()
  change(draft)
  return playbookState({ draft, ...over })
}

describe('tela do playbook', () => {
  it('[C23] menu com cinco itens na ordem e Disponível em breve', async () => {
    const { user } = await openPlaybook()
    const menu = within(screen.getByRole('navigation', { name: 'Menu' }))
    const items = ['Nova call', 'Ao vivo', 'Calls', 'Coach', 'Playbook'].map((label) => menu.getByText(label).closest('a')!)

    const order = menu.getAllByText(/^(Nova call|Ao vivo|Calls|Coach|Playbook)$/).map((el) => el.textContent)
    expect(order).toEqual(['Nova call', 'Ao vivo', 'Calls', 'Coach', 'Playbook'])
    expect(items.slice(0, 4).map((a) => a.getAttribute('title'))).toEqual(Array(4).fill('Disponível em breve'))
    expect(items[4]!.classList.contains('on')).toBe(true)
    expect(items[4]!.getAttribute('title')).toBeNull()

    expect(screen.queryByRole('status')).toBeNull()
    await user.click(menu.getByText('Ao vivo'))
    expect(screen.getByRole('status').textContent).toBe('Disponível em breve')
  })

  it('[C27] rascunho vazio mostra título, subtítulo e os dois jeitos de começar', async () => {
    await openPlaybook(playbookState({ draft: emptyDraft(), hasUnpublishedChanges: false }))

    expect(screen.getByRole('heading', { name: 'Playbook' })).toBeTruthy()
    expect(screen.getByText('Cada negócio cadastra o próprio jeito de vender, sem programar.')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Escrever o playbook' })).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Importar um documento' })).toBeTruthy()
  })

  it('[C28] Escrever o playbook mostra os quatro blocos vazios com os botões', async () => {
    const { user } = await openPlaybook(playbookState({ draft: emptyDraft(), hasUnpublishedChanges: false }))

    await user.click(screen.getByRole('button', { name: 'Escrever o playbook' }))

    for (const [region, button] of [
      ['Etapas da call', '+ Etapa'],
      ['O que precisa ser descoberto na call', '+ Informação'],
      ['Objeções e sugestões de fala', '+ Objeção'],
      ['Tabela de preços', '+ Plano'],
    ] as const) {
      expect(block(region).getByRole('heading', { name: region })).toBeTruthy()
      expect(block(region).getByRole('button', { name: button })).toBeTruthy()
    }
    expect(screen.queryByText('Abertura')).toBeNull()
  })

  it('[C30] + Etapa acrescenta ao fim numerada e salva ao sair do campo', async () => {
    const { user, calls } = await openPlaybook()

    await user.click(screen.getByRole('button', { name: '+ Etapa' }))
    await user.type(screen.getByLabelText('Nome da etapa'), 'Visita técnica')
    await user.click(document.body)

    await waitFor(() => expect(lastDraft(calls).stages.map((stage) => stage.name)).toEqual([
      'Abertura',
      'Diagnóstico da conta de luz',
      'Proposta',
      'Visita técnica',
    ]))
    expect(last(stageNames())).toBe('4Visita técnica')
  })

  it('[C31] setas trocam a etapa, renumeram, salvam e desabilitam nas pontas', async () => {
    const { user, calls } = await openPlaybook()
    const up = () => block('Etapas da call').getAllByRole('button', { name: 'Subir etapa' }) as HTMLButtonElement[]
    const down = () => block('Etapas da call').getAllByRole('button', { name: 'Descer etapa' }) as HTMLButtonElement[]

    expect(up()[0]!.disabled).toBe(true)
    expect(last(down()).disabled).toBe(true)
    expect(up()[1]!.disabled).toBe(false)

    await user.click(down()[0]!)

    expect(stageNames()).toEqual(['1Diagnóstico da conta de luz', '2Abertura', '3Proposta'])
    await waitFor(() => expect(lastDraft(calls).stages.map((stage) => stage.id)).toEqual(['s2', 's1', 's3']))
  })

  it('[C32] + Informação acrescenta a linha ao fim do bloco', async () => {
    const { user, calls } = await openPlaybook()

    await user.click(screen.getByRole('button', { name: '+ Informação' }))
    await user.type(screen.getByLabelText('Informação'), 'Quem decide')
    await user.type(screen.getByLabelText('Pergunta sugerida'), 'Mais alguém participa da decisão?')
    await user.click(document.body)

    const rows = block('O que precisa ser descoberto na call').getAllByRole('row')
    expect(last(rows).textContent).toContain('Quem decide')
    await waitFor(() =>
      expect(last(lastDraft(calls).discovery)).toMatchObject({
        information: 'Quem decide',
        question: 'Mais alguém participa da decisão?',
      }),
    )
    expect(lastDraft(calls).discovery).toHaveLength(3)
  })

  it('[C33] + Objeção acrescenta a linha com os três campos', async () => {
    const { user, calls } = await openPlaybook()

    await user.click(screen.getByRole('button', { name: '+ Objeção' }))
    expect(screen.getByLabelText('Como o lead costuma dizer')).toBeTruthy()
    await user.type(screen.getByLabelText('Objeção'), 'Vai mudar de casa')
    await user.type(screen.getByLabelText('Sugestão de fala'), 'O sistema pode ir junto com você.')
    await user.click(document.body)

    await waitFor(() =>
      expect(last(lastDraft(calls).objections)).toMatchObject({
        name: 'Vai mudar de casa',
        leadSays: '',
        approvedResponse: 'O sistema pode ir junto com você.',
      }),
    )
    expect(lastDraft(calls).objections).toHaveLength(2)
  })

  it('[C34] + Plano acrescenta a linha ao fim da tabela', async () => {
    const { user, calls } = await openPlaybook()

    await user.click(screen.getByRole('button', { name: '+ Plano' }))
    for (const label of ['Plano', 'Painéis', 'À vista', 'Número de parcelas', 'Valor da parcela']) {
      expect(screen.getByLabelText(label)).toBeTruthy()
    }
    await user.type(screen.getByLabelText('Plano'), 'Kit 8 kWp')
    await user.type(screen.getByLabelText('Painéis'), '16')
    await user.type(screen.getByLabelText('À vista'), '12.000')
    await user.type(screen.getByLabelText('Número de parcelas'), '12')
    await user.type(screen.getByLabelText('Valor da parcela'), '1.200')
    await user.click(document.body)

    await waitFor(() =>
      expect(last(lastDraft(calls).priceTable.plans)).toMatchObject({
        name: 'Kit 8 kWp',
        extraValue: '16',
        cash: '12000.00',
        installments: { count: 12, value: '1200.00' },
      }),
    )
    expect(last(block('Tabela de preços').getAllByRole('row')).textContent).toContain('R$ 12.000')
  })

  it('[C35] clicar numa linha edita no lugar e salva cada campo ao sair', async () => {
    const { user, calls } = await openPlaybook()

    await user.click(screen.getByText('Consumo mensal'))
    const information = screen.getByLabelText('Informação') as HTMLInputElement
    const question = screen.getByLabelText('Pergunta sugerida') as HTMLInputElement
    expect(information.value).toBe('Consumo mensal')
    expect(question.value).toBe('Quanto vem a sua conta de luz por mês?')

    await user.clear(information)
    await user.type(information, 'Consumo em kWh')
    await user.tab()
    await waitFor(() => expect(lastDraft(calls).discovery[0]!.information).toBe('Consumo em kWh'))

    await user.clear(question)
    await user.type(question, 'Quantos kWh você gasta por mês?')
    await user.click(document.body)
    await waitFor(() => expect(lastDraft(calls).discovery[0]!.question).toBe('Quantos kWh você gasta por mês?'))
    expect(puts(calls)).toHaveLength(2)
  })

  it('[C36] Remover tira a linha sem confirmação e renumera as etapas', async () => {
    const confirm = vi.fn<() => boolean>(() => true)
    vi.stubGlobal('confirm', confirm)
    const { user, calls } = await openPlaybook()

    await user.click(block('Etapas da call').getAllByRole('button', { name: 'Remover' })[0]!)

    expect(stageNames()).toEqual(['1Diagnóstico da conta de luz', '2Proposta'])
    expect(confirm).not.toHaveBeenCalled()
    await waitFor(() => expect(lastDraft(calls).stages.map((stage) => stage.id)).toEqual(['s2', 's3']))
  })

  it('[C37b] erro de limite mostra Máximo de N caracteres e mantém o valor salvo', async () => {
    const tooLong = 'a'.repeat(121)
    const { user, server } = await openPlaybook(playbookState(), {
      [`PUT ${PLAYBOOK_URL}/draft`]: problem(422, 'invalid_draft', [
        { itemId: 's1', field: 'name', code: 'too_long', message: 'too long' },
      ]),
    })

    await user.click(screen.getByText('Abertura'))
    await user.clear(screen.getByLabelText('Nome da etapa'))
    await user.type(screen.getByLabelText('Nome da etapa'), tooLong)
    await user.tab()

    expect(await screen.findByText('Máximo de 120 caracteres.')).toBeTruthy()
    expect(server.state.draft.stages[0]!.name).toBe('Abertura')
  })

  it('[C38b] bloco no limite desabilita o botão e mostra Limite de N itens', async () => {
    const state = withDraft((d) => {
      d.stages = Array.from({ length: 30 }, (_, i) => ({ id: `s${i}`, name: `Etapa ${i + 1}` }))
    })
    await openPlaybook(state)

    expect((screen.getByRole('button', { name: '+ Etapa' }) as HTMLButtonElement).disabled).toBe(true)
    expect(screen.getByText('Limite de 30 itens neste bloco.')).toBeTruthy()
    expect((screen.getByRole('button', { name: '+ Objeção' }) as HTMLButtonElement).disabled).toBe(false)
    cleanup()

    await openPlaybook(
      withDraft((d) => {
        d.discovery = Array.from({ length: 50 }, (_, i) => ({ id: `d${i}`, information: `I${i}`, question: 'Q' }))
        d.objections = Array.from({ length: 50 }, (_, i) => ({ id: `o${i}`, name: `O${i}`, leadSays: '', approvedResponse: 'R' }))
        d.priceTable.plans = Array.from({ length: 20 }, (_, i) => ({
          id: `p${i}`, name: `P${i}`, extraValue: '', cash: '100.00', installments: null, review: null,
        }))
      }),
    )
    for (const [button, limit] of [['+ Informação', 50], ['+ Objeção', 50], ['+ Plano', 20]] as const) {
      expect((screen.getByRole('button', { name: button }) as HTMLButtonElement).disabled).toBe(true)
      expect(screen.getAllByText(`Limite de ${limit} itens neste bloco.`).length).toBeGreaterThan(0)
    }
  })

  it('[C39] textos aparecem entre aspas e são guardados sem aspas', async () => {
    const { user, calls } = await openPlaybook()

    expect(screen.getByText('"Quanto vem a sua conta de luz por mês?"')).toBeTruthy()
    expect(screen.getByText('"Achei caro"')).toBeTruthy()
    expect(screen.getByText('"Quanto você paga de luz em um ano?"')).toBeTruthy()

    await user.click(screen.getByText('Consumo mensal'))
    const question = screen.getByLabelText('Pergunta sugerida')
    await user.clear(question)
    await user.type(question, '"Qual o valor da sua conta?"')
    await user.click(document.body)

    await waitFor(() => expect(lastDraft(calls).discovery[0]!.question).toBe('Qual o valor da sua conta?'))
    expect(screen.getByText('"Qual o valor da sua conta?"')).toBeTruthy()
  })

  it('[C40] falha ao salvar mantém o valor e oferece Tentar de novo e Desfazer', async () => {
    let reply: Reply = problem(503, '')
    const { server, calls, user } = await openPlaybook(playbookState(), {
      [`PUT ${PLAYBOOK_URL}/draft`]: (call) => (reply === 'network' || reply.status >= 500 ? reply : acceptLater(call)),
    })
    const acceptLater = (call: Call) => {
      const body = call.body as { draft: Draft }
      server.state.draft = body.draft
      return ok({ draft: body.draft, revision: 8, hasUnpublishedChanges: true })
    }

    await user.click(screen.getByText('Abertura'))
    await user.clear(screen.getByLabelText('Nome da etapa'))
    await user.type(screen.getByLabelText('Nome da etapa'), 'Boas-vindas')
    await user.click(document.body)

    expect(await screen.findByText('Não foi possível salvar. Tente de novo.')).toBeTruthy()
    expect(screen.getByText('Boas-vindas')).toBeTruthy()
    reply = 'network'
    await user.click(screen.getByRole('button', { name: 'Tentar de novo' }))
    expect(await screen.findByText('Não foi possível salvar. Tente de novo.')).toBeTruthy()

    reply = ok()
    await user.click(screen.getByRole('button', { name: 'Tentar de novo' }))
    await waitFor(() => expect(server.state.draft.stages[0]!.name).toBe('Boas-vindas'))
    expect(screen.queryByText('Não foi possível salvar. Tente de novo.')).toBeNull()

    reply = 'network'
    await user.click(screen.getByText('Boas-vindas'))
    await user.clear(screen.getByLabelText('Nome da etapa'))
    await user.type(screen.getByLabelText('Nome da etapa'), 'Recepção')
    await user.click(document.body)
    await user.click(await screen.findByRole('button', { name: 'Desfazer' }))

    expect(screen.getByText('Boas-vindas')).toBeTruthy()
    expect(screen.queryByText('Recepção')).toBeNull()
    expect(last(puts(calls)).draft.stages[0]!.name).toBe('Recepção')
  })

  it('[C41b] revision_conflict mostra O playbook foi alterado em outra aba', async () => {
    const { user } = await openPlaybook(playbookState(), {
      [`PUT ${PLAYBOOK_URL}/draft`]: problem(409, 'revision_conflict'),
    })

    await user.click(screen.getByText('Abertura'))
    await user.type(screen.getByLabelText('Nome da etapa'), ' do contato')
    await user.click(document.body)

    expect(await screen.findByText('O playbook foi alterado em outra aba. Recarregue para continuar.')).toBeTruthy()
  })

  it('[C42b] importação ativa deixa o rascunho só para leitura', async () => {
    const running = { id: 'j1', status: 'running' as const, counts: null, skipped: 0, notices: [], errorCode: null }
    const { user, calls } = await openPlaybook(playbookState({ importJob: running }))

    expect(screen.getByText('Importando…')).toBeTruthy()
    await user.click(screen.getByText('Abertura'))
    expect(screen.queryByLabelText('Nome da etapa')).toBeNull()
    for (const name of ['+ Etapa', '+ Informação', '+ Objeção', '+ Plano']) {
      expect((screen.getByRole('button', { name }) as HTMLButtonElement).disabled).toBe(true)
    }
    expect(screen.getAllByRole('button', { name: 'Remover' }).every((b) => (b as HTMLButtonElement).disabled)).toBe(true)
    expect(puts(calls)).toHaveLength(0)
  })

  it('[C44] nome da coluna extra vira o título entre Plano e À vista', async () => {
    await openPlaybook()

    expect(headers('Tabela de preços').slice(0, 4)).toEqual(['Plano', 'Painéis', 'À vista', 'Parcelado'])
  })

  it('[C45] coluna extra vazia some fora da edição', async () => {
    const state = withDraft((d) => {
      d.priceTable.extraColumnName = ''
      for (const plan of d.priceTable.plans) plan.extraValue = ''
    })
    const { user } = await openPlaybook(state)

    expect(headers('Tabela de preços').slice(0, 3)).toEqual(['Plano', 'À vista', 'Parcelado'])
    await user.click(screen.getByText('Kit 3 kWp'))
    expect(screen.getByLabelText('Nome da coluna extra')).toBeTruthy()
  })

  it('[C46b] campo à vista converte 4.500,50 e recusa abc com a mensagem', async () => {
    const { user, calls } = await openPlaybook()

    await user.click(screen.getByText('Kit 3 kWp'))
    const cash = screen.getByLabelText('À vista')
    await user.clear(cash)
    await user.type(cash, '4.500,50')
    await user.click(document.body)

    await waitFor(() => expect(lastDraft(calls).priceTable.plans[0]!.cash).toBe('4500.50'))
    expect(screen.getByText('R$ 4.500,50')).toBeTruthy()

    await user.click(screen.getByText('Kit 3 kWp'))
    await user.clear(screen.getByLabelText('À vista'))
    await user.type(screen.getByLabelText('À vista'), 'abc')
    await user.click(document.body)

    expect(screen.getByText('Informe um valor em reais maior que zero.')).toBeTruthy()
    expect(puts(calls)).toHaveLength(1)
  })

  it('[C47b] parcelado inválido mostra a mensagem e vazio aparece como travessão', async () => {
    const { user, calls } = await openPlaybook()
    const kit5 = screen.getByText('Kit 5 kWp').closest('tr')!
    expect(within(kit5).getByText('—')).toBeTruthy()

    await user.click(screen.getByText('Kit 3 kWp'))
    await user.clear(screen.getByLabelText('Número de parcelas'))
    await user.type(screen.getByLabelText('Número de parcelas'), '61')
    await user.click(document.body)

    expect(screen.getByText('Parcelas de 2 a 60, com valor maior que zero.')).toBeTruthy()
    expect(puts(calls)).toHaveLength(0)
  })

  it('[C48] formata R$ 4.500, 6x 1.166,67 e 3x 1.500', () => {
    expect(formatCash('4500.00')).toBe('R$ 4.500')
    expect(formatInstallments({ count: 6, value: '1166.67' })).toBe('6x 1.166,67')
    expect(formatInstallments({ count: 3, value: '1500.00' })).toBe('3x 1.500')
  })

  it('lê valores em reais digitados em pt-BR', () => {
    expect(parseMoney('4500')).toBe('4500.00')
    expect(parseMoney('4.500,50')).toBe('4500.50')
    expect(parseMoney('R$ 4.500,5')).toBe('4500.50')
    expect(parseMoney('10.000.000,00')).toBe('10000000.00')
    for (const invalid of ['0', 'abc', '10.000.000,01', '4.500,505', '-10', '45.00', '']) {
      expect(parseMoney(invalid)).toBeNull()
    }
    expect(parseMoney('0', { allowZero: true })).toBe('0.00')
  })

  it('[C49] desconto informado aparece como Desconto máximo na call R$ valor', async () => {
    const { user, calls } = await openPlaybook()

    await user.click(screen.getByText('a definir'))
    await user.type(screen.getByLabelText('Desconto máximo na call'), '300')
    await user.click(document.body)

    await waitFor(() => expect(lastDraft(calls).priceTable.maxDiscount).toBe('300.00'))
    expect(discountLine()).toContain('Desconto máximo na call: R$ 300')
  })

  it('[C50] sem desconto mostra o selo a definir', async () => {
    await openPlaybook()

    const badge = screen.getByText('a definir')
    expect(badge.classList.contains('amb')).toBe(true)
    expect(discountLine()).toContain('Desconto máximo na call:')
  })

  it('[C51b] desconto inválido mostra a mensagem com o menor à vista', async () => {
    const { user, calls } = await openPlaybook()

    await user.click(screen.getByText('a definir'))
    await user.type(screen.getByLabelText('Desconto máximo na call'), '4.500')
    await user.click(document.body)

    expect(
      screen.getByText('O desconto máximo precisa ser menor que o menor preço à vista (R$ 4.500).'),
    ).toBeTruthy()
    expect(puts(calls)).toHaveLength(0)
  })

  it('[C52] sem plano a linha do desconto não aparece', async () => {
    await openPlaybook(withDraft((d) => (d.priceTable.plans = [])))

    expect(screen.queryByText(/Desconto máximo na call/)).toBeNull()
  })

  it('[C53] sem call registrada a coluna Nas calls não aparece', async () => {
    await openPlaybook(playbookState({ hasCalls: false }))

    expect(headers('Objeções e sugestões de fala')).not.toContain('Nas calls')
    expect(headers('Objeções e sugestões de fala').slice(0, 3)).toEqual([
      'Objeção',
      'Como o lead costuma dizer',
      'Sugestão de fala',
    ])
  })

  it('[C54b] após publicar o topo mostra a versão nova', async () => {
    const { user, server, calls } = await openPlaybook(playbookState(), {
      [`POST ${PLAYBOOK_URL}/versions`]: () => {
        server.state.latestVersion = { number: 1, publishedAt: '2026-11-15T12:00:00Z' }
        server.state.hasUnpublishedChanges = false
        return ok({ number: 1 }, 201)
      },
    })
    expect(screen.getByText('Playbook: Vendas Solar · rascunho')).toBeTruthy()

    await user.click(screen.getByRole('button', { name: 'Publicar' }))

    expect(await screen.findByText('Playbook: Vendas Solar · versão 1')).toBeTruthy()
    expect(calls.find((c) => c.method === 'POST')!.body).toEqual({ playbookId: 'pb1', revision: 7 })
  })

  it('[C55b] publicar com revision_conflict mostra O playbook foi alterado em outra aba', async () => {
    const { user } = await openPlaybook(playbookState(), {
      [`POST ${PLAYBOOK_URL}/versions`]: problem(409, 'revision_conflict'),
    })

    await user.click(screen.getByRole('button', { name: 'Publicar' }))

    expect(await screen.findByText('O playbook foi alterado em outra aba. Recarregue para continuar.')).toBeTruthy()
  })

  it('[C56b] publish_minimum mostra a mensagem do mínimo', async () => {
    const { user } = await openPlaybook(playbookState(), {
      [`POST ${PLAYBOOK_URL}/versions`]: problem(422, 'publish_minimum'),
    })

    await user.click(screen.getByRole('button', { name: 'Publicar' }))

    expect(
      await screen.findByText(
        'Para publicar, cadastre ao menos uma etapa, uma informação a descobrir e uma objeção.',
      ),
    ).toBeTruthy()
  })

  it('[C57b] incomplete_rows destaca as linhas e mostra Complete ou remova', async () => {
    const { user } = await openPlaybook(playbookState(), {
      [`POST ${PLAYBOOK_URL}/versions`]: problem(422, 'incomplete_rows', [
        { itemId: 's2', field: 'name', code: 'required', message: 'required' },
        { itemId: 'p2', field: 'cash', code: 'required', message: 'required' },
      ]),
    })

    await user.click(screen.getByRole('button', { name: 'Publicar' }))

    expect(await screen.findByText('Complete ou remova as linhas destacadas.')).toBeTruthy()
    const highlighted = [...document.querySelectorAll('[data-incomplete]')].map((el) => el.getAttribute('data-row'))
    expect(highlighted).toEqual(['s2', 'p2'])
  })

  it('[C59b] pending_review mostra Confira os valores marcados', async () => {
    const { user } = await openPlaybook(playbookState(), {
      [`POST ${PLAYBOOK_URL}/versions`]: problem(422, 'pending_review'),
    })

    await user.click(screen.getByRole('button', { name: 'Publicar' }))

    expect(
      await screen.findByText('Confira os valores marcados "Importado · conferir" antes de publicar.'),
    ).toBeTruthy()
  })

  it('[C60] alteração pendente desabilita Publicar com Há alterações não salvas', async () => {
    let release: (reply: Reply) => void = () => {}
    const { user } = await openPlaybook(playbookState(), {
      [`PUT ${PLAYBOOK_URL}/draft`]: () => new Promise<Reply>((resolve) => (release = resolve)),
    })
    const publish = () => screen.getByRole('button', { name: 'Publicar' }) as HTMLButtonElement
    expect(publish().disabled).toBe(false)

    await user.click(screen.getByText('Abertura'))
    await user.type(screen.getByLabelText('Nome da etapa'), ' do contato')
    await user.click(document.body)

    expect(publish().disabled).toBe(true)
    expect(screen.getByText('Há alterações não salvas. Salve ou desfaça antes de publicar.')).toBeTruthy()

    release('network')
    expect(await screen.findByText('Não foi possível salvar. Tente de novo.')).toBeTruthy()
    expect(publish().disabled).toBe(true)

    await user.click(screen.getByRole('button', { name: 'Desfazer' }))
    expect(publish().disabled).toBe(false)
    expect(screen.queryByText('Há alterações não salvas. Salve ou desfaça antes de publicar.')).toBeNull()
  })

  it('[C61b] Alterações não publicadas aparece só com diferença e Publicar desabilita sem ela', async () => {
    await openPlaybook(playbookState({ hasUnpublishedChanges: true }))
    expect(screen.getByText('Alterações não publicadas')).toBeTruthy()
    expect((screen.getByRole('button', { name: 'Publicar' }) as HTMLButtonElement).disabled).toBe(false)
    cleanup()

    await openPlaybook(
      playbookState({ hasUnpublishedChanges: false, latestVersion: { number: 2, publishedAt: '2026-11-15T12:00:00Z' } }),
    )
    expect(screen.queryByText('Alterações não publicadas')).toBeNull()
    expect((screen.getByRole('button', { name: 'Publicar' }) as HTMLButtonElement).disabled).toBe(true)
    cleanup()

    let unpublished = true
    const { user } = await openPlaybook(
      playbookState({ hasUnpublishedChanges: true, latestVersion: { number: 2, publishedAt: '2026-11-15T12:00:00Z' } }),
      {
        [`PUT ${PLAYBOOK_URL}/draft`]: (call) => {
          const body = call.body as { draft: Draft }
          return ok({ draft: body.draft, revision: 8, hasUnpublishedChanges: unpublished })
        },
      },
    )
    unpublished = false
    await user.click(screen.getByText('Abertura'))
    await user.clear(screen.getByLabelText('Nome da etapa'))
    await user.type(screen.getByLabelText('Nome da etapa'), 'Abertura da call')
    await user.click(document.body)
    await waitFor(() => expect(screen.queryByText('Alterações não publicadas')).toBeNull())
    expect((screen.getByRole('button', { name: 'Publicar' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('[C62] sem versão o topo mostra rascunho', async () => {
    await openPlaybook(playbookState({ latestVersion: null }))

    expect(screen.getByText('Playbook: Vendas Solar · rascunho')).toBeTruthy()
  })
})
