import type { Draft, PlaybookState } from '@/features/playbook/api'
import type { Me } from '@/features/auth/session'
import { ok, type Call, type Reply } from '@/__tests__/harness'

// Playbook inventado de uma empresa de energia solar; nenhum dado vem das gravações.
export const BUSINESS = 'b1'
export const PLAYBOOK_URL = `/api/businesses/${BUSINESS}/playbook`

export function me(over: Partial<Me> = {}): Me {
  return {
    user: { email: 'gestor@exemplo.com' },
    businesses: [{ id: BUSINESS, name: 'Solar Norte', role: 'gestor' }],
    lastBusinessId: BUSINESS,
    canCreateBusiness: false,
    ...over,
  }
}

export function emptyDraft(): Draft {
  return {
    schemaVersion: 1,
    stages: [],
    discovery: [],
    objections: [],
    priceTable: { extraColumnName: '', maxDiscount: null, maxDiscountReview: null, plans: [] },
  }
}

export function sampleDraft(): Draft {
  return {
    schemaVersion: 1,
    stages: [
      { id: 's1', name: 'Abertura' },
      { id: 's2', name: 'Diagnóstico da conta de luz' },
      { id: 's3', name: 'Proposta' },
    ],
    discovery: [
      { id: 'd1', information: 'Consumo mensal', question: 'Quanto vem a sua conta de luz por mês?' },
      { id: 'd2', information: 'Tipo de telhado', question: 'O telhado é de cerâmica ou de laje?' },
    ],
    objections: [
      { id: 'o1', name: 'Preço alto', leadSays: 'Achei caro', approvedResponse: 'Quanto você paga de luz em um ano?' },
    ],
    priceTable: {
      extraColumnName: 'Painéis',
      maxDiscount: null,
      maxDiscountReview: null,
      plans: [
        { id: 'p1', name: 'Kit 3 kWp', extraValue: '6', cash: '4500.00', installments: { count: 6, value: '1166.67' }, review: null },
        { id: 'p2', name: 'Kit 5 kWp', extraValue: '10', cash: '7000.00', installments: null, review: null },
      ],
    },
  }
}

export function playbookState(over: Partial<PlaybookState> = {}): PlaybookState {
  return {
    playbookId: 'pb1',
    name: 'Vendas Solar',
    draft: sampleDraft(),
    revision: 7,
    latestVersion: null,
    hasUnpublishedChanges: true,
    hasCalls: false,
    importJob: null,
    ...over,
  }
}

/** Servidor falso do playbook: guarda o rascunho a cada PUT aceito e devolve a revisão seguinte. */
export function playbookServer(initial: PlaybookState) {
  const server = { state: structuredClone(initial) }
  const acceptDraft = (call: Call): Reply => {
    const body = call.body as { draft: Draft }
    server.state.draft = body.draft
    server.state.revision += 1
    return ok({ draft: body.draft, revision: server.state.revision, hasUnpublishedChanges: true })
  }
  const routes = {
    'GET /api/me': ok(me()),
    [`GET ${PLAYBOOK_URL}`]: () => ok(structuredClone(server.state)),
    [`PUT ${PLAYBOOK_URL}/draft`]: acceptDraft,
  }
  return { server, routes, acceptDraft }
}
