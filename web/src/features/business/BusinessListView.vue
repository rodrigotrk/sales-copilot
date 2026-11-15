<script setup lang="ts">
import { computed } from 'vue'
import { me, playbookPath } from '@/features/auth/session'
import SignOutButton from '@/features/auth/SignOutButton.vue'

const businesses = computed(() => [...(me.value?.businesses ?? [])].sort((a, b) => a.name.localeCompare(b.name, 'pt-BR')))
</script>

<template>
  <main class="grid min-h-screen place-content-center justify-items-center p-4">
    <div class="card w-full max-w-sm">
      <h1 class="m-0 mb-4 text-[21px] font-bold">Escolha o negócio</h1>
      <ul class="m-0 grid list-none gap-2 p-0">
        <li v-for="b in businesses" :key="b.id">
          <RouterLink class="btn w-full" :to="playbookPath(b.id)">{{ b.name }}</RouterLink>
        </li>
      </ul>
    </div>
    <SignOutButton />
  </main>
</template>
