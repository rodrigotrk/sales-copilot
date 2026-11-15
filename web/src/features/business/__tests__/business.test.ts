import { cleanup, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'
import { fakeApi, ok, renderApp } from '@/__tests__/harness'
import { me, playbookState } from '@/features/playbook/__tests__/fixtures'

const two = [
  { id: 'b1', name: 'Solar Norte', role: 'gestor' as const },
  { id: 'b2', name: 'Loja Sul', role: 'gestor' as const },
]

const playbookOf = (id: string) => ({ [`GET /api/businesses/${id}/playbook`]: ok(playbookState()) })

describe('negócio', () => {
  it('[C12b] formulário de criação abre /n/id/playbook', async () => {
    const calls = fakeApi({
      'GET /api/me': ok(me({ businesses: [], lastBusinessId: null, canCreateBusiness: true })),
      'POST /api/businesses': ok({ businessId: 'b9' }, 201),
      ...playbookOf('b9'),
    })
    const { router, user } = await renderApp('/')

    await user.type(screen.getByLabelText('Nome do negócio'), 'Solar Norte')
    await user.type(screen.getByLabelText('Nome do playbook'), 'Vendas Solar')
    await user.click(screen.getByRole('button', { name: 'Criar' }))

    expect(calls.find((c) => c.method === 'POST')!.body).toEqual({
      businessName: 'Solar Norte',
      playbookName: 'Vendas Solar',
    })
    expect(router.currentRoute.value.path).toBe('/n/b9/playbook')
  })

  it('[C13b] sem liberação mostra Seu acesso ainda não foi liberado', async () => {
    fakeApi({ 'GET /api/me': ok(me({ businesses: [], lastBusinessId: null, canCreateBusiness: false })) })
    await renderApp('/')

    expect(screen.getByText('Seu acesso ainda não foi liberado.')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Criar' })).toBeNull()
  })

  it('[C16b] entrada abre o último negócio válido ou o único', async () => {
    fakeApi({ 'GET /api/me': ok(me({ businesses: two, lastBusinessId: 'b2' })), ...playbookOf('b2') })
    const last = await renderApp('/')
    expect(last.router.currentRoute.value.path).toBe('/n/b2/playbook')
    cleanup()

    fakeApi({ 'GET /api/me': ok(me({ lastBusinessId: null })), ...playbookOf('b1') })
    const only = await renderApp('/')
    expect(only.router.currentRoute.value.path).toBe('/n/b1/playbook')
  })

  it('[C17] mais de um negócio sem último válido mostra a lista e abre o escolhido', async () => {
    fakeApi({ 'GET /api/me': ok(me({ businesses: two, lastBusinessId: 'b7' })), ...playbookOf('b2') })
    const { router, user } = await renderApp('/')

    expect(router.currentRoute.value.path).toBe('/negocios')
    await user.click(await screen.findByRole('link', { name: 'Loja Sul' }))

    await screen.findByText('Playbook: Vendas Solar · rascunho')
    expect(router.currentRoute.value.path).toBe('/n/b2/playbook')
  })

  it('[C18] Trocar de negócio aparece só com mais de um negócio e abre a lista', async () => {
    fakeApi({ 'GET /api/me': ok(me()), ...playbookOf('b1') })
    await renderApp('/n/b1/playbook')
    await screen.findByText('Playbook: Vendas Solar · rascunho')
    expect(screen.queryByText('Trocar de negócio')).toBeNull()

    fakeApi({ 'GET /api/me': ok(me({ businesses: two })), ...playbookOf('b1') })
    cleanup()
    const { router, user } = await renderApp('/n/b1/playbook')
    await user.click(await screen.findByRole('link', { name: 'Trocar de negócio' }))

    expect(router.currentRoute.value.path).toBe('/negocios')
    expect(await screen.findByRole('link', { name: 'Solar Norte' })).toBeTruthy()
  })
})
