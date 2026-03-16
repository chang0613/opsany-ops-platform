import { ref } from 'vue'

import { createAiJob, listAiJobs } from '../../../lib/api'
import type { AiJob } from '../../../types/ai'
import { AI_INSIGHT_TEMPLATES } from '../constants'
import { toJsonInput } from '../utils'

const DEFAULT_TEMPLATE = AI_INSIGHT_TEMPLATES[0]!

export function useAiInsights() {
  const jobs = ref<AiJob[]>([])
  const running = ref(false)
  const loading = ref(false)
  const initialized = ref(false)
  const selectedJobType = ref(DEFAULT_TEMPLATE.value)
  const input = ref(toJsonInput(DEFAULT_TEMPLATE.createSample()))

  async function refresh(force = false): Promise<void> {
    if (loading.value && !force) {
      return
    }

    loading.value = true
    try {
      jobs.value = await listAiJobs()
      initialized.value = true
    } finally {
      loading.value = false
    }
  }

  function applyTemplate(jobType: string): void {
    const template = AI_INSIGHT_TEMPLATES.find((item) => item.value === jobType) ?? DEFAULT_TEMPLATE
    selectedJobType.value = template.value
    input.value = toJsonInput(template.createSample())
  }

  async function run(jobType?: string): Promise<AiJob> {
    const target = jobType || selectedJobType.value
    if (jobType) {
      selectedJobType.value = target
    }

    running.value = true
    try {
      const job = await createAiJob(target, input.value.trim())
      await refresh(true)
      return job
    } finally {
      running.value = false
    }
  }

  return {
    applyTemplate,
    initialized,
    input,
    jobs,
    loading,
    refresh,
    run,
    running,
    selectedJobType,
    templates: AI_INSIGHT_TEMPLATES,
  }
}
