<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ApiError } from '@/lib/http'
import { requestLoginLink } from './api'
import { authMessages } from './messages'

const route = useRoute()
const email = ref('')
const message = ref('')
const error = ref('')

const googleMessage = computed(() => {
  if (route.query.erro === 'google_unverified') return authMessages.googleUnverified
  if (typeof route.query.confirmar === 'string') return authMessages.confirmEmail(route.query.confirmar)
  return ''
})

async function send() {
  message.value = ''
  error.value = ''
  const address = email.value.trim()
  if (!address.includes('@')) {
    error.value = authMessages.invalidEmail
    return
  }
  try {
    await requestLoginLink(address)
    message.value = authMessages.linkSent(address)
  } catch (e) {
    if (!(e instanceof ApiError)) throw e
    error.value = e.code === 'too_many_requests' ? authMessages.tooManyRequests : authMessages.sendFailed
  }
}
</script>

<template>
  <main class="grid min-h-screen place-items-center p-4">
    <div class="card w-full max-w-sm">
      <h1 class="m-0 mb-4 text-[21px] font-bold">Entrar</h1>
      <p v-if="googleMessage" class="notice mb-3" role="status">{{ googleMessage }}</p>
      <a class="btn w-full justify-center" href="/oauth2/authorization/google">Entrar com Google</a>
      <p class="my-3 text-center text-[12.5px] text-muted-foreground">ou receba um link por e-mail</p>
      <form novalidate @submit.prevent="send">
        <label class="mb-1 block text-[13px]" for="email">E-mail</label>
        <input id="email" v-model="email" class="f" type="email" autocomplete="email" />
        <p v-if="error" class="err" role="alert">{{ error }}</p>
        <button class="btn pri mt-3 w-full justify-center" type="submit">Enviar link de acesso</button>
      </form>
      <p v-if="message" class="mt-3 text-[13px]" role="status">{{ message }}</p>
    </div>
  </main>
</template>
