<script lang="ts" setup>
import { onMounted, ref } from 'vue'

const message = ref<string>('')
const error = ref<string>('')

onMounted(async () => {
  try {
    const response = await fetch('/api/hello')
    if (!response.ok) {
      throw new Error(`HTTP error: ${response.status}`)
    }
    message.value = await response.text()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to fetch message'
  }
})
</script>

<template>
  <main>
    <h1>Event Checker</h1>
    <p v-if="message" class="message">{{ message }}</p>
    <p v-if="error" class="error">{{ error }}</p>
  </main>
</template>

<style scoped>
.message {
  font-size: 1.5rem;
  color: var(--color-text);
}

.error {
  color: red;
}
</style>
