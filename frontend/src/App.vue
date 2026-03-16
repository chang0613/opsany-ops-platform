<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'

import AiFloatingAssistant from './features/ai/components/AiFloatingAssistant.vue'

const route = useRoute()
const showFloatingAssistant = computed(() => route.path !== '/login')
</script>

<template>
  <RouterView />
  <AiFloatingAssistant v-if="showFloatingAssistant" />
</template>
