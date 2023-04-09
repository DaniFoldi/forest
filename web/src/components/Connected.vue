<script lang="ts" setup>
import { onMounted} from 'vue'
  import {useWebSocket} from "@vueuse/core";

  const props = defineProps<{
    id: string,
    secret: string
  }>()

  const ws = useWebSocket(`/api/${props.id}`, {
    autoReconnect: true,
    protocols: ['forest', props.secret],
    onConnected: () => {
      console.log('connected')
      ws.send('apiVersion')
    },
    onError: (e) => {
      console.log('error', e)
    },
    onMessage: (e) => {
      console.log('message', e)
    },
    onDisconnected: (e) => {
      console.log('close', e)
    }
  })
  onMounted(() => {
    ws.open()
  })
</script>

<template>

</template>

<style lang="scss" scoped>

</style>
