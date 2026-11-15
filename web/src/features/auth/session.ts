import { ref } from 'vue'
import { ApiError, getJson, request } from '@/lib/http'

export interface Membership {
  id: string
  name: string
  role: 'gestor' | 'closer'
}

export interface Me {
  user: { email: string }
  businesses: Membership[]
  lastBusinessId: string | null
  canCreateBusiness: boolean
}

export const me = ref<Me | null>(null)

// Lida a cada navegação protegida, para que uma sessão expirada leve a /entrar na tela seguinte.
export async function loadMe(): Promise<Me | null> {
  try {
    me.value = await getJson<Me>('/api/me')
  } catch (e) {
    if (!(e instanceof ApiError && e.status === 401)) throw e
    me.value = null
  }
  return me.value
}

export async function logout() {
  await request('POST', '/logout')
  me.value = null
}

export const playbookPath = (businessId: string) => `/n/${businessId}/playbook`

// null: a pessoa não tem negócio e fica na tela de criação ou de acesso não liberado.
export function landingPath(current: Me): string | null {
  const ids = current.businesses.map((b) => b.id)
  if (current.lastBusinessId && ids.includes(current.lastBusinessId)) return playbookPath(current.lastBusinessId)
  if (ids.length === 1) return playbookPath(ids[0]!)
  if (ids.length > 1) return '/negocios'
  return null
}
