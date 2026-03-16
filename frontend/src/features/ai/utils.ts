import type { AiAgentStep } from '../../types/ai'

export function prettyJson(value?: string | null): string {
  if (!value) {
    return '--'
  }

  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

export function truncateTitle(value: string, max = 18): string {
  const normalized = value.replace(/\s+/g, ' ').trim()
  if (!normalized) {
    return '新对话'
  }
  return normalized.length > max ? `${normalized.slice(0, max)}...` : normalized
}

export function toJsonInput(value: Record<string, unknown>): string {
  return JSON.stringify(value, null, 2)
}

export function roleLabel(role: string): string {
  if (role === 'assistant') {
    return '智能助手'
  }
  if (role === 'user') {
    return '你'
  }
  if (role === 'system') {
    return '系统'
  }
  return role
}

export function summarizeText(value: string, max = 48): string {
  const normalized = value.replace(/\s+/g, ' ').trim()
  if (!normalized) {
    return '--'
  }
  return normalized.length > max ? `${normalized.slice(0, max)}...` : normalized
}

function detectTools(prompt: string): string[] {
  const tools = new Set<string>()
  const normalized = prompt.toLowerCase()

  if (/告警|alert|alarm/.test(prompt)) {
    tools.add('告警摘要')
  }
  if (/日志|log|exception|trace/.test(prompt)) {
    tools.add('日志聚类')
  }
  if (/根因|原因|cause/.test(prompt)) {
    tools.add('根因分析')
  }
  if (/工单|ticket|派单|方案/.test(prompt)) {
    tools.add('工单方案')
  }
  if (/配置|变更|nginx|redis|mysql|k8s|cpu|ip/.test(normalized) || /配置|变更/.test(prompt)) {
    tools.add('配置检查')
  }
  if (tools.size === 0) {
    tools.add('通用运维问答')
  }

  return Array.from(tools)
}

export function buildAgentSteps(prompt: string): AiAgentStep[] {
  const tools = detectTools(prompt)
  const joined = tools.join('、')

  return [
    {
      id: 'intent',
      title: '识别意图',
      detail: `分析问题类型，判断优先调用 ${joined} 能力。`,
      status: 'in_progress',
      tool: 'intent-router',
    },
    {
      id: 'knowledge',
      title: '检索知识库',
      detail: '召回历史故障条目、处理方案和排障经验，补充上下文。',
      status: 'pending',
      tool: 'knowledge-search',
    },
    {
      id: 'context',
      title: '组装 Agent 上下文',
      detail: '拼接会话历史、知识片段和当前输入，形成可执行上下文。',
      status: 'pending',
      tool: 'context-builder',
    },
    {
      id: 'tools',
      title: '执行工具策略',
      detail: `根据问题内容模拟调度 ${joined} 工具，并整理中间结论。`,
      status: 'pending',
      tool: joined,
    },
    {
      id: 'response',
      title: '流式生成回复',
      detail: '按段落逐步输出最终答案，便于边看边处理。',
      status: 'pending',
      tool: 'stream-renderer',
    },
  ]
}

export function updateStepStatus(steps: AiAgentStep[], currentId: string): AiAgentStep[] {
  let currentReached = false
  return steps.map((step) => {
    if (step.id === currentId) {
      currentReached = true
      return { ...step, status: 'in_progress' }
    }
    if (!currentReached) {
      return { ...step, status: 'completed' }
    }
    return { ...step, status: 'pending' }
  })
}

export function completeAllSteps(steps: AiAgentStep[]): AiAgentStep[] {
  return steps.map((step) => ({ ...step, status: 'completed' }))
}

export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}
