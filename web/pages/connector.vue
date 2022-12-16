<template>
  <Container center max>
    <Container>
      <Text center title>
        Forest connector
      </Text>
      <Text>
        Manage your Forest setup conveniently!
      </Text>
      <Text italic>
        Type /forest in game to get started.
      </Text>
      <Input v-model="socket" placeholder="socket"/>
      <Input v-model="secret" placeholder="secret"/>
      <Button @click="openSocket">
        Connect
      </Button>
    </Container>
  </Container>
</template>

<script lang="ts" setup>

const socket = ref('')
const secret = useState('secret')

async function openSocket() {
  await navigateTo('/dash')
}

</script>