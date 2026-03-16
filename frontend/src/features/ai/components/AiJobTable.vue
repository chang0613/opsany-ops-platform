<script setup lang="ts">
import { computed } from 'vue'

import type { AiJob } from '../../../types/ai'
import { prettyJson } from '../utils'

const props = withDefaults(defineProps<{
  jobs: AiJob[]
  compact?: boolean
}>(), {
  compact: false,
})

const visibleJobs = computed(() => (props.compact ? props.jobs.slice(0, 4) : props.jobs))
</script>

<template>
  <section class="job-table" :class="{ 'is-compact': props.compact }">
    <div v-if="props.compact" class="job-table__cards">
      <article v-for="job in visibleJobs" :key="job.id" class="job-table__card">
        <div class="job-table__card-header">
          <strong>{{ job.jobType }}</strong>
          <el-tag :type="job.status === 'DONE' ? 'success' : job.status === 'FAILED' ? 'danger' : 'warning'" effect="light" round>
            {{ job.status }}
          </el-tag>
        </div>
        <p class="job-table__time">{{ job.updatedAt }}</p>
        <pre class="mono">{{ prettyJson(job.resultJson) }}</pre>
      </article>
      <div v-if="!visibleJobs.length" class="job-table__empty">还没有洞察任务，先运行一个模板试试。</div>
    </div>

    <table v-else>
      <thead>
        <tr>
          <th>ID</th>
          <th>任务类型</th>
          <th>状态</th>
          <th>创建时间</th>
          <th>更新时间</th>
          <th>结果</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="job in visibleJobs" :key="job.id">
          <td class="mono">{{ job.id }}</td>
          <td>{{ job.jobType }}</td>
          <td>
            <el-tag :type="job.status === 'DONE' ? 'success' : job.status === 'FAILED' ? 'danger' : 'warning'" effect="light" round>
              {{ job.status }}
            </el-tag>
          </td>
          <td>{{ job.createdAt }}</td>
          <td>{{ job.updatedAt }}</td>
          <td><pre class="mono">{{ prettyJson(job.resultJson) }}</pre></td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.job-table {
  overflow: auto;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(29, 40, 56, 0.08);
}

.job-table.is-compact {
  padding: 16px;
  border-radius: 20px;
}

.job-table__cards {
  display: grid;
  gap: 12px;
}

.job-table__card {
  padding: 14px;
  border-radius: 18px;
  background: #f8f4eb;
  border: 1px solid rgba(29, 40, 56, 0.08);
}

.job-table__card-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.job-table__time {
  margin: 8px 0 10px;
  color: #68717f;
  font-size: 12px;
}

.job-table__empty {
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
  vertical-align: top;
}

th {
  color: #68717f;
  font-size: 12px;
}

pre {
  margin: 0;
  white-space: pre-wrap;
}

.mono {
  font-family: 'Consolas', 'SFMono-Regular', monospace;
}
</style>
