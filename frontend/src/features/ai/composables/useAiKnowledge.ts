import { reactive, ref } from 'vue'

import { createAiKnowledge, deleteAiKnowledge, listAiKnowledge } from '../../../lib/api'
import type { AiKnowledgeEntry } from '../../../types/ai'

export function useAiKnowledge() {
  const items = ref<AiKnowledgeEntry[]>([])
  const saving = ref(false)
  const loading = ref(false)
  const initialized = ref(false)
  const dialogOpen = ref(false)
  const form = reactive({
    title: '',
    content: '',
    tags: '',
  })

  async function refresh(force = false): Promise<void> {
    if (loading.value && !force) {
      return
    }

    loading.value = true
    try {
      items.value = await listAiKnowledge()
      initialized.value = true
    } finally {
      loading.value = false
    }
  }

  function openCreateDialog(): void {
    resetForm()
    dialogOpen.value = true
  }

  function closeDialog(): void {
    dialogOpen.value = false
    resetForm()
  }

  function resetForm(): void {
    form.title = ''
    form.content = ''
    form.tags = ''
  }

  async function save(): Promise<void> {
    saving.value = true
    try {
      await createAiKnowledge({
        title: form.title,
        content: form.content,
        tags: form.tags || undefined,
      })
      dialogOpen.value = false
      resetForm()
      await refresh(true)
    } finally {
      saving.value = false
    }
  }

  async function remove(id: number): Promise<void> {
    await deleteAiKnowledge(id)
    await refresh(true)
  }

  return {
    closeDialog,
    dialogOpen,
    form,
    initialized,
    items,
    loading,
    openCreateDialog,
    refresh,
    remove,
    resetForm,
    save,
    saving,
  }
}

