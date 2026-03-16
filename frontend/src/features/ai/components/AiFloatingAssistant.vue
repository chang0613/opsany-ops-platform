<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAiConfig } from '../composables/useAiConfig'
import AiAssistantWorkspace from './AiAssistantWorkspace.vue'
import AiConfigDialog from './AiConfigDialog.vue'
import AiInsightsWorkspace from './AiInsightsWorkspace.vue'
import AiKnowledgeWorkspace from './AiKnowledgeWorkspace.vue'

type FloatingTab = 'assistant' | 'insights' | 'knowledge'

const route = useRoute()
const router = useRouter()

const STORAGE_OPEN_KEY = 'opsany.ai.floating.open'
const STORAGE_TAB_KEY = 'opsany.ai.floating.tab'

const tabs: Array<{ key: FloatingTab; label: string; hint: string; route: string }> = [
  { key: 'assistant', label: '对话', hint: '问答与诊断', route: '/ai/assistant' },
  { key: 'insights', label: '洞察', hint: '快捷分析', route: '/ai/insights' },
  { key: 'knowledge', label: '知识', hint: '沉淀经验', route: '/ai/knowledge' },
]

function readStoredOpen(): boolean {
  if (typeof window === 'undefined') {
    return false
  }
  return window.localStorage.getItem(STORAGE_OPEN_KEY) === '1'
}

function readStoredTab(): FloatingTab {
  if (typeof window === 'undefined') {
    return 'assistant'
  }

  const stored = window.localStorage.getItem(STORAGE_TAB_KEY)
  return tabs.some((item) => item.key === stored) ? (stored as FloatingTab) : 'assistant'
}

const open = ref(readStoredOpen())
const activeTab = ref<FloatingTab>(readStoredTab())
const configDialogOpen = ref(false)
const defaultTab = tabs[0]!

const { config, form, loading, refresh, save, saving, test, testing } = useAiConfig()

const activeTabMeta = computed<(typeof tabs)[number]>(() => tabs.find((item) => item.key === activeTab.value) ?? defaultTab)
const showLauncherBadge = computed(() => route.path.startsWith('/ai/'))
const configStatusText = computed(() => {
  if (!config.value) {
    return '未加载'
  }
  if (config.value.provider === 'mock') {
    return 'Mock 模式'
  }
  return config.value.configured ? '已配置' : '待配置'
})

watch(open, (value) => {
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(STORAGE_OPEN_KEY, value ? '1' : '0')
  }
})

watch(activeTab, (value) => {
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(STORAGE_TAB_KEY, value)
  }
})

onMounted(async () => {
  try {
    await refresh()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载 AI 配置失败')
  }
})

function toggleOpen(): void {
  open.value = !open.value
}

function switchTab(tab: FloatingTab): void {
  activeTab.value = tab
}

function openCurrentRoute(): void {
  void router.push(activeTabMeta.value.route)
}

async function handleSaveConfig(): Promise<void> {
  try {
    await save()
    ElMessage.success('AI 配置已保存并立即生效')
    configDialogOpen.value = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存 AI 配置失败')
  }
}

async function handleTestConfig(): Promise<void> {
  try {
    const result = await test()
    if (result.success) {
      ElMessage.success(result.message)
      return
    }
    ElMessage.error(result.message)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '测试连接失败')
  }
}
</script>

<template>
  <div class="floating-ai">
    <transition name="floating-ai-panel">
      <aside v-if="open" class="floating-ai__panel">
        <header class="floating-ai__header">
          <div class="floating-ai__brand">
            <div class="floating-ai__brand-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none">
                <path d="M12 2 14.6 8.1 21 10.7 14.6 13.3 12 19.4 9.4 13.3 3 10.7 9.4 8.1 12 2Z" />
              </svg>
            </div>
            <div>
              <strong>智能助手</strong>
              <p>{{ activeTabMeta.hint }}</p>
            </div>
          </div>
          <div class="floating-ai__header-actions">
            <button class="floating-ai__link" type="button" @click="configDialogOpen = true">配置</button>
            <button class="floating-ai__link" type="button" @click="openCurrentRoute">展开页面</button>
            <button class="floating-ai__close" type="button" @click="toggleOpen">收起</button>
          </div>
        </header>

        <div class="floating-ai__status-bar">
          <el-tag :type="config?.provider === 'bigmodel' && config?.configured ? 'success' : config?.provider === 'mock' ? 'info' : 'warning'" effect="light" round>
            {{ configStatusText }}
          </el-tag>
          <span>
            {{ config?.provider === 'bigmodel' ? `模型/智能体: ${config?.model || '--'}` : '当前使用 Mock Provider' }}
          </span>
        </div>

        <nav class="floating-ai__tabs">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="floating-ai__tab"
            :class="{ 'is-active': activeTab === tab.key }"
            type="button"
            @click="switchTab(tab.key)"
          >
            <strong>{{ tab.label }}</strong>
            <span>{{ tab.hint }}</span>
          </button>
        </nav>

        <div class="floating-ai__body">
          <AiAssistantWorkspace v-if="activeTab === 'assistant'" compact />
          <AiInsightsWorkspace v-else-if="activeTab === 'insights'" compact />
          <AiKnowledgeWorkspace v-else compact />
        </div>
      </aside>
    </transition>

    <button class="floating-ai__launcher" type="button" @click="toggleOpen">
      <span v-if="showLauncherBadge" class="floating-ai__badge">AI</span>
      <span class="floating-ai__launcher-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none">
          <path d="M12 2 14.6 8.1 21 10.7 14.6 13.3 12 19.4 9.4 13.3 3 10.7 9.4 8.1 12 2Z" />
        </svg>
      </span>
      <span class="floating-ai__launcher-copy">
        <strong>{{ open ? '收起智能助手' : '打开智能助手' }}</strong>
        <small>对话、洞察、知识一体化入口</small>
      </span>
    </button>

    <AiConfigDialog
      v-model="configDialogOpen"
      :config="config"
      :form="form"
      :loading="loading"
      :saving="saving"
      :testing="testing"
      @save="handleSaveConfig"
      @test="handleTestConfig"
    />
  </div>
</template>

<style scoped>
.floating-ai {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 60;
  pointer-events: none;
}

.floating-ai__panel,
.floating-ai__launcher {
  pointer-events: auto;
}

.floating-ai__panel {
  width: min(460px, calc(100vw - 24px));
  max-height: min(84vh, 920px);
  margin-left: auto;
  margin-bottom: 14px;
  border-radius: 28px;
  overflow: hidden;
  border: 1px solid rgba(23, 47, 53, 0.12);
  background:
    radial-gradient(circle at top right, rgba(241, 189, 104, 0.18), transparent 28%),
    linear-gradient(180deg, rgba(255, 252, 247, 0.98), rgba(245, 239, 229, 0.98));
  box-shadow: 0 28px 72px rgba(16, 28, 35, 0.24);
  backdrop-filter: blur(18px);
}

.floating-ai__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px 18px 12px;
}

.floating-ai__brand {
  display: flex;
  gap: 12px;
  align-items: center;
}

.floating-ai__brand-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(135deg, #113941, #1f616b);
}

.floating-ai__brand-icon svg,
.floating-ai__launcher-icon svg {
  width: 22px;
  height: 22px;
  stroke: currentColor;
  stroke-width: 1.6;
}

.floating-ai__brand strong {
  display: block;
  font-size: 16px;
  color: #102129;
}

.floating-ai__brand p {
  margin: 4px 0 0;
  color: #60707c;
  font-size: 12px;
}

.floating-ai__header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.floating-ai__status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 18px 12px;
  color: #60707c;
  font-size: 12px;
}

.floating-ai__link,
.floating-ai__close {
  border: 0;
  background: rgba(255, 255, 255, 0.8);
  color: #1b5965;
  border-radius: 999px;
  padding: 8px 12px;
  cursor: pointer;
}

.floating-ai__tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding: 0 18px 14px;
}

.floating-ai__tab {
  border: 1px solid rgba(29, 40, 56, 0.08);
  background: rgba(255, 255, 255, 0.74);
  border-radius: 18px;
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
}

.floating-ai__tab strong {
  display: block;
  color: #1d2838;
}

.floating-ai__tab span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #60707c;
}

.floating-ai__tab.is-active {
  background: linear-gradient(135deg, #1b5965, #23424c);
}

.floating-ai__tab.is-active strong,
.floating-ai__tab.is-active span {
  color: #fff;
}

.floating-ai__body {
  max-height: calc(min(84vh, 920px) - 192px);
  overflow: auto;
  padding: 0 18px 18px;
}

.floating-ai__launcher {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
  position: relative;
  min-width: 238px;
  border: 0;
  border-radius: 999px;
  padding: 12px 16px 12px 12px;
  background: linear-gradient(135deg, #143b44, #225762);
  color: #fff;
  box-shadow: 0 22px 48px rgba(17, 39, 46, 0.32);
  cursor: pointer;
}

.floating-ai__launcher-icon {
  flex: none;
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
  color: #fff4e0;
}

.floating-ai__launcher-copy {
  display: grid;
  text-align: left;
}

.floating-ai__launcher-copy strong {
  font-size: 14px;
}

.floating-ai__launcher-copy small {
  color: rgba(255, 255, 255, 0.82);
}

.floating-ai__badge {
  position: absolute;
  top: -6px;
  right: 12px;
  padding: 4px 8px;
  border-radius: 999px;
  background: #f0b45e;
  color: #102129;
  font-size: 11px;
  font-weight: 700;
}

.floating-ai-panel-enter-active,
.floating-ai-panel-leave-active {
  transition: all 0.22s ease;
}

.floating-ai-panel-enter-from,
.floating-ai-panel-leave-to {
  opacity: 0;
  transform: translateY(14px) scale(0.98);
}

@media (max-width: 768px) {
  .floating-ai {
    right: 12px;
    left: 12px;
    bottom: 12px;
  }

  .floating-ai__panel {
    width: 100%;
  }

  .floating-ai__launcher {
    width: 100%;
    min-width: 0;
  }

  .floating-ai__tabs {
    grid-template-columns: 1fr;
  }

  .floating-ai__status-bar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
