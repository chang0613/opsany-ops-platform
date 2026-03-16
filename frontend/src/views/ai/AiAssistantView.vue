<script setup lang="ts">
import AiModuleShell from '../../features/ai/components/AiModuleShell.vue'
import AiAssistantWorkspace from '../../features/ai/components/AiAssistantWorkspace.vue'
</script>

<template>
  <AiModuleShell
    title="对话式运维助手"
    description="面向告警、日志、工单和配置场景的多轮问答入口。右下角悬浮助手也复用同一套能力，你可以在任何页面边操作边提问。"
  >
    <AiAssistantWorkspace />
  </AiModuleShell>
</template>
