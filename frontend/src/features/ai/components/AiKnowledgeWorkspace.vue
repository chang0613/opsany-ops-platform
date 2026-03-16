<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'

import { useAiKnowledge } from '../composables/useAiKnowledge'
import AiKnowledgeDialog from './AiKnowledgeDialog.vue'
import AiKnowledgeTable from './AiKnowledgeTable.vue'

const props = withDefaults(defineProps<{
  compact?: boolean
}>(), {
  compact: false,
})

const {
  closeDialog,
  dialogOpen,
  form,
  items,
  openCreateDialog,
  refresh,
  remove,
  save,
  saving,
} = useAiKnowledge()

const keyword = ref('')

const filteredItems = computed(() => {
  const normalized = keyword.value.trim().toLowerCase()
  if (!normalized) {
    return items.value
  }

  return items.value.filter((item) => {
    const source = `${item.title} ${item.tags || ''} ${item.content}`.toLowerCase()
    return source.includes(normalized)
  })
})

onMounted(async () => {
  try {
    await refresh()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载知识库失败')
  }
})

async function handleSave(): Promise<void> {
  try {
    await save()
    ElMessage.success('知识条目已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存知识条目失败')
  }
}

async function handleDelete(id: number): Promise<void> {
  if (!window.confirm('确认删除该知识条目吗？')) {
    return
  }

  try {
    await remove(id)
    ElMessage.success('知识条目已删除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除知识条目失败')
  }
}
</script>

<template>
  <div class="knowledge-workspace">
    <section class="knowledge-workspace__hero knowledge-workspace__panel">
      <div>
        <p class="knowledge-workspace__eyebrow">知识库</p>
        <h3>把处置经验沉淀成团队可复用的故障知识</h3>
      </div>
      <div class="knowledge-workspace__actions">
        <el-input v-model="keyword" placeholder="搜索标题、标签或内容" clearable />
        <el-button type="primary" @click="openCreateDialog">新增条目</el-button>
      </div>
    </section>

    <AiKnowledgeTable :items="filteredItems" :compact="props.compact" @create="openCreateDialog" @delete="handleDelete" />
    <AiKnowledgeDialog
      :model-value="dialogOpen"
      :form="form"
      :saving="saving"
      @save="handleSave"
      @update:model-value="$event ? openCreateDialog() : closeDialog()"
    />
  </div>
</template>

<style scoped>
.knowledge-workspace {
  display: grid;
  gap: 16px;
}

.knowledge-workspace__panel {
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(29, 40, 56, 0.08);
}

.knowledge-workspace__hero {
  display: grid;
  gap: 16px;
}

.knowledge-workspace__eyebrow {
  margin: 0 0 8px;
  color: #8f6233;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.knowledge-workspace__hero h3 {
  margin: 0;
  font-size: 22px;
}

.knowledge-workspace__actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}

@media (max-width: 900px) {
  .knowledge-workspace__actions {
    grid-template-columns: 1fr;
  }
}
</style>
