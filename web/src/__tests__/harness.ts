import { cleanup, render } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { createMemoryHistory } from 'vue-router'
import { afterEach, vi } from 'vitest'
import App from '@/App.vue'
import { createAppRouter } from '@/router'

export interface Call {
  method: string
  path: string
  body: unknown
  headers: Record<string, string>
}

export type Reply = { status: number; body?: unknown } | 'network'
type Route = Reply | ((call: Call) => Reply | Promise<Reply>)

export const ok = (body?: unknown, status = 200): Reply => ({ status, body })

export const problem = (status: number, code: string, errors?: unknown[]): Reply => ({
  status,
  body: { type: `/problems/${code}`, title: code, status, detail: code, instance: '/', code, errors },
})

/** Troca o fetch por respostas por "MÉTODO caminho" e devolve a lista de chamadas feitas. */
export function fakeApi(routes: Record<string, Route>): Call[] {
  const calls: Call[] = []
  // O cookie que o servidor emite na primeira resposta; a busca dele quando falta é provada em http.test.ts.
  if (!document.cookie.includes('XSRF-TOKEN=')) document.cookie = 'XSRF-TOKEN=token-de-teste'

  vi.stubGlobal('fetch', async (url: string, init: RequestInit = {}) => {
    const method = init.method ?? 'GET'
    let body = init.body as unknown
    if (typeof body === 'string') body = JSON.parse(body)
    else if (body instanceof URLSearchParams) body = Object.fromEntries(body)
    const call = { method, path: url, body, headers: (init.headers ?? {}) as Record<string, string> }
    calls.push(call)
    const route = routes[`${method} ${url.split('?')[0]}`]
    if (!route) throw new Error(`teste sem resposta para ${method} ${url}`)
    const reply = typeof route === 'function' ? await route(call) : route
    if (reply === 'network') throw new TypeError('Failed to fetch')
    const type = reply.status >= 400 ? 'application/problem+json' : 'application/json'
    const payload = reply.body === undefined ? null : JSON.stringify(reply.body)
    return new Response(payload, { status: reply.status, headers: { 'Content-Type': type } })
  })
  return calls
}

export async function renderApp(path: string, options: Parameters<typeof userEvent.setup>[0] = {}) {
  const router = createAppRouter(createMemoryHistory())
  // Navega antes de montar para o roteador não disparar a navegação inicial para "/".
  await router.push(path)
  const user = userEvent.setup(options)
  render(App, { global: { plugins: [router] } })
  return { router, user }
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.useRealTimers()
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
})
