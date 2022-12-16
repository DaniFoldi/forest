<template>
  <Paintbrush app-name="Paintbrush Template App" page-title="Paintbrush Template App">
    <NuxtPage />
  </Paintbrush>
</template>

<script lang="ts" setup>
  import { useNuxtApp } from '#app'
  useNuxtApp().vueApp.config.unwrapInjectedRef = true
</script>
