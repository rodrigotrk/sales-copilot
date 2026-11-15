export interface FieldError {
  itemId: string | null
  field: string
  code: string
  message: string
}

// O servidor responde erros em Problem Details (RFC 9457); a tela escolhe o texto pelo `code`.
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    readonly errors: FieldError[] = [],
  ) {
    super(code || `HTTP ${status}`)
  }
}

function csrfToken(): string | undefined {
  const cookie = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='))
  return cookie && decodeURIComponent(cookie.slice('XSRF-TOKEN='.length))
}

// O Spring emite o cookie XSRF-TOKEN em qualquer resposta; a tela /entrar não faz GET antes do
// primeiro POST, então o cliente pede o cookie quando ele ainda não existe.
async function ensureCsrfCookie(): Promise<string | undefined> {
  if (!csrfToken()) await fetch('/api/me', { credentials: 'same-origin' })
  return csrfToken()
}

export async function request(method: string, url: string, body?: unknown): Promise<Response> {
  const headers: Record<string, string> = {}
  const token = method === 'GET' ? undefined : await ensureCsrfCookie()
  if (token) headers['X-XSRF-TOKEN'] = token
  let payload: BodyInit | undefined
  if (body instanceof FormData || body instanceof URLSearchParams) payload = body
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body)
  }
  // `manual` porque os logins do Spring respondem 302; seguir o redirecionamento baixaria o HTML do app.
  const res = await fetch(url, { method, headers, body: payload, credentials: 'same-origin', redirect: 'manual' })
  if (res.ok || res.type === 'opaqueredirect') return res
  // Um 502 do proxy chega em HTML, sem `code`; o status basta para a tela.
  const problem = await res.json().catch(() => ({}))
  throw new ApiError(res.status, problem.code ?? '', problem.errors ?? [])
}

export async function getJson<T>(url: string): Promise<T> {
  return (await request('GET', url)).json()
}

export async function sendJson<T>(method: string, url: string, body: unknown): Promise<T> {
  return (await request(method, url, body)).json()
}
