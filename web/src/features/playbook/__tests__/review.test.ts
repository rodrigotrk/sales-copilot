import { screen, waitFor, within } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'
import { fakeApi, ok, problem, renderApp, type Call } from '@/__tests__/harness'
import type { Draft } from '../api'
import { me, playbookServer, playbookState, sampleDraft } from './fixtures'

const button = (name: string) => screen.getByRole('button', { name }) as HTMLButtonElement

describe('correções da revisão de código', () => {
  it('[C92] Importar documento fica desabilitado com alteração não salva ou com falha', async () => {
    const { routes } = playbookServer(playbookState())
    fakeApi({ ...routes, 'PUT /api/businesses/b1/playbook/draft': problem(503, '') })
    const { user } = await renderApp('/n/b1/playbook')
    await screen.findByText(/^Playbook: /)
    expect(button('Importar documento').disabled).toBe(false)

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
})
