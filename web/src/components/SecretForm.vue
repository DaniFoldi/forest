<script lang="ts" setup>
  import {ref} from "vue";

  const emit = defineEmits<{
    (e: 'update', secret: string): void
  }>()

  const secret = ref('')
</script>

<template>
    <input type="text" v-model="secret"/>
    <button @click="emit('update', secret)">Submit</button>
</template>

<style lang="scss" scoped>

</style>
