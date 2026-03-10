<script setup lang="ts">
import { reactive, watch } from 'vue'

import type { WorkOrderPayload } from '../types/platform'

const props = defineProps<{
  open: boolean
  services: string[]
}>()

const emit = defineEmits<{
  close: []
  submit: [payload: WorkOrderPayload]
}>()

const form = reactive<WorkOrderPayload>({
  title: '',
  type: '请求管理',
  serviceName: '',
  description: '',
  priority: '中',
})

watch(
  () => props.open,
  (open) => {
    if (!open) {
      return
    }
    form.title = ''
    form.type = '请求管理'
    form.serviceName = props.services[0] ?? ''
    form.description = ''
    form.priority = '中'
  },
  { immediate: true },
)

function submit() {
  emit('submit', { ...form })
}
</script>

<template>
  <el-dialog
    :model-value="open"
    width="640px"
    destroy-on-close
    class="order-dialog"
    @close="emit('close')"
  >
    <template #header>
      <div class="modal-header">
        <div>
          <h3>提交工单</h3>
          <p>这一步会写入 MySQL，并通过 RabbitMQ 触发任务和消息。</p>
        </div>
      </div>
    </template>

    <el-form label-position="top" class="modal-body">
      <el-form-item label="工单标题">
        <el-input v-model="form.title" placeholder="请输入工单标题" />
      </el-form-item>

      <div class="form-grid">
        <el-form-item label="工单类型">
          <el-select v-model="form.type" class="full-width">
            <el-option label="请求管理" value="请求管理" />
            <el-option label="变更管理" value="变更管理" />
            <el-option label="事件管理" value="事件管理" />
          </el-select>
        </el-form-item>

        <el-form-item label="优先级">
          <el-select v-model="form.priority" class="full-width">
            <el-option label="高" value="高" />
            <el-option label="中" value="中" />
            <el-option label="低" value="低" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="关联服务">
        <el-select v-model="form.serviceName" class="full-width">
          <el-option v-for="item in services" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>

      <el-form-item label="工单说明">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="补充工单背景、目标和实施说明"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="modal-footer">
        <el-button @click="emit('close')">取消</el-button>
        <el-button type="success" :disabled="!form.title.trim()" @click="submit">提交工单</el-button>
      </div>
    </template>
  </el-dialog>
</template>
