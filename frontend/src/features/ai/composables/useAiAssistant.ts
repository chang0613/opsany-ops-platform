import { computed, ref } from 'vue'

import { createAiConversation, listAiConversations, listAiMessages, sendAiMessageStream } from '../../../lib/api'
import type { AiAgentStep, AiConversation, AiMessage } from '../../../types/ai'
import { buildAgentSteps, completeAllSteps, truncateTitle, updateStepStatus } from '../utils'

function nowLabel(): string {
  return new Date().toLocaleString('zh-CN', { hour12: false })
}

export function useAiAssistant() {
  const conversations = ref<AiConversation[]>([])
  const messages = ref<AiMessage[]>([])
  const activeConversationId = ref(0)
  const prompt = ref('')
  const loading = ref(false)
  const sending = ref(false)
  const initialized = ref(false)
  const streaming = ref(false)
  const streamingText = ref('')
  const pendingUserMessage = ref<AiMessage | null>(null)
  const toolSteps = ref<AiAgentStep[]>([])

  const activeConversation = computed(() => conversations.value.find((item) => item.id === activeConversationId.value) ?? null)

  const renderedMessages = computed(() => {
    const result = [...messages.value]
    if (pendingUserMessage.value) {
      result.push(pendingUserMessage.value)
    }
    if (streamingText.value) {
      result.push({
        id: -1,
        conversationId: activeConversationId.value,
        role: 'assistant',
        content: streamingText.value,
        createdAt: nowLabel(),
      })
    }
    return result
  })

  async function refreshConversations(): Promise<void> {
    conversations.value = await listAiConversations()
  }

  async function loadMessages(id: number): Promise<void> {
    activeConversationId.value = id
    messages.value = await listAiMessages(id)
  }

  async function ensureConversation(seedTitle = '运维助手对话'): Promise<void> {
    await refreshConversations()
    if (!activeConversationId.value) {
      activeConversationId.value = conversations.value[0]?.id ?? 0
    }
    if (!activeConversationId.value) {
      const created = await createAiConversation(seedTitle)
      await refreshConversations()
      activeConversationId.value = created.id
    }
    await loadMessages(activeConversationId.value)
  }

  async function init(force = false): Promise<void> {
    if (initialized.value && !force) {
      return
    }

    loading.value = true
    try {
      await ensureConversation()
      initialized.value = true
    } finally {
      loading.value = false
    }
  }

  async function createConversation(seedTitle?: string): Promise<void> {
    const created = await createAiConversation(truncateTitle(seedTitle || '新对话'))
    await refreshConversations()
    await loadMessages(created.id)
    initialized.value = true
  }

  async function selectConversation(id: number): Promise<void> {
    loading.value = true
    try {
      resetTransientState()
      await loadMessages(id)
    } finally {
      loading.value = false
    }
  }

  function updatePrompt(value: string): void {
    prompt.value = value
  }

  function resetTransientState(): void {
    pendingUserMessage.value = null
    streamingText.value = ''
    streaming.value = false
  }

  function applyInitialSteps(content: string): void {
    toolSteps.value = buildAgentSteps(content)
  }

  function registerStep(payload: Record<string, unknown>): void {
    const stepId = String(payload.stepId || '')
    const title = String(payload.title || '')
    const detail = String(payload.detail || '')
    const status = String(payload.status || 'pending') as AiAgentStep['status']

    if (!stepId) {
      return
    }

    const exists = toolSteps.value.some((item) => item.id === stepId)
    if (exists) {
      toolSteps.value = toolSteps.value.map((item) => item.id === stepId ? { ...item, title, detail, status } : item)
      return
    }

    toolSteps.value = [...toolSteps.value, { id: stepId, title, detail, status }]
  }

  function markStepProgress(payload: Record<string, unknown>): void {
    const stepId = String(payload.stepId || '')
    if (!stepId) {
      return
    }
    toolSteps.value = updateStepStatus(toolSteps.value, stepId)
  }

  async function submitPrompt(customPrompt?: string): Promise<void> {
    const content = (customPrompt ?? prompt.value).trim()
    if (!content) {
      return
    }

    if (!activeConversationId.value) {
      await ensureConversation(truncateTitle(content))
    }

    prompt.value = ''
    pendingUserMessage.value = {
      id: -2,
      conversationId: activeConversationId.value,
      role: 'user',
      content,
      createdAt: nowLabel(),
    }
    streamingText.value = ''
    streaming.value = true
    sending.value = true
    applyInitialSteps(content)

    try {
      await sendAiMessageStream(activeConversationId.value, content, {
        onStep: registerStep,
        onStepProgress: markStepProgress,
        onDelta: (payload) => {
          const delta = String(payload.delta || '')
          if (delta) {
            streamingText.value += delta
          }
        },
        onDone: () => {
          toolSteps.value = completeAllSteps(toolSteps.value)
        },
        onError: (payload) => {
          throw new Error(String(payload.message || '流式回复失败'))
        },
      })

      await loadMessages(activeConversationId.value)
      await refreshConversations()
      initialized.value = true
    } finally {
      pendingUserMessage.value = null
      streamingText.value = ''
      streaming.value = false
      sending.value = false
    }
  }

  return {
    activeConversation,
    activeConversationId,
    conversations,
    createConversation,
    init,
    initialized,
    loading,
    messages: renderedMessages,
    prompt,
    refreshConversations,
    selectConversation,
    sending,
    streaming,
    submitPrompt,
    toolSteps,
    updatePrompt,
  }
}
