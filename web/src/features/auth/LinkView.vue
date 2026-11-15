<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/lib/http'
import { consumeLink, readLink, type LinkKind } from './api'
import { authMessages } from './messages'

const props = defineProps<{ kind: LinkKind }>()
const route = useRoute()
const router = useRouter()
const token = typeof route.query.token === 'string' ? route.query.token : ''
const email = ref('')
const error = ref('')

function refuse(e: unknown) {
  if (!(e instanceof ApiError)) throw e
  error.value = authMessages.linkInvalid
}

onMounted(async () => {
  try {
    email.value = (await readLink(props.kind, token)).email
  } catch (e) {
    refuse(e)
  }
})

async function signIn() {
  try {
    await consumeLink(props.kind, token)
  } catch (e) {
    refuse(e)
    return
  }
  await router.push('/')
}
</script>

<template>
  <main class="grid min-h-screen place-items-center p-4">
    <div class="card w-full max-w-sm">
      <p v-if="error" class="err text-[13px]" role="alert">{{ error }}</p>
      <template v-else-if="email">
        <h1 class="m-0 mb-4 text-[18px] font-semibold">{{ authMessages.signInAs(email) }}</h1>
        <button class="btn pri w-full justify-center" type="button" @click="signIn">Entrar</button>
      </template>
    </div>
  </main>
</template>
