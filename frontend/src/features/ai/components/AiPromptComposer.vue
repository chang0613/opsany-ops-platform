<script setup lang="ts">
const props = withDefaults(defineProps<{
  disabled?: boolean
  loading?: boolean
  modelValue: string
  placeholder?: string
  suggestions?: string[]
  compact?: boolean
}>(), {
  disabled: false,
  loading: false,
  placeholder: '输入告警上下文、日志片段、工单描述或配置问题',
  suggestions: () => [],
  compact: false,
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
  (event: 'submit'): void
  (event: 'pick-suggestion', value: string): void
}>()
</script>

<template>
  <section class="prompt-composer" :class="{ 'is-compact': props.compact }">
    <div v-if="props.suggestions.length" class="prompt-composer__suggestions">
      <button
        v-for="item in props.suggestions"
        :key="item"
        class="prompt-composer__chip"
        type="button"
        @click="emit('pick-suggestion', item)"
      >
        {{ item }}
      </button>
    </div>

    <el-input
      :model-value="props.modelValue"
      type="textarea"
      :autosize="{ minRows: props.compact ? 3 : 4, maxRows: props.compact ? 7 : 9 }"
      :placeholder="props.placeholder"
      @update:model-value="emit('update:modelValue', $event)"
      @keydown.ctrl.enter.prevent="emit('submit')"
      @keydown.meta.enter.prevent="emit('submit')"
    />
    <div class="prompt-composer__footer">
      <p>建议补充影响范围、时间、关键日志和最近变更，回答会更准确。按 Ctrl/Cmd + Enter 可快捷发送。</p>
      <el-button type="primary" :disabled="props.disabled" :loading="props.loading" @click="emit('submit')">发送</el-button>
    </div>
  </section>
</template>

<style scoped>
.prompt-composer {
  display: grid;
  gap: 12px;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(29, 40, 56, 0.08);
}

.prompt-composer.is-compact {
  padding: 16px;
  border-radius: 20px;
}

.prompt-composer__suggestions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.prompt-composer__chip {
  border: 1px solid rgba(29, 40, 56, 0.08);
  background: rgba(247, 243, 234, 0.9);
  color: #23424c;
  border-radius: 999px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 12px;
}

.prompt-composer__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.prompt-composer__footer p {
  margin: 0;
  color: #68717f;
  font-size: 13px;
}

@media (max-width: 900px) {
  .prompt-composer__footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
