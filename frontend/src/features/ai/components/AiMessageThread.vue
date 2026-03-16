<script setup lang="ts">
import type { AiMessage } from '../../../types/ai'

import { roleLabel } from '../utils'

const props = withDefaults(defineProps<{
  loading?: boolean
  messages: AiMessage[]
  compact?: boolean
  emptyText?: string
}>(), {
  loading: false,
  compact: false,
  emptyText: '暂时还没有消息，先发起一个问题吧。',
})
</script>

<template>
  <section class="message-thread" :class="{ 'is-compact': props.compact }">
    <el-skeleton v-if="props.loading" :rows="props.compact ? 4 : 6" animated />
    <div v-else-if="props.messages.length" class="message-thread__list">
      <article
        v-for="message in props.messages"
        :key="message.id"
        class="message-thread__item"
        :class="[`role-${message.role}`]"
      >
        <div class="message-thread__meta">
          <strong>{{ roleLabel(message.role) }}</strong>
          <span>{{ message.createdAt }}</span>
        </div>
        <div class="message-thread__content">{{ message.content }}</div>
      </article>
    </div>
    <div v-else class="message-thread__empty">{{ props.emptyText }}</div>
  </section>
</template>

<style scoped>
.message-thread {
  min-height: 420px;
  max-height: 640px;
  overflow: auto;
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(29, 40, 56, 0.08);
}

.message-thread.is-compact {
  min-height: 280px;
  max-height: 380px;
  padding: 16px;
  border-radius: 20px;
}

.message-thread__list {
  display: grid;
  gap: 14px;
}

.message-thread__item {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: 20px;
  background: #f8f4eb;
}

.message-thread__item.role-assistant {
  background: #eef5f4;
}

.message-thread__item.role-system {
  background: #f4f0ff;
}

.message-thread__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  color: #68717f;
  font-size: 12px;
}

.message-thread__content {
  white-space: pre-wrap;
  line-height: 1.7;
}

.message-thread__empty {
  min-height: 220px;
  display: grid;
  place-items: center;
  color: #68717f;
  text-align: center;
}
</style>
