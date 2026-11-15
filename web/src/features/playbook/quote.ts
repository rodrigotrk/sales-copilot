/** Falas aparecem entre aspas fora da edição e são guardadas sem elas. */
export const quoted = (text: string) => (text ? `"${text}"` : '')
