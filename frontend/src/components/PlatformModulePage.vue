<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import { savePlatformPageState } from '../lib/api'

type CellValue = string | number | boolean | null | undefined

interface ModuleColumn {
  key: string
  label: string
}

interface ModuleRow {
  __id: string
  [key: string]: CellValue
}

interface ModuleTable {
  key: string
  title: string
  subtitle?: string
  emptyText?: string
  columns: ModuleColumn[]
  rows: ModuleRow[]
  filters: string[]
  readOnly: boolean
  entityName: string
}

const props = defineProps<{
  page: Record<string, any>
  pageKey: string
  platformKey?: string
}>()

const emit = defineEmits<{
  action: [value: string]
  navigate: [value: string]
}>()

const normalizedTables = ref<ModuleTable[]>([])
const tableRows = ref<Record<string, ModuleRow[]>>({})
const searchQuery = ref('')
const activeFilter = ref('')
const dialogOpen = ref(false)
const drawerOpen = ref(false)
const editorMode = ref<'create' | 'edit'>('create')
const currentTableKey = ref('')
const currentRowId = ref('')
const drawerTitle = ref('')
const drawerRows = ref<Array<{ label: string; value: string }>>([])
const formModel = reactive<Record<string, string>>({})
const savingState = ref(false)

const toolbarActions = computed(() => {
  const actions = normalizeActionLabels(props.page.actions)
  if (actions.length > 0) {
    return actions
  }
  const primaryTable = normalizedTables.value.find((table) => !table.readOnly)
  if (!primaryTable || primaryTable.readOnly) {
    return []
  }
  return [`新增${primaryTable.entityName}`]
})

const pageFilters = computed(() => {
  const filters = Array.isArray(props.page.filters) ? props.page.filters.map((item) => String(item)) : []
  return filters
})

const showToolbar = computed(() => {
  return Boolean(props.page.searchPlaceholder || normalizedTables.value.length || toolbarActions.value.length)
})

const currentTable = computed(() => normalizedTables.value.find((table) => table.key === currentTableKey.value) ?? null)
const editorColumns = computed(() => currentTable.value?.columns ?? [])

watch(
  () => [props.page, props.pageKey, props.platformKey],
  () => {
    initializePageState()
  },
  { immediate: true, deep: true },
)

function initializePageState(): void {
  normalizedTables.value = normalizeTables(props.page)
  searchQuery.value = ''
  activeFilter.value = pageFilters.value[0] ?? ''
  const nextRows: Record<string, ModuleRow[]> = {}
  normalizedTables.value.forEach((table) => {
    nextRows[table.key] = table.rows
  })
  tableRows.value = nextRows
}

function normalizeTables(page: Record<string, any>): ModuleTable[] {
  const rawTables = Array.isArray(page.tables) ? page.tables : []
  return rawTables.map((table, index) => {
    const rawColumns = Array.isArray(table.columns) ? table.columns : []
    const rawRows = Array.isArray(table.rows) ? table.rows : []
    const columns = normalizeColumns(rawColumns, rawRows)
    return {
      key: table.key ? String(table.key) : `table_${index + 1}`,
      title: String(table.title ?? page.title ?? `数据表 ${index + 1}`),
      subtitle: table.subtitle ? String(table.subtitle) : undefined,
      emptyText: table.emptyText ? String(table.emptyText) : undefined,
      columns,
      rows: normalizeRows(rawRows, columns),
      filters: Array.isArray(table.filters) ? table.filters.map((item: unknown) => String(item)) : [],
      readOnly: inferReadOnly(table, page),
      entityName: inferEntityName(String(table.title ?? page.title ?? '记录')),
    }
  })
}

function normalizeColumns(rawColumns: unknown[], rawRows: unknown[]): ModuleColumn[] {
  if (rawColumns.length > 0) {
    return rawColumns.map((column, index) => {
      if (typeof column === 'string') {
        return {
          key: `col_${index + 1}`,
          label: column,
        }
      }
      const record = (column ?? {}) as Record<string, unknown>
      return {
        key: String(record.key ?? `col_${index + 1}`),
        label: String(record.label ?? record.key ?? `字段${index + 1}`),
      }
    })
  }

  const firstRow = rawRows.find((item) => typeof item === 'object' && item !== null && !Array.isArray(item)) as Record<string, unknown> | undefined
  if (!firstRow) {
    return [{ key: 'value', label: '内容' }]
  }

  return Object.keys(firstRow)
    .filter((key) => key !== '__id')
    .map((key) => ({ key, label: key }))
}

function normalizeRows(rawRows: unknown[], columns: ModuleColumn[]): ModuleRow[] {
  return rawRows.map((row, index) => {
    if (Array.isArray(row)) {
      const mapped: ModuleRow = { __id: `row_${index + 1}` }
      columns.forEach((column, columnIndex) => {
        mapped[column.key] = normalizeCellValue(row[columnIndex])
      })
      return mapped
    }

    if (typeof row === 'object' && row !== null) {
      const source = row as Record<string, unknown>
      const normalized: ModuleRow = {
        __id: String(source.id ?? source.code ?? source.name ?? source.title ?? `row_${index + 1}`),
      }
      columns.forEach((column) => {
        normalized[column.key] = normalizeCellValue(source[column.key] ?? source[column.label])
      })
      return normalized
    }

    return {
      __id: `row_${index + 1}`,
      value: normalizeCellValue(row),
    }
  })
}

function normalizeCellValue(value: unknown): CellValue {
  if (value === null || value === undefined) {
    return ''
  }
  if (Array.isArray(value)) {
    return value.join(' / ')
  }
  if (typeof value === 'boolean' || typeof value === 'number' || typeof value === 'string') {
    return value
  }
  return JSON.stringify(value)
}

function inferReadOnly(table: Record<string, any>, page: Record<string, any>): boolean {
  if (typeof table.readOnly === 'boolean') {
    return table.readOnly
  }
  const text = `${page.title ?? ''} ${table.title ?? ''} ${page.kind ?? ''}`
  return /历史|审计|会话|搜索记录/.test(text)
}

function inferEntityName(value: string): string {
  return value.replace(/列表|清单|记录|概览|视图/g, '').trim() || '记录'
}

function normalizeActionLabels(actions: unknown): string[] {
  if (!Array.isArray(actions)) {
    return []
  }
  return actions.map((item) => {
    if (typeof item === 'string') {
      return item
    }
    if (item && typeof item === 'object' && 'label' in item) {
      return String((item as { label: unknown }).label)
    }
    return String(item)
  })
}

function displayRows(table: ModuleTable): ModuleRow[] {
  const rows = tableRows.value[table.key] ?? table.rows
  const keyword = searchQuery.value.trim().toLowerCase()
  const filter = activeFilter.value.trim()

  return rows.filter((row) => {
    if (filter && !/^全部/.test(filter)) {
      const filterMatched = table.columns.some((column) => String(row[column.key] ?? '').includes(filter))
      if (!filterMatched) {
        return false
      }
    }

    if (!keyword) {
      return true
    }

    return table.columns.some((column) => String(row[column.key] ?? '').toLowerCase().includes(keyword))
  })
}

function statusTone(value: CellValue): 'success' | 'warning' | 'danger' | 'info' {
  const text = String(value ?? '')
  if (/成功|完成|启用|已上线|在线|运行|已发布|正常|已授权/.test(text)) {
    return 'success'
  }
  if (/待|处理中|执行中|草稿|审核中|暂停/.test(text)) {
    return 'warning'
  }
  if (/失败|停用|离线|禁用|驳回|删除/.test(text)) {
    return 'danger'
  }
  return 'info'
}

function isStatusValue(column: ModuleColumn, value: CellValue): boolean {
  const text = `${column.label} ${column.key} ${String(value ?? '')}`
  return /状态|结果|优先级|在线|启用|发布|授权/.test(text)
}

function rowActionLabels(table: ModuleTable): string[] {
  return table.readOnly ? ['查看'] : ['查看', '编辑', '删除']
}

function handleToolbarAction(action: string): void {
  const primaryTable = normalizedTables.value.find((table) => !table.readOnly) ?? normalizedTables.value[0]
  if (!primaryTable) {
    emit('action', action)
    return
  }

  if (/新增|新建|创建/.test(action) && !primaryTable.readOnly) {
    openEditor(primaryTable)
    return
  }

  if (/导入|同步/.test(action) && !primaryTable.readOnly) {
    importDemoRow(primaryTable)
    return
  }

  if (/保存|模板/.test(action) && !primaryTable.readOnly) {
    openEditor(primaryTable)
    return
  }

  if (/执行|发布|扫描|装配/.test(action)) {
    if (!primaryTable.readOnly) {
      importDemoRow(primaryTable, action)
      return
    }
    ElMessage.success(`${action} 已触发`)
    return
  }

  emit('action', action)
}

function importDemoRow(table: ModuleTable, actionLabel = '导入资源'): void {
  const nextRow: ModuleRow = { __id: `row_${Date.now()}` }
  table.columns.forEach((column, index) => {
    if (/时间|日期/.test(column.label)) {
      nextRow[column.key] = new Date().toLocaleString('zh-CN', { hour12: false })
    } else if (/状态|结果/.test(column.label)) {
      nextRow[column.key] = /执行|发布|扫描|装配/.test(actionLabel) ? '执行中' : '已导入'
    } else if (/IP/.test(column.label)) {
      nextRow[column.key] = `172.16.${20 + index}.${40 + index}`
    } else {
      nextRow[column.key] = `${table.entityName}${(tableRows.value[table.key] ?? []).length + 1}`
    }
  })
  tableRows.value = {
    ...tableRows.value,
    [table.key]: [nextRow, ...(tableRows.value[table.key] ?? [])],
  }
  void persistPageState(`${actionLabel} 已写入 ${table.title}`)
}

function handleRowAction(table: ModuleTable, action: string, row: ModuleRow): void {
  if (action === '查看') {
    openDrawer(table, row)
    return
  }
  if (action === '编辑') {
    openEditor(table, row)
    return
  }
  if (action === '删除') {
    deleteRow(table, row)
  }
}

function openEditor(table: ModuleTable, row?: ModuleRow): void {
  editorMode.value = row ? 'edit' : 'create'
  currentTableKey.value = table.key
  currentRowId.value = row?.__id ?? ''
  resetFormModel(table, row)
  dialogOpen.value = true
}

function resetFormModel(table: ModuleTable, row?: ModuleRow): void {
  Object.keys(formModel).forEach((key) => delete formModel[key])
  table.columns.forEach((column) => {
    formModel[column.key] = row ? String(row[column.key] ?? '') : ''
  })
}

async function saveEditor(): Promise<void> {
  const table = currentTable.value
  if (!table) {
    return
  }
  const nextRow: ModuleRow = {
    __id: currentRowId.value || `row_${Date.now()}`,
  }
  table.columns.forEach((column) => {
    nextRow[column.key] = formModel[column.key] ?? ''
  })

  const currentRows = [...(tableRows.value[table.key] ?? [])]
  if (editorMode.value === 'edit') {
    const index = currentRows.findIndex((row) => row.__id === currentRowId.value)
    if (index >= 0) {
      currentRows.splice(index, 1, nextRow)
    }
  } else {
    currentRows.unshift(nextRow)
  }
  tableRows.value = {
    ...tableRows.value,
    [table.key]: currentRows,
  }
  dialogOpen.value = false
  await persistPageState(`${editorMode.value === 'edit' ? '更新' : '新增'}${table.entityName}成功`)
}

function deleteRow(table: ModuleTable, row: ModuleRow): void {
  if (!window.confirm(`确认删除 ${table.entityName} 吗？`)) {
    return
  }
  tableRows.value = {
    ...tableRows.value,
    [table.key]: (tableRows.value[table.key] ?? []).filter((item) => item.__id !== row.__id),
  }
  void persistPageState(`${table.entityName}已删除`)
}

function openDrawer(table: ModuleTable, row: ModuleRow): void {
  drawerTitle.value = `${table.title}详情`
  drawerRows.value = table.columns.map((column) => ({
    label: column.label,
    value: String(row[column.key] ?? '--'),
  }))
  drawerOpen.value = true
}

function tableKey(table: ModuleTable): string {
  return `${props.pageKey}-${table.key}`
}

function actionLabel(value: unknown): string {
  return value ? String(value) : '查看详情'
}

function buildTableStates(): Array<{ tableKey: string; rows: Array<Record<string, unknown>> }> {
  return normalizedTables.value.map((table) => ({
    tableKey: table.key,
    rows: (tableRows.value[table.key] ?? []).map((row) => ({ ...row })),
  }))
}

async function persistPageState(successMessage: string): Promise<void> {
  if (!props.platformKey) {
    ElMessage.success(successMessage)
    return
  }

  savingState.value = true
  try {
    await savePlatformPageState({
      platformKey: props.platformKey,
      pageKey: props.pageKey,
      tableStates: buildTableStates(),
    })
    ElMessage.success(successMessage)
  } catch (error) {
    const message = error instanceof Error ? error.message : '保存模块数据失败'
    ElMessage.error(message)
  } finally {
    savingState.value = false
  }
}
</script>

<template>
  <section class="page-stack platform-module-page">
    <article v-if="pageFilters.length" class="panel page-block">
      <div class="chip-row">
        <button
          v-for="item in pageFilters"
          :key="item"
          class="tab-chip"
          :class="{ active: activeFilter === item }"
          @click="activeFilter = item"
        >
          {{ item }}
        </button>
      </div>
    </article>

    <article v-if="showToolbar" class="panel page-block">
      <div class="toolbar">
        <input
          v-if="page.searchPlaceholder || normalizedTables.length"
          v-model="searchQuery"
          class="text-input"
          :placeholder="String(page.searchPlaceholder || `请输入${page.title}关键词`)"
        />
        <div class="toolbar-actions">
          <button
            v-for="action in toolbarActions"
            :key="action"
            class="action-btn"
            :class="{ primary: /新增|新建|创建|执行/.test(action) }"
            @click="handleToolbarAction(action)"
          >
            {{ action }}
          </button>
        </div>
      </div>
    </article>

    <article v-if="page.stats?.length" class="panel page-block">
      <div class="summary-grid platform-summary-grid">
        <div v-for="item in page.stats" :key="item.label" class="summary-panel platform-summary-panel">
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
          <small v-if="item.hint">{{ item.hint }}</small>
        </div>
      </div>
    </article>

    <article v-if="page.cards?.length" class="panel page-block">
      <div class="platform-module-grid">
        <article
          v-for="card in page.cards"
          :key="card.title"
          class="platform-module-card"
          :class="card.accent ? `accent-${card.accent}` : ''"
        >
          <div class="platform-module-card-top">
            <span class="platform-module-card-tag">{{ card.meta || '功能卡片' }}</span>
          </div>
          <h3>{{ card.title }}</h3>
          <p>{{ card.desc }}</p>
          <div class="platform-module-card-actions">
            <button
              class="action-btn primary"
              @click="card.route ? emit('navigate', String(card.route)) : emit('action', actionLabel(card.action || card.title))"
            >
              {{ card.route ? '进入模块' : actionLabel(card.action || card.title) }}
            </button>
          </div>
        </article>
      </div>
    </article>

    <article
      v-for="table in normalizedTables"
      :key="tableKey(table)"
      class="panel page-block"
    >
      <div class="panel-header">
        <div>
          <h3>{{ table.title }}</h3>
          <p v-if="table.subtitle" class="panel-subtitle">{{ table.subtitle }}</p>
        </div>
      </div>
      <div v-if="table.filters.length" class="chip-row compact">
        <button
          v-for="item in table.filters"
          :key="item"
          class="tab-chip"
          :class="{ active: item === table.filters[0] }"
        >
          {{ item }}
        </button>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th v-for="column in table.columns" :key="column.key">{{ column.label }}</th>
              <th v-if="rowActionLabels(table).length">操作</th>
            </tr>
          </thead>
          <tbody v-if="displayRows(table).length">
            <tr v-for="row in displayRows(table)" :key="row.__id">
              <td v-for="column in table.columns" :key="column.key">
                <el-tag
                  v-if="isStatusValue(column, row[column.key])"
                  :type="statusTone(row[column.key])"
                  effect="light"
                  round
                >
                  {{ row[column.key] }}
                </el-tag>
                <span v-else>{{ row[column.key] }}</span>
              </td>
              <td v-if="rowActionLabels(table).length" class="op-cell">
                <button
                  v-for="action in rowActionLabels(table)"
                  :key="`${row.__id}-${action}`"
                  class="mini-link"
                  @click="handleRowAction(table, action, row)"
                >
                  {{ action }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!displayRows(table).length" class="empty-state compact">
          <div class="empty-illustration">0</div>
          <h3>暂无数据</h3>
          <p>{{ table.emptyText || '当前模块还没有数据，你可以直接在前端新增一条记录继续体验。' }}</p>
        </div>
      </div>
    </article>

    <article
      v-if="!pageFilters.length && !toolbarActions.length && !page.searchPlaceholder && !page.stats?.length && !page.cards?.length && !normalizedTables.length"
      class="panel page-block"
    >
      <div class="empty-state">
        <div class="empty-illustration">OpsAny</div>
        <h3>模块准备中</h3>
        <p>当前平台菜单已经切换完成，这里可以继续接入该模块的真实业务能力。</p>
      </div>
    </article>

    <el-dialog
      v-model="dialogOpen"
      width="680px"
      class="config-dialog"
    >
      <template #header>
        <div class="modal-header">
          <div>
            <h3>{{ editorMode === 'edit' ? `编辑${currentTable?.entityName || '记录'}` : `新增${currentTable?.entityName || '记录'}` }}</h3>
            <p>当前改动会保存到共享开发环境，刷新后仍然可继续使用。</p>
          </div>
        </div>
      </template>
      <el-form label-position="top" class="modal-body">
        <div class="form-grid">
          <el-form-item
            v-for="column in editorColumns"
            :key="column.key"
            :label="column.label"
          >
            <el-input v-model="formModel[column.key]" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="order-detail-footer">
          <el-button @click="dialogOpen = false">取消</el-button>
          <el-button type="primary" :loading="savingState" @click="saveEditor">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer
      v-model="drawerOpen"
      size="520px"
      direction="rtl"
      class="order-detail-drawer"
    >
      <template #header>
        <div class="order-detail-header">
          <div>
            <h3>{{ drawerTitle }}</h3>
            <p>模块详情</p>
          </div>
        </div>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item
          v-for="item in drawerRows"
          :key="item.label"
          :label="item.label"
        >
          {{ item.value }}
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </section>
</template>
