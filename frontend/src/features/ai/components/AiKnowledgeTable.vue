<script setup lang="ts">
import type { AiKnowledgeEntry } from '../../../types/ai'

const props = withDefaults(defineProps<{
  items: AiKnowledgeEntry[]
  compact?: boolean
}>(), {
  compact: false,
})

const emit = defineEmits<{
  (event: 'create'): void
  (event: 'delete', id: number): void
}>()
</script>

<template>
  <section class="knowledge-table" :class="{ 'is-compact': props.compact }">
    <div class="knowledge-table__header">
      <div>
        <p>知识沉淀</p>
        <strong>支持从告警、工单和排障经验中持续积累可检索知识。</strong>
      </div>
      <el-button type="primary" @click="emit('create')">新增条目</el-button>
    </div>

    <div v-if="props.compact" class="knowledge-table__cards">
      <article v-for="item in props.items" :key="item.id" class="knowledge-table__card">
        <div class="knowledge-table__card-header">
          <strong>{{ item.title }}</strong>
          <button class="danger-link" @click="emit('delete', item.id)">删除</button>
        </div>
        <p class="knowledge-table__tags">{{ item.tags || '未设置标签' }}</p>
        <p class="knowledge-table__content">{{ item.content }}</p>
        <span class="knowledge-table__meta">{{ item.createdAt }}</span>
      </article>
      <div v-if="!props.items.length" class="knowledge-table__empty">还没有知识条目，先沉淀一条常见故障处理方案吧。</div>
    </div>

    <table v-else>
      <thead>
        <tr>
          <th>标题</th>
          <th>标签</th>
          <th>来源</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in props.items" :key="item.id">
          <td>{{ item.title }}</td>
          <td class="mono">{{ item.tags || '--' }}</td>
          <td class="mono">{{ item.sourceType || '--' }} {{ item.sourceId || '' }}</td>
          <td>{{ item.createdAt }}</td>
          <td>
            <button class="danger-link" @click="emit('delete', item.id)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.knowledge-table {
  overflow: auto;
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(29, 40, 56, 0.08);
}

.knowledge-table.is-compact {
  padding: 16px;
  border-radius: 20px;
}

.knowledge-table__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
}

.knowledge-table__header p {
  margin: 0;
  color: #8f6233;
  font-size: 12px;
}

.knowledge-table__cards {
  display: grid;
  gap: 12px;
}

.knowledge-table__card {
  padding: 14px;
  border-radius: 18px;
  background: #f8f4eb;
  border: 1px solid rgba(29, 40, 56, 0.08);
}

.knowledge-table__card-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.knowledge-table__tags,
.knowledge-table__meta {
  margin: 8px 0 0;
  color: #68717f;
  font-size: 12px;
}

.knowledge-table__content {
  margin: 8px 0 0;
  color: #1d2838;
  line-height: 1.6;
  white-space: pre-wrap;
}

.knowledge-table__empty {
  color: #68717f;
  text-align: center;
  padding: 18px 0 6px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 16px;
  border-bottom: 1px solid rgba(29, 40, 56, 0.08);
  text-align: left;
}

th {
  color: #68717f;
  font-size: 12px;
}

.mono {
  font-family: 'Consolas', 'SFMono-Regular', monospace;
}

.danger-link {
  border: 0;
  background: transparent;
  color: #b5483d;
  cursor: pointer;
}
</style>
