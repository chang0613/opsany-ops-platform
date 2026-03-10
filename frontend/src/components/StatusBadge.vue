<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  label: string
}>()

const tone = computed(() => {
  if (props.label.includes('进行') || props.label.includes('启用') || props.label.includes('已发布') || props.label.includes('完成')) {
    return 'success'
  }
  if (props.label.includes('待')) {
    return 'warning'
  }
  if (props.label.includes('未') || props.label.includes('暂停') || props.label.includes('关闭')) {
    return 'info'
  }
  return 'danger'
})
</script>

<template>
  <el-tag :type="tone" effect="light" round>{{ label }}</el-tag>
</template>
