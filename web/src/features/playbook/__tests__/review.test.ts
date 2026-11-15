import { screen, waitFor, within } from '@testing-library/vue'
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
})
