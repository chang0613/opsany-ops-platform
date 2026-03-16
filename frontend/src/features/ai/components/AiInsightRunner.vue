<script setup lang="ts">
import type { AiInsightTemplate } from '../constants'

const props = withDefaults(defineProps<{
  input: string
  running?: boolean
  selectedJobType: string
  templates: AiInsightTemplate[]
  compact?: boolean
}>(), {
  running: false,
  compact: false,
})

const emit = defineEmits<{
  (event: 'update:input', value: string): void
  (event: 'update:selectedJobType', value: string): void
  (event: 'apply-template', value: string): void
  (event: 'run', value?: string): void
}>()
</script>

<template>
  <section class="insight-runner" :class="{ 'is-compact': props.compact }">
    <div class="insight-runner__quick-actions">
      <el-button type="primary" plain :disabled="props.running" @click="emit('run', 'alarm_summary')">告警摘要</el-button>
      <el-button plain :disabled="props.running" @click="emit('run', 'root_cause')">根因分析</el-button>
      <el-button plain :disabled="props.running" @click="emit('run', 'log_cluster')">日志聚类</el-button>
    </div>

    <div class="insight-runner__form">
      <el-form-item label="洞察模板">
        <el-select
          :model-value="props.selectedJobType"
          class="insight-runner__select"
          @update:model-value="emit('update:selectedJobType', $event)"
          @change="emit('apply-template', String($event))"
        >
          <el-option v-for="item in props.templates" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <p class="insight-runner__hint">{{ props.templates.find((item) => item.value === props.selectedJobType)?.description }}</p>
      </el-form-item>

      <div class="insight-runner__actions">
        <el-button plain @click="emit('apply-template', props.selectedJobType)">填充示例</el-button>
        <el-button type="primary" :loading="props.running" @click="emit('run')">运行分析</el-button>
      </div>
    </div>

    <el-form-item label="输入上下文">
      <el-input
        :model-value="props.input"
        type="textarea"
        :autosize="{ minRows: props.compact ? 8 : 12, maxRows: props.compact ? 14 : 18 }"
        placeholder="输入 JSON、日志片段、监控上下文或工单内容"
        @update:model-value="emit('update:input', $event)"
      />
    </el-form-item>
  </section>
</template>

<style scoped>
.insight-runner {
  display: grid;
  gap: 16px;
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(29, 40, 56, 0.08);
}

.insight-runner.is-compact {
  padding: 16px;
  border-radius: 20px;
}

.insight-runner__quick-actions,
.insight-runner__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.insight-runner__form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: end;
}

.insight-runner__select {
  width: 100%;
}

.insight-runner__hint {
  margin: 8px 0 0;
  color: #68717f;
  font-size: 13px;
}

@media (max-width: 900px) {
  .insight-runner__form {
    grid-template-columns: 1fr;
  }
}
</style>
