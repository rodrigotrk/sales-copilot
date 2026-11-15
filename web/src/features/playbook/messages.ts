import { formatMoney } from './money'

export const LIMITS = { stages: 30, discovery: 50, objections: 50, plans: 20 } as const

// Campos de 500 caracteres; os demais textos do playbook têm 120.
const LONG_FIELDS = new Set(['question', 'leadSays', 'approvedResponse'])

export const playbookMessages = {
  tooLong: (field: string) => `Máximo de ${LONG_FIELDS.has(field) ? 500 : 120} caracteres.`,
  blockLimit: (n: number) => `Limite de ${n} itens neste bloco.`,
  cash: 'Informe um valor em reais maior que zero.',
  installments: 'Parcelas de 2 a 60, com valor maior que zero.',
  discount: (minCash: string) =>
    `O desconto máximo precisa ser menor que o menor preço à vista (R$ ${formatMoney(minCash)}).`,
  saveFailed: 'Não foi possível salvar. Tente de novo.',
  conflict: 'O playbook foi alterado em outra aba. Recarregue para continuar.',
  unsaved: 'Há alterações não salvas. Salve ou desfaça antes de publicar.',
  publishMinimum: 'Para publicar, cadastre ao menos uma etapa, uma informação a descobrir e uma objeção.',
  incompleteRows: 'Complete ou remova as linhas destacadas.',
  pendingReview: 'Confira os valores marcados "Importado · conferir" antes de publicar.',
  publishFailed: 'Não foi possível publicar. Tente de novo.',
  importing: 'Importando…',
}
