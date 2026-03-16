<script setup lang="ts">
import AiKnowledgeWorkspace from '../../features/ai/components/AiKnowledgeWorkspace.vue'
import AiModuleShell from '../../features/ai/components/AiModuleShell.vue'
</script>

<template>
  <AiModuleShell
    title="智能知识库"
    description="把工单处理经验、告警处置方案和排障方法沉淀成可检索的知识条目，为问答与洞察持续提供上下文增强。"
  >
    <AiKnowledgeWorkspace />
  </AiModuleShell>
</template>
