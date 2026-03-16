<script setup lang="ts">
import type { AiConversation } from '../../../types/ai'

const props = withDefaults(defineProps<{
  conversations: AiConversation[]
  activeConversationId: number
  compact?: boolean
}>(), {
  compact: false,
})

const emit = defineEmits<{
  (event: 'select', id: number): void
  (event: 'create'): void
}>()
</script>

<template>
  <aside class="assistant-sidebar" :class="{ 'is-compact': props.compact }">
    <div class="assistant-sidebar__header">
      <div>
        <p>会话列表</p>
        <strong>{{ props.conversations.length }} 个会话</strong>
      </div>
      <el-button type="primary" plain @click="emit('create')">新建</el-button>
    </div>

    <div class="assistant-sidebar__list">
      <button
        v-for="conversation in props.conversations"
        :key="conversation.id"
        class="assistant-sidebar__item"
        :class="{ 'is-active': conversation.id === props.activeConversationId }"
        @click="emit('select', conversation.id)"
      >
        <strong>{{ conversation.title }}</strong>
        <span>{{ conversation.updatedAt }}</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.assistant-sidebar {
  display: grid;
  gap: 12px;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(29, 40, 56, 0.08);
}

.assistant-sidebar.is-compact {
  padding: 14px;
  border-radius: 20px;
}

.assistant-sidebar__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.assistant-sidebar__header p {
  margin: 0;
  color: #8f6233;
  font-size: 12px;
}

.assistant-sidebar__list {
  display: grid;
  gap: 10px;
  max-height: 420px;
  overflow: auto;
}

.assistant-sidebar__item {
  display: grid;
  gap: 4px;
  text-align: left;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(29, 40, 56, 0.08);
  background: rgba(245, 242, 236, 0.8);
  cursor: pointer;
}

.assistant-sidebar__item strong {
  font-size: 14px;
}

.assistant-sidebar__item span {
  color: #6d7480;
  font-size: 12px;
}

.assistant-sidebar__item.is-active {
  background: linear-gradient(135deg, #23424c, #1b5965);
  color: #fff;
}

.assistant-sidebar__item.is-active span {
  color: rgba(255, 255, 255, 0.8);
}
</style>
