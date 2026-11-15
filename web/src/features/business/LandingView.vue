<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '@/lib/http'
import { me, playbookPath } from '@/features/auth/session'
import SignOutButton from '@/features/auth/SignOutButton.vue'
import { createBusiness } from './api'

const router = useRouter()
const businessName = ref('')
const playbookName = ref('')
const error = ref('')
const notAllowed = 'Seu acesso ainda não foi liberado.'

async function create() {
  error.value = ''
  try {
    const { businessId } = await createBusiness(businessName.value.trim(), playbookName.value.trim())
    await router.push(playbookPath(businessId))
  } catch (e) {
    if (!(e instanceof ApiError)) throw e
    if (e.code === 'already_member') return router.push('/')
    error.value =
      e.code === 'signup_not_allowed' ? notAllowed : 'Informe o nome do negócio e o do playbook, com até 80 caracteres.'
  }
}
</script>

<template>
  <main class="grid min-h-screen place-content-center justify-items-center p-4">
    <div class="card w-full max-w-sm">
      <form v-if="me?.canCreateBusiness" @submit.prevent="create">
        <h1 class="m-0 mb-4 text-[21px] font-bold">Crie o seu negócio</h1>
        <label class="mb-1 block text-[13px]" for="business-name">Nome do negócio</label>
        <input id="business-name" v-model="businessName" class="f mb-3" required maxlength="80" />
        <label class="mb-1 block text-[13px]" for="playbook-name">Nome do playbook</label>
        <input id="playbook-name" v-model="playbookName" class="f" required maxlength="80" />
        <p v-if="error" class="err" role="alert">{{ error }}</p>
        <button class="btn pri mt-4 w-full justify-center" type="submit">Criar</button>
      </form>
      <p v-else class="m-0">{{ notAllowed }}</p>
    </div>
    <SignOutButton />
  </main>
</template>
