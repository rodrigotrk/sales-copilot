import { getJson, request } from '@/lib/http'

export type LinkKind = 'login' | 'identity'

const links = {
  login: { read: '/api/login-links/', consume: '/login/ott' },
  identity: { read: '/api/identity-links/', consume: '/login/identity-link' },
}

export function requestLoginLink(email: string) {
  return request('POST', '/ott/generate', new URLSearchParams({ username: email }))
}

export function readLink(kind: LinkKind, token: string) {
  return getJson<{ email: string }>(links[kind].read + encodeURIComponent(token))
}

export function consumeLink(kind: LinkKind, token: string) {
  return request('POST', links[kind].consume, new URLSearchParams({ token }))
}
