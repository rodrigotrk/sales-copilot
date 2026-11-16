import { cleanup, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'
import { fakeApi, ok, problem, renderApp } from '@/__tests__/harness'
import { me, playbookServer, playbookState } from '@/features/playbook/__tests__/fixtures'

describe('entrada', () => {
  it('[C01] sem sessão qualquer tela vai para /entrar com Google e campo de e-mail', async () => {
    fakeApi({ 'GET /api/me': problem(401, 'unauthorized') })
    const { router } = await renderApp('/n/b1/playbook')

    expect(router.currentRoute.value.path).toBe('/entrar')
    expect(screen.getByRole('link', { name: 'Entrar com Google' }).getAttribute('href')).toBe(
      '/oauth2/authorization/google',
    )
    expect(screen.getByLabelText('E-mail')).toBeTruthy()
  })

  it('[C02b] e-mail sem arroba mostra Informe um e-mail válido e não chama a API', async () => {
    const calls = fakeApi({})
    const { user } = await renderApp('/entrar')

    await user.type(screen.getByLabelText('E-mail'), 'gestor.exemplo.com')
    await user.click(screen.getByRole('button', { name: 'Enviar link de acesso' }))

    expect(screen.getByText('Informe um e-mail válido.')).toBeTruthy()
    expect(calls).toHaveLength(0)
  })

  it('[C02c] pedido aceito mostra Enviamos um link de acesso para o e-mail', async () => {
    document.cookie = 'XSRF-TOKEN=csrf-1'
    const calls = fakeApi({ 'POST /ott/generate': ok() })
    const { user } = await renderApp('/entrar')

    await user.type(screen.getByLabelText('E-mail'), 'gestor@exemplo.com')
    await user.click(screen.getByRole('button', { name: 'Enviar link de acesso' }))

    expect(await screen.findByText('Enviamos um link de acesso para gestor@exemplo.com.')).toBeTruthy()
    expect(calls[0]!.body).toEqual({ username: 'gestor@exemplo.com' })
    expect(calls[0]!.headers['X-XSRF-TOKEN']).toBe('csrf-1')
  })

  it('[C03b] resposta 429 mostra Aguarde 1 minuto para pedir outro link', async () => {
    fakeApi({ 'POST /ott/generate': problem(429, 'too_many_requests') })
    const { user } = await renderApp('/entrar')

    await user.type(screen.getByLabelText('E-mail'), 'gestor@exemplo.com')
    await user.click(screen.getByRole('button', { name: 'Enviar link de acesso' }))

    expect(await screen.findByText('Aguarde 1 minuto para pedir outro link.')).toBeTruthy()
  })

  it('[C123] e-mail recusado pelo servidor e falha de rede mostram mensagem na tela', async () => {
    fakeApi({ 'POST /ott/generate': problem(422, 'invalid_email') })
    const first = await renderApp('/entrar')
    await first.user.type(screen.getByLabelText('E-mail'), 'pessoa@')
    await first.user.click(screen.getByRole('button', { name: 'Enviar link de acesso' }))
    expect(await screen.findByText('Informe um e-mail válido.')).toBeTruthy()
    cleanup()

    fakeApi({ 'POST /ott/generate': 'network' })
    const second = await renderApp('/entrar')
    await second.user.type(screen.getByLabelText('E-mail'), 'gestor@exemplo.com')
    await second.user.click(screen.getByRole('button', { name: 'Enviar link de acesso' }))
    expect(await screen.findByText('Não foi possível enviar o link. Tente de novo.')).toBeTruthy()
  })

  it('[C04b] página do link mostra Entrar como o e-mail e o botão Entrar', async () => {
    const calls = fakeApi({ 'GET /api/login-links/tok-1': ok({ email: 'gestor@exemplo.com' }) })
    await renderApp('/entrar/link?token=tok-1')

    expect(await screen.findByText('Entrar como gestor@exemplo.com?')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeTruthy()
    expect(calls.map((c) => c.method)).toEqual(['GET'])
  })

  it('[C05b] Entrar envia o token por POST /login/ott', async () => {
    document.cookie = 'XSRF-TOKEN=csrf-2'
    const calls = fakeApi({
      'GET /api/login-links/tok-1': ok({ email: 'gestor@exemplo.com' }),
      'POST /login/ott': ok(),
      'GET /api/me': ok(me({ businesses: [], lastBusinessId: null, canCreateBusiness: true })),
    })
    const { router, user } = await renderApp('/entrar/link?token=tok-1')

    await user.click(await screen.findByRole('button', { name: 'Entrar' }))

    const post = calls.find((c) => c.method === 'POST')!
    expect(post.path).toBe('/login/ott')
    expect(post.body).toEqual({ token: 'tok-1' })
    expect(post.headers['X-XSRF-TOKEN']).toBe('csrf-2')
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('[C06b] link recusado mostra Este link já foi usado ou expirou', async () => {
    fakeApi({
      'GET /api/login-links/tok-1': ok({ email: 'gestor@exemplo.com' }),
      'POST /login/ott': problem(401, 'invalid_token'),
    })
    const { user } = await renderApp('/entrar/link?token=tok-1')

    await user.click(await screen.findByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('Este link já foi usado ou expirou. Peça um novo.')).toBeTruthy()
  })

  it('[C07b] erro google_unverified mostra Seu e-mail do Google não está verificado', async () => {
    fakeApi({})
    await renderApp('/entrar?erro=google_unverified')

    expect(screen.getByText('Seu e-mail do Google não está verificado.')).toBeTruthy()
  })

  it('[C09b] confirmação pendente mostra Confirme seu e-mail', async () => {
    fakeApi({})
    await renderApp('/entrar?confirmar=gestor@empresa.com.br')

    expect(screen.getByText('Confirme seu e-mail: enviamos um link para gestor@empresa.com.br.')).toBeTruthy()
  })

  it('[C24b] Sair abre /entrar', async () => {
    const { routes } = playbookServer(playbookState())
    const calls = fakeApi({ ...routes, 'POST /logout': ok(undefined, 204) })
    const { router, user } = await renderApp('/n/b1/playbook')

    await user.click(await screen.findByRole('button', { name: 'Sair' }))

    expect(calls.some((c) => c.method === 'POST' && c.path === '/logout')).toBe(true)
    expect(router.currentRoute.value.path).toBe('/entrar')
  })
})
