import { getJson, sendJson } from '@/lib/http'

export interface Review {
  status: 'pending'
  excerpt: string
}

export interface Stage {
  id: string
  name: string
}

export interface Discovery {
  id: string
  information: string
  question: string
}

export interface Objection {
  id: string
  name: string
  leadSays: string
  approvedResponse: string
}

export interface Installments {
  count: number
  value: string
}

export interface Plan {
  id: string
  name: string
  extraValue: string
  cash: string | null
  installments: Installments | null
  review: Review | null
}

export interface Draft {
  schemaVersion: 1
  stages: Stage[]
  discovery: Discovery[]
  objections: Objection[]
  priceTable: {
    extraColumnName: string
    maxDiscount: string | null
    maxDiscountReview: Review | null
    plans: Plan[]
  }
}

export interface ImportJob {
  id: string
  status: 'queued' | 'running' | 'done' | 'failed'
  counts: { stages: number; discovery: number; objections: number; plans: number } | null
  skipped: number
  notices: string[]
  errorCode: string | null
}

export interface PlaybookState {
  playbookId: string
  name: string
  draft: Draft
  revision: number
  latestVersion: { number: number; publishedAt: string } | null
  hasUnpublishedChanges: boolean
  hasCalls: boolean
  importJob: ImportJob | null
}

export const playbookUrl = (businessId: string) => `/api/businesses/${businessId}/playbook`

export const getPlaybook = (businessId: string) => getJson<PlaybookState>(playbookUrl(businessId))

export function saveDraft(businessId: string, playbookId: string, draft: Draft, revision: number) {
  return sendJson<{ draft: Draft; revision: number; hasUnpublishedChanges: boolean }>('PUT', `${playbookUrl(businessId)}/draft`, {
    playbookId,
    draft,
    revision,
  })
}

export function publishVersion(businessId: string, playbookId: string, revision: number) {
  return sendJson<{ number: number }>('POST', `${playbookUrl(businessId)}/versions`, { playbookId, revision })
}
