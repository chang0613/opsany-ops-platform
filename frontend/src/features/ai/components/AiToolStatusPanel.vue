<script setup lang="ts">
import type { AiAgentStep } from '../../../types/ai'

const props = withDefaults(defineProps<{
  steps: AiAgentStep[]
  active?: boolean
  compact?: boolean
}>(), {
  active: false,
  compact: false,
})

function statusLabel(status: AiAgentStep['status']): string {
  if (status === 'completed') {
    return '已完成'
  }
  if (status === 'in_progress') {
    return '进行中'
  }
  return '待执行'
}
</script>

<template>
  <section class="tool-panel" :class="{ 'is-compact': props.compact }">
    <div class="tool-panel__header">
      <div>
        <p>工具调用轨迹</p>
        <strong>{{ props.active ? '正在执行 Agent 流程' : '最近一次执行轨迹' }}</strong>
      </div>
      <el-tag :type="props.active ? 'warning' : 'success'" effect="light" round>
        {{ props.active ? '运行中' : '空闲' }}
      </el-tag>
    </div>

    <div v-if="props.steps.length" class="tool-panel__list">
      <article v-for="step in props.steps" :key="step.id" class="tool-panel__item" :class="[`is-${step.status}`]">
        <div class="tool-panel__item-top">
          <div class="tool-panel__dot"></div>
          <div>
            <strong>{{ step.title }}</strong>
            <span>{{ statusLabel(step.status) }}</span>
          </div>
        </div>
        <p>{{ step.detail }}</p>
        <small v-if="step.tool">工具: {{ step.tool }}</small>
      </article>
    </div>

    <div v-else class="tool-panel__empty">发送一条消息后，这里会显示知识检索、上下文编排和回复生成的执行状态。</div>
  </section>
</template>

<style scoped>
.tool-panel {
  display: grid;
  gap: 12px;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(29, 40, 56, 0.08);
}

.tool-panel.is-compact {
  padding: 16px;
  border-radius: 20px;
}

.tool-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.tool-panel__header p {
  margin: 0;
  color: #8f6233;
  font-size: 12px;
}

.tool-panel__list {
  display: grid;
  gap: 10px;
}

.tool-panel__item {
  padding: 12px 14px;
  border-radius: 18px;
  border: 1px solid rgba(29, 40, 56, 0.08);
  background: rgba(248, 244, 235, 0.9);
}

.tool-panel__item.is-in_progress {
  background: rgba(229, 240, 242, 0.95);
  border-color: rgba(27, 89, 101, 0.18);
}

.tool-panel__item.is-completed {
  background: rgba(238, 245, 244, 0.95);
}

.tool-panel__item-top {
  display: flex;
  gap: 10px;
  align-items: center;
}

.tool-panel__item-top strong,
.tool-panel__item-top span,
.tool-panel__item p,
.tool-panel__item small {
  display: block;
}

.tool-panel__item-top span,
.tool-panel__item p,
.tool-panel__item small,
.tool-panel__empty {
  color: #68717f;
}

.tool-panel__item p {
  margin: 8px 0 0;
  line-height: 1.6;
}

.tool-panel__item small {
  margin-top: 8px;
}

.tool-panel__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #c0c8d1;
}

.tool-panel__item.is-in_progress .tool-panel__dot {
  background: #1b5965;
  box-shadow: 0 0 0 6px rgba(27, 89, 101, 0.12);
}

.tool-panel__item.is-completed .tool-panel__dot {
  background: #2d8b6d;
}

.tool-panel__empty {
  text-align: center;
  padding: 10px 0 4px;
}
</style>
