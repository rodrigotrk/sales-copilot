// O dinheiro trafega como string decimal com ponto e duas casas ("4500.00"), nunca como número de ponto flutuante.

const MAX_CENTS = 1_000_000_000 // R$ 10.000.000,00

function groupThousands(digits: string): string {
  return digits.replace(/\B(?=(\d{3})+$)/g, '.')
}

/** "4500.00" -> "4.500"; "1166.67" -> "1.166,67". */
export function formatMoney(decimal: string): string {
  const [int = '0', cents = '00'] = decimal.split('.')
  const grouped = groupThousands(int)
  return cents === '00' ? grouped : `${grouped},${cents}`
}

export const formatCash = (decimal: string) => `R$ ${formatMoney(decimal)}`

export function formatInstallments(installments: { count: number; value: string } | null): string {
  return installments ? `${installments.count}x ${formatMoney(installments.value)}` : '—'
}

/**
 * Lê o que a pessoa digitou em reais ("4.500,50", "4500", "R$ 4.500") e devolve "4500.50", ou null
 * quando não é um valor a partir do mínimo com até duas casas. `capped` limita a R$ 10.000.000,00,
 * o teto do preço à vista; a parcela não tem teto.
 */
export function parseMoney(text: string, { allowZero = false, capped = true } = {}): string | null {
  const clean = text.replace(/^R\$\s*/, '').trim()
  if (!/^(\d{1,3}(\.\d{3})+|\d+)(,\d{1,2})?$/.test(clean)) return null
  const [int = '', frac = ''] = clean.replace(/\./g, '').split(',')
  const whole = int.replace(/^0+(?=\d)/, '')
  const cents = Number(whole) * 100 + Number(frac.padEnd(2, '0'))
  if (cents < (allowZero ? 0 : 1) || (capped && cents > MAX_CENTS)) return null
  // Montado a partir do texto: Number perde precisão em valores grandes.
  return `${whole}.${frac.padEnd(2, '0')}`
}

export const toCents = (decimal: string) => Math.round(Number(decimal) * 100)
