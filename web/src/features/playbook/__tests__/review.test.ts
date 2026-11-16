import { cleanup, screen, waitFor, within } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'
import { fakeApi, ok, problem, renderApp, type Call } from '@/__tests__/harness'
import { me as currentUser } from '@/features/auth/session'
import type { Draft } from '../api'
import { me, playbookServer, playbookState, sampleDraft } from './fixtures'

const button = (name: string) => screen.getByRole('button', { name }) as HTMLButtonElement

describe('correções da revisão de código', () => {
  it('[C92] Importar documento fica desabilitado com alteração não salva ou com falha', async () => {
    let release: () => void = () => {}
    let pending = true
    const { routes, acceptDraft } = playbookServer(playbookState())
    fakeApi({
      ...routes,
      'PUT /api/businesses/b1/playbook/draft': (call: Call) =>
        pending ? new Promise((resolve) => (release = () => resolve(acceptDraft(call)))) : problem(503, ''),
    })
    const { user } = await renderApp('/n/b1/playbook')
    await screen.findByText(/^Playbook: /)
    expect(button('Importar documento').disabled).toBe(false)

    await user.click(screen.getByText('Diagnóstico da conta de luz'))
    await user.clear(screen.getByLabelText('Nome da etapa'))
    await user.type(screen.getByLabelText('Nome da etapa'), 'Diagnóstico')
    await user.click(document.body)
    await waitFor(() => expect(button('Importar documento').disabled).toBe(true))
    pending = false
    release()
    await waitFor(() => expect(button('Importar documento').disabled).toBe(false))

    await user.click(screen.getByText('Abertura'))
    await user.clear(screen.getByLabelText('Nome da etapa'))
    await user.type(screen.getByLabelText('Nome da etapa'), 'Boas-vindas')
    await user.click(document.body)

    expect(await screen.findByText('Não foi possível salvar. Tente de novo.')).toBeTruthy()
    expect(button('Importar documento').disabled).toBe(true)
    expect(screen.getByText('Boas-vindas')).toBeTruthy()
  })

  it('[C93] falha ao remover uma linha mostra a mensagem com Tentar de novo e Desfazer', async () => {
    let fail = true
    const { server, routes, acceptDraft } = playbookServer(playbookState())
    fakeApi({
      ...routes,
      'PUT /api/businesses/b1/playbook/draft': (call: Call) => (fail ? problem(503, '') : acceptDraft(call)),
    })
    const { user } = await renderApp('/n/b1/playbook')
    await screen.findByText(/^Playbook: /)
    const stages = within(screen.getByRole('region', { name: 'Etapas da call' }))

    await user.click(stages.getAllByRole('button', { name: 'Remover' })[0]!)

    expect(await screen.findByText('Não foi possível salvar. Tente de novo.')).toBeTruthy()
    await user.click(screen.getByRole('button', { name: 'Desfazer' }))
    expect(stages.getByText('Abertura')).toBeTruthy()

    await user.click(stages.getAllByRole('button', { name: 'Remover' })[0]!)
    fail = false
    await user.click(await screen.findByRole('button', { name: 'Tentar de novo' }))
    await waitFor(() => expect(server.state.draft.stages.map((s) => s.id)).toEqual(['s2', 's3']))

    fail = true
    await user.click(stages.getAllByRole('button', { name: 'Descer etapa' })[0]!)
    expect(await screen.findByText('Não foi possível salvar. Tente de novo.')).toBeTruthy()
    await user.click(screen.getByRole('button', { name: 'Desfazer' }))
    await waitFor(() => expect(screen.queryByText('Não foi possível salvar. Tente de novo.')).toBeNull())

    await user.click(screen.getByRole('button', { name: '+ Etapa' }))
    await user.keyboard('Fechamento')
    await user.tab()
    expect(await screen.findByText('Não foi possível salvar. Tente de novo.')).toBeTruthy()
    fail = false
    await user.click(screen.getByRole('button', { name: 'Tentar de novo' }))
    await waitFor(() => expect(server.state.draft.stages.map((s) => s.name)).toContain('Fechamento'))
  })

  it('[C94] voltar pelo histórico a outro negócio recria a tela e grava no negócio do endereço', async () => {
    const draftA = sampleDraft()
    const draftB: Draft = { ...sampleDraft(), stages: [{ id: 'b-s1', name: 'Etapa do negócio B' }] }
    const calls = fakeApi({
      'GET /api/me': ok(me({ businesses: [{ id: 'b1', name: 'Solar Norte', role: 'gestor' }, { id: 'b2', name: 'Solar Sul', role: 'gestor' }] })),
      'GET /api/businesses/b1/playbook': ok(playbookState({ draft: draftA, name: 'Playbook A' })),
      'GET /api/businesses/b2/playbook': ok(playbookState({ playbookId: 'pb2', draft: draftB, name: 'Playbook B' })),
      'PUT /api/businesses/b1/playbook/draft': (call: Call) => ok({ draft: (call.body as { draft: Draft }).draft, revision: 8, hasUnpublishedChanges: true }),
    })
    const { router, user } = await renderApp('/n/b1/playbook')
    await screen.findByText('Playbook: Playbook A · rascunho')
    await router.push('/n/b2/playbook')
    await screen.findByText('Playbook: Playbook B · rascunho')
    router.back()
    await screen.findByText('Playbook: Playbook A · rascunho')

    await user.click(screen.getByText('Abertura'))
    await user.clear(screen.getByLabelText('Nome da etapa'))
    await user.type(screen.getByLabelText('Nome da etapa'), 'Abertura A')
    await user.click(document.body)

    await waitFor(() => expect(calls.some((c) => c.method === 'PUT')).toBe(true))
    const put = calls.filter((c) => c.method === 'PUT')
    expect(put.map((c) => c.path)).toEqual(['/api/businesses/b1/playbook/draft'])
    expect((put[0]!.body as { playbookId: string }).playbookId).toBe('pb1')
  })

  it('[C100] remover a linha com erro local libera Publicar e Importar documento', async () => {
    const { routes } = playbookServer(playbookState())
    fakeApi(routes)
    const { user } = await renderApp('/n/b1/playbook')
    await screen.findByText(/^Playbook: /)
    const prices = within(screen.getByRole('region', { name: 'Tabela de preços' }))

    await user.click(screen.getByText('Kit 3 kWp'))
    await user.clear(screen.getByLabelText('À vista'))
    await user.type(screen.getByLabelText('À vista'), 'abc')
    await user.click(document.body)
    expect(screen.getByText('Informe um valor em reais maior que zero.')).toBeTruthy()
    expect(button('Publicar').disabled).toBe(true)
    expect(button('Importar documento').disabled).toBe(true)

    await user.click(prices.getAllByRole('button', { name: 'Remover' })[0]!)

    await waitFor(() => expect(button('Importar documento').disabled).toBe(false))
    expect(button('Publicar').disabled).toBe(false)
    expect(screen.queryByText('Informe um valor em reais maior que zero.')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Desfazer' })).toBeNull()
  })

  it('[C101] sessão expirada durante a edição leva a /entrar na requisição seguinte', async () => {
    const { routes } = playbookServer(playbookState())
    fakeApi({ ...routes, 'PUT /api/businesses/b1/playbook/draft': problem(401, 'unauthenticated') })
    const { router, user } = await renderApp('/n/b1/playbook')
    await screen.findByText(/^Playbook: /)

    await user.click(screen.getByText('Abertura'))
    await user.clear(screen.getByLabelText('Nome da etapa'))
    await user.type(screen.getByLabelText('Nome da etapa'), 'Boas-vindas')
    await user.click(document.body)

    await waitFor(() => expect(router.currentRoute.value.path).toBe('/entrar'))
    expect(currentUser.value).toBeNull()
    expect(screen.queryByText('Não foi possível salvar. Tente de novo.')).toBeNull()
  })

  it('[C102] parcela acima de R$ 10 milhões é aceita, sem o teto do preço à vista', async () => {
    const { server, routes } = playbookServer(playbookState())
    fakeApi(routes)
    const { user } = await renderApp('/n/b1/playbook')
    await screen.findByText(/^Playbook: /)

    await user.click(screen.getByText('Kit 3 kWp'))
    await user.clear(screen.getByLabelText('Valor da parcela'))
    await user.type(screen.getByLabelText('Valor da parcela'), '10.000.000,01')
    await user.click(document.body)

    await waitFor(() =>
      expect(server.state.draft.priceTable.plans[0]!.installments).toEqual({ count: 6, value: '10000000.01' }),
    )
    expect(screen.queryByText('Parcelas de 2 a 60, com valor maior que zero.')).toBeNull()
  })

  it('[C105] importação iniciada em outra aba fecha a linha aberta e nenhum campo fica editável', async () => {
    const { server, routes } = playbookServer(playbookState())
    fakeApi({
      ...routes,
      'PUT /api/businesses/b1/playbook/draft': () => {
        server.state.importJob = { id: 'j1', status: 'running', counts: null, skipped: 0, notices: [], errorCode: null }
        return problem(409, 'import_running')
      },
    })
    const { user } = await renderApp('/n/b1/playbook')
    await screen.findByText(/^Playbook: /)

    await user.click(screen.getByText('Consumo mensal'))
    await user.clear(screen.getByLabelText('Informação'))
    await user.type(screen.getByLabelText('Informação'), 'Consumo')
    await user.click(screen.getByLabelText('Pergunta sugerida'))

    await waitFor(() => expect(screen.queryByLabelText('Pergunta sugerida')).toBeNull())
    expect(screen.queryByLabelText('Informação')).toBeNull()
    expect(document.querySelectorAll('main input:not([disabled])')).toHaveLength(0)
  })

  it('[C108] durante a publicação nenhuma linha abre para edição e nada digitado se perde', async () => {
    let release: () => void = () => {}
    const { server, routes } = playbookServer(playbookState())
    fakeApi({
      ...routes,
      'POST /api/businesses/b1/playbook/versions': () =>
        new Promise((resolve) => {
          release = () => {
            server.state.latestVersion = { number: 1, publishedAt: '2026-11-15T12:00:00Z' }
            server.state.hasUnpublishedChanges = false
            resolve(ok({ number: 1 }, 201))
          }
        }),
    })
    const { user } = await renderApp('/n/b1/playbook')
    await screen.findByText(/^Playbook: /)

    await user.click(button('Publicar'))
    await user.click(screen.getByText('Abertura'))
    expect(screen.queryByLabelText('Nome da etapa')).toBeNull()
    expect(document.querySelectorAll('main input:not([disabled])')).toHaveLength(0)

    release()
    expect(await screen.findByText(/versão 1/)).toBeTruthy()
    await user.click(screen.getByText('Abertura'))
    expect(screen.getByLabelText('Nome da etapa')).toBeTruthy()
  })

  it('[C109] Desfazer fica indisponível enquanto outra gravação está em andamento', async () => {
    let release: () => void = () => {}
    let call = 0
    const { server, routes, acceptDraft } = playbookServer(playbookState())
    fakeApi({
      ...routes,
      'PUT /api/businesses/b1/playbook/draft': (c: Call) => {
        call += 1
        if (call === 1) return problem(503, '')
        return new Promise((resolve) => (release = () => resolve(acceptDraft(c))))
      },
    })
    const { user } = await renderApp('/n/b1/playbook')
    await screen.findByText(/^Playbook: /)

    await user.click(screen.getByText('Abertura'))
    await user.clear(screen.getByLabelText('Nome da etapa'))
    await user.type(screen.getByLabelText('Nome da etapa'), 'Boas-vindas')
    await user.click(document.body)
    expect(await screen.findByText('Não foi possível salvar. Tente de novo.')).toBeTruthy()

    await user.click(screen.getByText('Proposta'))
    await user.clear(screen.getByLabelText('Nome da etapa'))
    await user.type(screen.getByLabelText('Nome da etapa'), 'Proposta final')
    await user.click(document.body)
    await waitFor(() => expect(call).toBe(2))
    for (const undo of screen.getAllByRole('button', { name: 'Desfazer' })) {
      expect((undo as HTMLButtonElement).disabled).toBe(true)
      await user.click(undo)
    }

    release()
    await waitFor(() => expect(screen.queryByText('Não foi possível salvar. Tente de novo.')).toBeNull())
    expect(server.state.draft.stages.map((st) => st.name)).toEqual(['Boas-vindas', 'Diagnóstico da conta de luz', 'Proposta final'])
    expect(screen.getByText('Boas-vindas')).toBeTruthy()
    expect(screen.getByText('Proposta final')).toBeTruthy()
  })

  it('[C110] consulta da importação que falha uma vez continua até a tarefa terminar', async () => {
    let gets = 0
    const running = playbookState()
    running.importJob = { id: 'j1', status: 'running', counts: null, skipped: 0, notices: [], errorCode: null }
    const done = playbookState()
    done.importJob = { id: 'j1', status: 'done', counts: { stages: 1, discovery: 0, objections: 0, plans: 0 }, skipped: 0, notices: [], errorCode: null }
    fakeApi({
      'GET /api/me': ok(me()),
      'GET /api/businesses/b1/playbook': () => {
        gets += 1
        if (gets === 1) return ok(running)
        if (gets === 2) return problem(503, '')
        return ok(done)
      },
    })
    await renderApp('/n/b1/playbook')
    expect(await screen.findByText('Importando…')).toBeTruthy()

    await waitFor(() => expect(screen.queryByText('Importando…')).toBeNull(), { timeout: 7000 })
    expect(gets).toBeGreaterThanOrEqual(3)
    expect(button('Importar documento').disabled).toBe(false)
    cleanup()

    // Sair da tela com uma consulta em andamento encerra o acompanhamento.
    let pending: (reply: ReturnType<typeof ok>) => void = () => {}
    let later = 0
    fakeApi({
      'GET /api/me': ok(me()),
      'GET /api/businesses/b1/playbook': () => {
        later += 1
        if (later === 1) return ok(running)
        return new Promise((resolve) => (pending = resolve))
      },
    })
    await renderApp('/n/b1/playbook')
    expect(await screen.findByText('Importando…')).toBeTruthy()
    await waitFor(() => expect(later).toBe(2), { timeout: 4000 })
    cleanup()
    pending(ok(running))
    await new Promise((resolve) => setTimeout(resolve, 4500))
    expect(later).toBe(2)
  }, 20000)

  it('[C111] voltar o campo ao valor gravado apaga o erro do servidor e libera Publicar', async () => {
    const { routes, acceptDraft } = playbookServer(playbookState())
    fakeApi({
      ...routes,
      'PUT /api/businesses/b1/playbook/draft': (c: Call) => {
        const draft = (c.body as { draft: Draft }).draft
        if (draft.stages.some((st) => st.name.length > 120)) {
          return problem(422, 'invalid_draft', [
            { itemId: 's1', field: 'name', code: 'too_long', message: 'Máximo de 120 caracteres.' },
          ])
        }
        return acceptDraft(c)
      },
    })
    const { user } = await renderApp('/n/b1/playbook')
    await screen.findByText(/^Playbook: /)

    await user.click(screen.getByText('Abertura'))
    await user.clear(screen.getByLabelText('Nome da etapa'))
    await user.type(screen.getByLabelText('Nome da etapa'), 'x'.repeat(121))
    await user.click(document.body)
    expect(await screen.findByText('Máximo de 120 caracteres.')).toBeTruthy()
    expect(button('Publicar').disabled).toBe(true)

    await user.click(screen.getByText('x'.repeat(121)))
    await user.clear(screen.getByLabelText('Nome da etapa'))
    await user.type(screen.getByLabelText('Nome da etapa'), 'Abertura')
    await user.click(document.body)

    await waitFor(() => expect(screen.queryByText('Máximo de 120 caracteres.')).toBeNull())
    expect(button('Publicar').disabled).toBe(false)
    expect(button('Importar documento').disabled).toBe(false)
  })

  it('[C24c] Sair aparece no acesso não liberado, na criação de negócio e na lista e leva a /entrar', async () => {
    const calls = fakeApi({
      'GET /api/me': ok(me({ businesses: [], lastBusinessId: null, canCreateBusiness: false })),
      'POST /logout': ok(undefined, 204),
    })
    const { router, user } = await renderApp('/')
    expect(screen.getByText('Seu acesso ainda não foi liberado.')).toBeTruthy()
    await user.click(screen.getByRole('button', { name: 'Sair' }))
    await waitFor(() => expect(router.currentRoute.value.path).toBe('/entrar'))
    expect(calls.some((c) => c.method === 'POST' && c.path === '/logout')).toBe(true)
    cleanup()

    const create = fakeApi({
      'GET /api/me': ok(me({ businesses: [], lastBusinessId: null, canCreateBusiness: true })),
      'POST /logout': ok(undefined, 204),
    })
    const creation = await renderApp('/')
    expect(screen.getByText('Crie o seu negócio')).toBeTruthy()
    await creation.user.click(screen.getByRole('button', { name: 'Sair' }))
    await waitFor(() => expect(creation.router.currentRoute.value.path).toBe('/entrar'))
    expect(create.some((c) => c.method === 'POST' && c.path === '/logout')).toBe(true)
    cleanup()

    const listCalls = fakeApi({
      'GET /api/me': ok(me({ businesses: [{ id: 'b1', name: 'Solar Norte', role: 'gestor' }, { id: 'b2', name: 'Loja Sul', role: 'gestor' }], lastBusinessId: null })),
      'POST /logout': ok(undefined, 204),
    })
    const list = await renderApp('/negocios')
    expect(await screen.findByText('Escolha o negócio')).toBeTruthy()
    expect(screen.getByText('Solar Norte')).toBeTruthy()
    expect(screen.getByText('Loja Sul')).toBeTruthy()
    await list.user.click(screen.getByRole('button', { name: 'Sair' }))
    await waitFor(() => expect(list.router.currentRoute.value.path).toBe('/entrar'))
    expect(listCalls.some((c) => c.method === 'POST' && c.path === '/logout')).toBe(true)
  })

  it('[C112] remover o último plano apaga o erro do desconto e libera Publicar e Importar documento', async () => {
    const { routes } = playbookServer(playbookState())
    fakeApi(routes)
    const { user } = await renderApp('/n/b1/playbook')
    await screen.findByText(/^Playbook: /)
    const prices = within(screen.getByRole('region', { name: 'Tabela de preços' }))

    await user.click(screen.getByText(/Desconto máximo na call/))
    await user.clear(screen.getByLabelText('Desconto máximo na call'))
    await user.type(screen.getByLabelText('Desconto máximo na call'), 'abc')
    await user.click(document.body)
    expect(button('Publicar').disabled).toBe(true)

    await user.click(prices.getAllByRole('button', { name: 'Remover' })[0]!)
    await waitFor(() => expect(prices.getAllByRole('button', { name: 'Remover' })).toHaveLength(1))
    await user.click(prices.getAllByRole('button', { name: 'Remover' })[0]!)

    await waitFor(() => expect(button('Importar documento').disabled).toBe(false))
    expect(button('Publicar').disabled).toBe(false)
    expect(screen.queryByText(/Desconto máximo na call/)).toBeNull()
  })

  it('[C113] importação aceita com 202 fica em Importando e é acompanhada mesmo se a primeira consulta falhar', async () => {
    let gets = 0
    const done = playbookState()
    done.importJob = { id: 'j1', status: 'done', counts: { stages: 1, discovery: 0, objections: 0, plans: 0 }, skipped: 0, notices: [], errorCode: null }
    fakeApi({
      'GET /api/me': ok(me()),
      'GET /api/businesses/b1/playbook': () => {
        gets += 1
        if (gets === 1) return ok(playbookState())
        if (gets === 2) return problem(503, '')
        return ok(done)
      },
      'POST /api/businesses/b1/playbook/imports': ok({ jobId: 'j1' }, 202),
    })
    const { user } = await renderApp('/n/b1/playbook')
    await screen.findByText(/^Playbook: /)

    await user.click(button('Importar documento'))
    const dialog = within(await screen.findByRole('dialog'))
    await user.click(dialog.getByLabelText('Texto colado'))
    await user.type(dialog.getByLabelText('Texto do playbook'), 'Etapa 1: abertura.')
    await user.click(dialog.getByRole('button', { name: 'Importar' }))

    expect(await screen.findByText('Importando…')).toBeTruthy()
    await waitFor(() => expect(gets).toBe(2))
    expect(button('Importar documento').disabled).toBe(true)
    expect(button('Publicar').disabled).toBe(true)
    expect(button('+ Etapa').disabled).toBe(true)
    await user.click(screen.getByText('Abertura'))
    expect(screen.queryByLabelText('Nome da etapa')).toBeNull()
    await waitFor(() => expect(screen.queryByText('Importando…')).toBeNull(), { timeout: 7000 })
    expect(gets).toBe(3)
  }, 12000)
})
