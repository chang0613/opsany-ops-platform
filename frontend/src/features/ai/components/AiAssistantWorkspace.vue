<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted } from 'vue'

import { AI_ASSISTANT_SUGGESTIONS } from '../constants'
import { useAiAssistant } from '../composables/useAiAssistant'
import AiConversationSidebar from './AiConversationSidebar.vue'
import AiMessageThread from './AiMessageThread.vue'
import AiPromptComposer from './AiPromptComposer.vue'
import AiToolStatusPanel from './AiToolStatusPanel.vue'

const props = withDefaults(defineProps<{
  compact?: boolean
}>(), {
  compact: false,
})

const {
  activeConversation,
  activeConversationId,
  conversations,
  createConversation,
  init,
  loading,
  messages,
  prompt,
  selectConversation,
  sending,
  streaming,
  submitPrompt,
  toolSteps,
  updatePrompt,
} = useAiAssistant()

const conversationSummary = computed(() => activeConversation.value?.title || '新的运维会话')
const assistantBusy = computed(() => sending.value || streaming.value)

onMounted(async () => {
  try {
    await init()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载 AI 对话失败')
  }
})

async function handleCreateConversation(): Promise<void> {
  try {
    await createConversation()
    ElMessage.success('已创建新的 AI 会话')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建会话失败')
  }
}

async function handleSelectConversation(id: number): Promise<void> {
  try {
    await selectConversation(id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载会话失败')
  }
}

async function handleSubmit(value?: string): Promise<void> {
  try {
    await submitPrompt(value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送消息失败')
  }
}

async function handleSuggestion(value: string): Promise<void> {
  if (props.compact) {
    await handleSubmit(value)
    return
  }

  updatePrompt(value)
}
</script>

<template>
  <div class="assistant-workspace" :class="{ 'is-compact': props.compact }">
    <template v-if="props.compact">
      <section class="assistant-workspace__hero assistant-workspace__panel">
        <div>
          <p class="assistant-workspace__eyebrow">智能对话助手</p>
          <h3>{{ conversationSummary }}</h3>
          <p>支持多轮问答、流式回复和工具轨迹查看，可直接用于告警解释、日志归因、工单总结和变更评估。</p>
        </div>
        <div class="assistant-workspace__hero-actions">
          <el-select
            :model-value="activeConversationId || undefined"
            class="assistant-workspace__select"
            placeholder="选择会话"
            @update:model-value="handleSelectConversation(Number($event))"
          >
            <el-option v-for="item in conversations" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
          <el-button type="primary" plain @click="handleCreateConversation">新建</el-button>
        </div>
      </section>

      <AiToolStatusPanel compact :steps="toolSteps" :active="assistantBusy" />
      <AiMessageThread compact :loading="loading" :messages="messages" :empty-text="assistantBusy ? '助手正在准备响应，请稍候。' : undefined" />
      <AiPromptComposer
        compact
        :model-value="prompt"
        :disabled="loading || assistantBusy"
        :loading="assistantBusy"
        :suggestions="Array.from(AI_ASSISTANT_SUGGESTIONS)"
        @update:model-value="updatePrompt"
        @pick-suggestion="handleSuggestion"
        @submit="handleSubmit()"
      />
    </template>

    <template v-else>
      <AiConversationSidebar
        :conversations="conversations"
        :active-conversation-id="activeConversationId"
        @create="handleCreateConversation"
        @select="handleSelectConversation"
      />

      <div class="assistant-workspace__main">
        <section class="assistant-workspace__hero assistant-workspace__panel">
          <div>
            <p class="assistant-workspace__eyebrow">当前会话</p>
            <h3>{{ conversationSummary }}</h3>
            <p>把故障上下文尽量一次说明清楚，助手会先展示工具执行轨迹，再以流式方式输出最终结论。</p>
          </div>
        </section>

        <div class="assistant-workspace__content-grid">
          <AiMessageThread :loading="loading" :messages="messages" />
          <AiToolStatusPanel :steps="toolSteps" :active="assistantBusy" />
        </div>
        <AiPromptComposer
          :model-value="prompt"
          :disabled="loading || assistantBusy"
          :loading="assistantBusy"
          :suggestions="Array.from(AI_ASSISTANT_SUGGESTIONS)"
          @update:model-value="updatePrompt"
          @pick-suggestion="handleSuggestion"
          @submit="handleSubmit()"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.assistant-workspace {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
}

.assistant-workspace.is-compact {
  grid-template-columns: 1fr;
}

.assistant-workspace__main {
  display: grid;
  gap: 16px;
}

.assistant-workspace__panel {
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(29, 40, 56, 0.08);
}

.assistant-workspace__hero {
  display: grid;
  gap: 16px;
}

.assistant-workspace__content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.7fr);
  gap: 16px;
  align-items: start;
}

.assistant-workspace__hero h3 {
  margin: 0;
  font-size: 22px;
}

.assistant-workspace__hero p {
  margin: 8px 0 0;
  color: #5e6776;
}

.assistant-workspace__eyebrow {
  margin: 0;
  color: #8f6233;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.assistant-workspace__hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.assistant-workspace__select {
  flex: 1;
  min-width: 0;
}

@media (max-width: 1100px) {
  .assistant-workspace {
    grid-template-columns: 1fr;
  }

  .assistant-workspace__content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
