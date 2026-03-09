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
  <Teleport to="body">
    <div v-if="open" class="modal-mask" @click.self="emit('close')">
      <div class="modal-card">
        <div class="modal-header">
          <div>
            <h3>提交工单</h3>
            <p>这一步会写入 MySQL，并通过 RabbitMQ 触发任务和消息。</p>
          </div>
          <button class="icon-close" @click="emit('close')">×</button>
        </div>

        <div class="modal-body">
          <label class="form-field">
            <span>工单标题</span>
            <input v-model="form.title" class="text-input" placeholder="请输入工单标题" />
          </label>

          <div class="form-grid">
            <label class="form-field">
              <span>工单类型</span>
              <select v-model="form.type" class="select-input">
                <option>请求管理</option>
                <option>变更管理</option>
                <option>事件管理</option>
              </select>
            </label>

            <label class="form-field">
              <span>优先级</span>
              <select v-model="form.priority" class="select-input">
                <option>高</option>
                <option>中</option>
                <option>低</option>
              </select>
            </label>
          </div>

          <label class="form-field">
            <span>关联服务</span>
            <select v-model="form.serviceName" class="select-input">
              <option v-for="item in services" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>

          <label class="form-field">
            <span>工单说明</span>
            <textarea v-model="form.description" class="text-area" placeholder="补充工单背景、目标和实施说明"></textarea>
          </label>
        </div>

        <div class="modal-footer">
          <button class="action-btn" @click="emit('close')">取消</button>
          <button class="action-btn primary" :disabled="!form.title.trim()" @click="submit">提交工单</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
