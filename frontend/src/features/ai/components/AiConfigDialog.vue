<script setup lang="ts">
import type { AiRuntimeConfig, AiRuntimeConfigUpdate } from '../../../types/ai'

const props = defineProps<{
  modelValue: boolean
  loading?: boolean
  saving?: boolean
  testing?: boolean
  config: AiRuntimeConfig | null
  form: AiRuntimeConfigUpdate
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'save'): void
  (event: 'test'): void
}>()
</script>

<template>
  <el-dialog :model-value="props.modelValue" width="760px" @update:model-value="emit('update:modelValue', $event)">
    <template #header>
      <div class="config-header">
        <div>
          <h3>AI 连接配置</h3>
          <p>配置 URL、API Key 和模型/智能体 ID 后即可直接使用。</p>
        </div>
        <el-tag :type="props.config?.configured ? 'success' : 'warning'" effect="light" round>
          {{ props.config?.configured ? '已配置' : '待配置' }}
        </el-tag>
      </div>
    </template>

    <el-skeleton v-if="props.loading" :rows="8" animated />
    <el-form v-else label-position="top">
      <div class="config-grid">
        <el-form-item label="Provider">
          <el-select v-model="props.form.provider" class="full-width">
            <el-option label="BigModel" value="bigmodel" />
            <el-option label="Mock" value="mock" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识库增强">
          <el-switch v-model="props.form.knowledgeContextEnabled" inline-prompt active-text="开" inactive-text="关" />
        </el-form-item>
      </div>

      <el-form-item label="接口 URL">
        <el-input v-model="props.form.baseUrl" placeholder="https://open.bigmodel.cn/api/v1/agents" />
      </el-form-item>

      <div class="config-grid">
        <el-form-item label="API Key">
          <el-input
            v-model="props.form.apiKey"
            type="password"
            show-password
            :placeholder="props.config?.apiKeyConfigured ? `已保存: ${props.config.apiKeyMasked}` : '请输入 API Key'"
          />
        </el-form-item>
        <el-form-item label="模型 / 智能体 ID">
          <el-input v-model="props.form.model" placeholder="例如：service_check_agent" />
        </el-form-item>
      </div>

      <div class="config-grid">
        <el-form-item label="连接超时 (ms)">
          <el-input-number v-model="props.form.connectTimeoutMs" :min="1000" :step="1000" class="full-width" />
        </el-form-item>
        <el-form-item label="读取超时 (ms)">
          <el-input-number v-model="props.form.readTimeoutMs" :min="5000" :step="1000" class="full-width" />
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <div class="config-footer">
        <el-button @click="emit('update:modelValue', false)">关闭</el-button>
        <el-button :loading="props.testing" @click="emit('test')">测试连接</el-button>
        <el-button type="primary" :loading="props.saving" @click="emit('save')">保存并生效</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.config-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.config-header h3 {
  margin: 0;
}

.config-header p {
  margin: 8px 0 0;
  color: #68717f;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.full-width {
  width: 100%;
}

.config-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
