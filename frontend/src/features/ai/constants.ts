export interface AiInsightTemplate {
  label: string
  value: string
  description: string
  createSample: () => Record<string, unknown>
}

export const AI_MODULE_TABS = [
  { label: '对话助手', to: '/ai/assistant' },
  { label: '智能洞察', to: '/ai/insights' },
  { label: '知识库', to: '/ai/knowledge' },
] as const

export const AI_ASSISTANT_SUGGESTIONS = [
  '帮我总结最近 30 分钟的核心告警并给出排查顺序',
  '根据这段错误日志，判断最可能的根因和下一步验证动作',
  '把这张工单整理成标准化处理方案和回滚建议',
  '结合配置变更内容，评估可能影响到的系统和风险点',
] as const

export const AI_INSIGHT_TEMPLATES: AiInsightTemplate[] = [
  {
    label: '告警摘要',
    value: 'alarm_summary',
    description: '把同一故障下的多条告警汇总成面向运维的故障摘要。',
    createSample: () => ({
      alerts: [
        { name: 'mysql connection high', level: 'P1', value: 92, time: '2026-03-12 10:00:00' },
        { name: 'api latency p99', level: 'P1', value: 3800, time: '2026-03-12 10:02:00' },
      ],
      scope: 'production',
      impact: 'order service timeout',
    }),
  },
  {
    label: '根因分析',
    value: 'root_cause',
    description: '结合指标、日志与变更信息，输出可验证的根因候选。',
    createSample: () => ({
      metric: 'api p99 latency',
      trend: 'up',
      recentChanges: ['release-2026.03.12', 'nginx config updated'],
      logs: ['timeout from db', 'redis response slow'],
    }),
  },
  {
    label: '日志聚类',
    value: 'log_cluster',
    description: '对日志样本做模式归类，识别共性事件与异常模式。',
    createSample: () => ({
      logs: [
        'ERROR connect timeout to redis',
        'WARN redis response slow',
        'ERROR connect timeout to mysql',
      ],
    }),
  },
  {
    label: '容量规划',
    value: 'capacity_plan',
    description: '结合历史趋势和业务背景，给出扩容与优化建议。',
    createSample: () => ({
      resource: 'k8s cluster cpu',
      last30d: [62, 64, 66, 70, 74],
      holiday: false,
      businessGrowth: 'promotion campaign next week',
    }),
  },
  {
    label: '变更影响',
    value: 'change_impact',
    description: '对配置项和变更内容进行影响范围和风险分析。',
    createSample: () => ({
      ci: 'prod-nginx-01',
      change: 'upgrade nginx 1.26',
      dependencies: ['gateway', 'order-api'],
      changeWindow: '2026-03-12 23:00:00',
    }),
  },
  {
    label: '工单派单',
    value: 'ticket_dispatch',
    description: '给出工单类型、优先级和推荐处理队列。',
    createSample: () => ({
      title: '生产环境订单接口超时',
      description: '用户反馈下单失败，接口响应超过 10 秒。',
      priority: 'high',
      service: 'order-api',
    }),
  },
  {
    label: '工单方案',
    value: 'ticket_solution',
    description: '结合上下文推荐标准化处置步骤与验证动作。',
    createSample: () => ({
      title: '生产环境订单接口超时',
      description: '最近 15 分钟大量超时，数据库连接池耗尽。',
      priority: 'high',
      service: 'order-api',
    }),
  },
  {
    label: '配置标准化',
    value: 'config_normalize',
    description: '对配置项属性做标准化、补全和异常提示。',
    createSample: () => ({
      hostname: 'Prod-Redis-01',
      cpu: 'Cpu',
      owner: 'ops',
      ip: '10.0.0.8',
      env: 'prod',
    }),
  },
]
