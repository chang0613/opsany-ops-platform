export interface AiConversation {
  id: number
  title: string
  ownerUsername: string
  createdAt: string
  updatedAt: string
}

export interface AiMessage {
  id: number
  conversationId: number
  role: 'system' | 'user' | 'assistant' | string
  content: string
  createdAt: string
}

export interface AiJob {
  id: number
  jobType: string
  status: string
  ownerUsername: string
  inputJson?: string | null
  resultJson?: string | null
  createdAt: string
  updatedAt: string
}

export interface AiKnowledgeEntry {
  id: number
  title: string
  content: string
  tags?: string | null
  sourceType?: string | null
  sourceId?: string | null
  ownerUsername: string
  createdAt: string
  updatedAt: string
}

export type AiAgentStepStatus = 'pending' | 'in_progress' | 'completed'

export interface AiAgentStep {
  id: string
  title: string
  detail: string
  status: AiAgentStepStatus
  tool?: string
}

export interface AiRuntimeConfig {
  provider: string
  knowledgeContextEnabled: boolean
  configured: boolean
  apiKeyConfigured: boolean
  apiKeyMasked: string
  baseUrl: string
  model: string
  connectTimeoutMs: number
  readTimeoutMs: number
}

export interface AiRuntimeConfigUpdate {
  provider: string
  knowledgeContextEnabled: boolean
  apiKey?: string
  clearApiKey?: boolean
  baseUrl: string
  model: string
  connectTimeoutMs: number
  readTimeoutMs: number
}

export interface AiRuntimeTestResult {
  success: boolean
  message: string
  preview: string
}
