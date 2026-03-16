<script setup lang="ts">
const props = defineProps<{
  modelValue: boolean
  saving?: boolean
  form: {
    title: string
    content: string
    tags: string
  }
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'save'): void
}>()
</script>

<template>
  <el-dialog :model-value="props.modelValue" width="720px" class="config-dialog" @update:model-value="emit('update:modelValue', $event)">
    <template #header>
      <div class="modal-header">
        <div>
          <h3>新增知识条目</h3>
          <p>把常见故障、根因、验证动作和处理方案沉淀成可检索知识。</p>
        </div>
      </div>
    </template>

    <el-form label-position="top" class="modal-body">
      <el-form-item label="标题">
        <el-input v-model="props.form.title" placeholder="例如：MySQL 连接耗尽排查步骤" />
      </el-form-item>
      <el-form-item label="标签">
        <el-input v-model="props.form.tags" placeholder="例如：mysql, 连接池, 生产" />
      </el-form-item>
      <el-form-item label="内容">
        <el-input v-model="props.form.content" type="textarea" :rows="8" placeholder="记录现象、根因、验证方法和处置步骤" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="modal-footer">
        <el-button @click="emit('update:modelValue', false)">取消</el-button>
        <el-button type="success" :loading="props.saving" @click="emit('save')">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>
