<template>

</template>

<script lang="ts" setup>
  const path = String(useRoute().params.id)
  const socket = useWebSocket('wss://forest.danifoldi.com/api/${path}', {
    autoReconnect: true
  })

  const focus = useWindowFocus()

  watch(focus, isFocused => {
    if (!isFocused) {
      socket.close(1000)
    }
  })
</script>