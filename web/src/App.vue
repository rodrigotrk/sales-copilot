<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <!-- A chave recria a tela ao trocar de endereço: o editor pertence ao negócio do endereço (AD-045). -->
  <RouterView v-slot="{ Component, route }">
    <component :is="Component" :key="route.path" />
  </RouterView>
</template>
