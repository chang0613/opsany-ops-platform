<script setup lang="ts">
const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

function toggle(value: string | number | boolean) {
  emit('update:modelValue', Boolean(value))
}
</script>

<template>
  <el-switch
    :model-value="modelValue"
    inline-prompt
    active-text="开"
    inactive-text="关"
    @change="toggle"
  />
</template>
