<script lang="ts" setup>
import { computed,ref } from 'vue'
  import {useBrowserLocation} from "@vueuse/core";
import SecretForm from "./SecretForm.vue";
import Connected from "./Connected.vue";

  const location = useBrowserLocation()
  const id = computed(() => location.value.pathname.split('/').pop())
  const secret = ref<string>('')


const secretField = ref('')
  function updateSecret(newsecret: string) {
    secret.value = newsecret
  }

</script>

<template>
  <SecretForm v-if="secret === ''">
    <input type="text" v-model="secretField"/>
    <button @click="updateSecret(secretField)">Submit</button>
  </SecretForm>
  <Connected v-else :id="id" :secret="secret"></Connected>
</template>

<style lang="scss" scoped>

</style>
