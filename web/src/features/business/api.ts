import { sendJson } from '@/lib/http'

export function createBusiness(businessName: string, playbookName: string) {
  return sendJson<{ businessId: string }>('POST', '/api/businesses', { businessName, playbookName })
}
