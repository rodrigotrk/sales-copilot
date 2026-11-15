export const authMessages = {
  invalidEmail: 'Informe um e-mail válido.',
  linkSent: (email: string) => `Enviamos um link de acesso para ${email}.`,
  tooManyRequests: 'Aguarde 1 minuto para pedir outro link.',
  sendFailed: 'Não foi possível enviar o link. Tente de novo.',
  linkInvalid: 'Este link já foi usado ou expirou. Peça um novo.',
  googleUnverified: 'Seu e-mail do Google não está verificado.',
  confirmEmail: (email: string) => `Confirme seu e-mail: enviamos um link para ${email}.`,
  signInAs: (email: string) => `Entrar como ${email}?`,
}
