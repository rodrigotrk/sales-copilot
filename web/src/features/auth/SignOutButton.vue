<script setup lang="ts">
import { useRouter } from 'vue-router'
import { logout } from './session'

// "Sair" nas telas autenticadas fora do menu do produto (AC 24): criação de negócio, acesso não liberado e lista.
const router = useRouter()

async function signOut() {
  await logout()
  await router.push('/entrar')
}
</script>

<template>
  <button type="button" class="btn xs mt-3" @click="signOut">Sair</button>
</template>
