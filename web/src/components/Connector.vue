<script lang="ts" setup>
import { computed,ref } from 'vue'
  import {useBrowserLocation} from "@vueuse/core";
import SecretForm from "./SecretForm.vue";
import Connected from "./Connected.vue";

  const location = useBrowserLocation()
  const id = computed(() => location.value.pathname.split('/').pop())
  const secret = ref<string>('')
</script>

<template>
  <SecretForm v-if="secret === ''" @update="i => secret = i">
  </SecretForm>
  <Connected v-else :pool="id" :secret="secret"></Connected>
</template>

<style lang="scss" scoped>

</style>
