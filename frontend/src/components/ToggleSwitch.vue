<script setup lang="ts">
const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

function toggle() {
  emit('update:modelValue', !props.modelValue)
}
</script>

<template>
  <button class="toggle-switch" :class="{ on: modelValue }" @click="toggle">
    <span class="toggle-handle"></span>
    <span class="toggle-text">{{ modelValue ? '开' : '关' }}</span>
  </button>
</template>
