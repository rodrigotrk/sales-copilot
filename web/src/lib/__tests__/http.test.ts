import { afterEach, describe, expect, it, vi } from 'vitest'
import { request } from '../http'

describe('cliente http', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
  })

  it('[C91] sem cookie XSRF-TOKEN busca o token antes do primeiro POST e o envia', async () => {
    const calls: { url: string; headers: Record<string, string> }[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        calls.push({ url, headers: (init?.headers ?? {}) as Record<string, string> })
        if (url === '/api/me') document.cookie = 'XSRF-TOKEN=token-emitido; path=/'
        return new Response(null, { status: url === '/api/me' ? 401 : 200 })
      }),
    )

    await request('POST', '/ott/generate', new URLSearchParams({ username: 'gestor@exemplo.test' }))

    expect(calls.map((c) => c.url)).toEqual(['/api/me', '/ott/generate'])
    expect(calls[1]!.headers['X-XSRF-TOKEN']).toBe('token-emitido')
  })
})
