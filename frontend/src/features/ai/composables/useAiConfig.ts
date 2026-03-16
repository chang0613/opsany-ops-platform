import { reactive, ref } from 'vue'

import { getAiConfig, testAiConfig, updateAiConfig } from '../../../lib/api'
import type { AiRuntimeConfig, AiRuntimeConfigUpdate, AiRuntimeTestResult } from '../../../types/ai'

function createDefaultForm(): AiRuntimeConfigUpdate {
  return {
    provider: 'mock',
    knowledgeContextEnabled: true,
    apiKey: '',
    clearApiKey: false,
    baseUrl: 'https://open.bigmodel.cn/api/v1/agents',
    model: 'service_check_agent',
    connectTimeoutMs: 10000,
    readTimeoutMs: 60000,
  }
}

export function useAiConfig() {
  const config = ref<AiRuntimeConfig | null>(null)
  const loading = ref(false)
  const saving = ref(false)
  const testing = ref(false)
  const loaded = ref(false)
  const form = reactive<AiRuntimeConfigUpdate>(createDefaultForm())

  function applyConfigToForm(value: AiRuntimeConfig): void {
    form.provider = value.provider || 'mock'
    form.knowledgeContextEnabled = value.knowledgeContextEnabled
    form.apiKey = ''
    form.clearApiKey = false
    form.baseUrl = value.baseUrl || 'https://open.bigmodel.cn/api/v1/agents'
    form.model = value.model || 'service_check_agent'
    form.connectTimeoutMs = value.connectTimeoutMs || 10000
    form.readTimeoutMs = value.readTimeoutMs || 60000
  }

  async function refresh(force = false): Promise<void> {
    if (loading.value && !force) {
      return
    }

    loading.value = true
    try {
      const result = await getAiConfig()
      config.value = result
      applyConfigToForm(result)
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  async function save(): Promise<AiRuntimeConfig> {
    saving.value = true
    try {
      const result = await updateAiConfig({ ...form })
      config.value = result
      applyConfigToForm(result)
      return result
    } finally {
      saving.value = false
    }
  }

  async function test(): Promise<AiRuntimeTestResult> {
    testing.value = true
    try {
      return await testAiConfig({ ...form })
    } finally {
      testing.value = false
    }
  }

  return {
    config,
    form,
    loaded,
    loading,
    refresh,
    save,
    saving,
    test,
    testing,
  }
}
