<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { getStoredUser } from '../../../lib/session'
import { AI_MODULE_TABS } from '../constants'

const props = defineProps<{
  title: string
  description: string
}>()

const route = useRoute()
const user = computed(() => getStoredUser())
</script>

<template>
  <div class="ai-shell">
    <header class="ai-shell__header">
      <div>
        <p class="ai-shell__eyebrow">AI 赋能</p>
        <h1>{{ props.title }}</h1>
        <p class="ai-shell__desc">{{ props.description }}</p>
      </div>
      <div class="ai-shell__header-actions">
        <RouterLink class="ai-shell__back" to="/">返回工作台</RouterLink>
        <div class="ai-shell__user">
          <span>{{ user?.displayName || user?.username || '当前用户' }}</span>
        </div>
      </div>
    </header>

    <nav class="ai-shell__tabs">
      <RouterLink
        v-for="tab in AI_MODULE_TABS"
        :key="tab.to"
        :to="tab.to"
        class="ai-shell__tab"
        :class="{ 'is-active': route.path === tab.to }"
      >
        {{ tab.label }}
      </RouterLink>
    </nav>

    <main class="ai-shell__content">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.ai-shell {
  min-height: 100vh;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(241, 189, 104, 0.22), transparent 26%),
    linear-gradient(180deg, #f7f3ea 0%, #f1ece1 100%);
  color: #1d2838;
}

.ai-shell__header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
  margin-bottom: 20px;
}

.ai-shell__eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #8f6233;
}

.ai-shell__header h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.1;
}

.ai-shell__desc {
  max-width: 760px;
  margin: 10px 0 0;
  color: #5e6776;
}

.ai-shell__header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.ai-shell__back,
.ai-shell__user {
  border-radius: 999px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(29, 40, 56, 0.08);
  color: #1d2838;
  text-decoration: none;
}

.ai-shell__tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.ai-shell__tab {
  padding: 10px 16px;
  border-radius: 999px;
  text-decoration: none;
  color: #4d5563;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(29, 40, 56, 0.08);
}

.ai-shell__tab.is-active {
  color: #fff;
  background: linear-gradient(135deg, #1b5965, #23424c);
}

.ai-shell__content {
  display: grid;
  gap: 16px;
}

@media (max-width: 900px) {
  .ai-shell {
    padding: 20px;
  }

  .ai-shell__header {
    flex-direction: column;
  }

  .ai-shell__header h1 {
    font-size: 28px;
  }
}
</style>
