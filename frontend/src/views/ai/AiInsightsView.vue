<script setup lang="ts">
import AiModuleShell from '../../features/ai/components/AiModuleShell.vue'
import AiInsightsWorkspace from '../../features/ai/components/AiInsightsWorkspace.vue'
</script>

<template>
  <AiModuleShell
    title="智能洞察"
    description="通过模板化任务快速生成告警摘要、根因分析、日志聚类、容量规划等结果。右下角助手也可以直接运行这些能力。"
  >
    <AiInsightsWorkspace />
  </AiModuleShell>
</template>
