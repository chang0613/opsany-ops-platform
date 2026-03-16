<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted } from 'vue'

import { useAiInsights } from '../composables/useAiInsights'
import AiInsightRunner from './AiInsightRunner.vue'
import AiJobTable from './AiJobTable.vue'

const props = withDefaults(defineProps<{
  compact?: boolean
}>(), {
  compact: false,
})

const {
  applyTemplate,
  input,
  jobs,
  refresh,
  run,
  running,
  selectedJobType,
  templates,
} = useAiInsights()

const latestJob = computed(() => jobs.value[0] ?? null)

onMounted(async () => {
  try {
    await refresh()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载 AI 洞察失败')
  }
})

async function handleRun(jobType?: string): Promise<void> {
  try {
    const job = await run(jobType)
    if (job.status === 'DONE') {
      ElMessage.success('AI 洞察任务已完成分析')
      return
    }
    ElMessage.success('AI 洞察任务已提交，可在结果区查看处理进度')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '执行 AI 洞察失败')
  }
}
</script>

<template>
  <div class="insights-workspace" :class="{ 'is-compact': props.compact }">
    <section class="insights-workspace__hero insights-workspace__panel">
      <div>
        <p class="insights-workspace__eyebrow">智能洞察</p>
        <h3>把告警、日志、变更和工单输入转成结构化分析结论</h3>
      </div>
      <div class="insights-workspace__stats">
        <article>
          <span>可用模板</span>
          <strong>{{ templates.length }}</strong>
        </article>
        <article>
          <span>最近任务</span>
          <strong>{{ latestJob?.jobType || '暂无' }}</strong>
        </article>
        <article>
          <span>最近状态</span>
          <strong>{{ latestJob?.status || '--' }}</strong>
        </article>
      </div>
    </section>

    <AiInsightRunner
      v-model:input="input"
      v-model:selected-job-type="selectedJobType"
      :compact="props.compact"
      :running="running"
      :templates="templates"
      @apply-template="applyTemplate"
      @run="handleRun"
    />
    <AiJobTable :jobs="jobs" :compact="props.compact" />
  </div>
</template>

<style scoped>
.insights-workspace {
  display: grid;
  gap: 16px;
}

.insights-workspace__panel {
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(29, 40, 56, 0.08);
}

.insights-workspace__hero {
  display: grid;
  gap: 16px;
}

.insights-workspace__eyebrow {
  margin: 0 0 8px;
  color: #8f6233;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.insights-workspace__hero h3 {
  margin: 0;
  font-size: 22px;
  line-height: 1.4;
}

.insights-workspace__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.insights-workspace__stats article {
  padding: 14px;
  border-radius: 18px;
  background: #f8f4eb;
}

.insights-workspace__stats span {
  display: block;
  color: #68717f;
  font-size: 12px;
  margin-bottom: 6px;
}

.insights-workspace__stats strong {
  font-size: 16px;
}

@media (max-width: 900px) {
  .insights-workspace__stats {
    grid-template-columns: 1fr;
  }
}
</style>
