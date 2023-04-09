<script lang="ts" setup>
import {tryOnMounted} from "@vueuse/core";
  import {toRefs} from "vue";

  const props = defineProps<{
    pool: string,
    secret: string
  }>()

  const {pool, secret} = toRefs(props)

  tryOnMounted(() => {
    const ws = new WebSocket(`wss://forest.danifoldi.com/api/${pool.value}`, ['forest', secret.value])
    ws.onopen = () => {
      console.log('connected')
      ws.send('apiVersion')
    }
    ws.onmessage = (e) => {
      console.log('message', e)
    }
    ws.onclose = (e) => {
      console.log('close', e)
    }
    ws.onerror = (e) => {
      console.log('error', e)
    }
  })
</script>

<template>

</template>

<style lang="scss" scoped>

</style>
