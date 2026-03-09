<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { login } from '../lib/api'
import { saveSession } from '../lib/session'

const router = useRouter()
const route = useRoute()

const username = ref('demo')
const password = ref('123456.coM')
const submitting = ref(false)
const errorMessage = ref('')

async function submit() {
  submitting.value = true
  errorMessage.value = ''

  try {
    const response = await login(username.value, password.value)
    saveSession(response.token, response.user)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : response.landingRoute
    await router.replace(redirect || '/')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-hero">
      <div class="login-copy">
        <p class="eyebrow">OpsAny Replica</p>
        <h1>数字化运维平台 1 比 1 复刻</h1>
        <p>
          当前实现基于 <strong>Vue 3 + TypeScript</strong> 与 <strong>Spring Boot</strong>，
          后端接入 MySQL、Redis、RabbitMQ，用于承接工作台、工单、任务和消息链路。
        </p>
        <div class="login-hint">
          <span>演示账号</span>
          <strong>demo / 123456.coM</strong>
        </div>
      </div>

      <form class="login-card" @submit.prevent="submit">
        <div class="login-card-header">
          <h2>欢迎登录数字化运维平台</h2>
          <p>复用原平台的工作台导航与核心页面结构</p>
        </div>

        <label class="form-field">
          <span>用户名</span>
          <input v-model="username" class="text-input" placeholder="请输入用户名" />
        </label>

        <label class="form-field">
          <span>密码</span>
          <input v-model="password" class="text-input" type="password" placeholder="请输入密码" />
        </label>

        <p v-if="errorMessage" class="login-error">{{ errorMessage }}</p>

        <button class="login-submit" :disabled="submitting">
          {{ submitting ? '登录中...' : '立即登录' }}
        </button>
      </form>
    </section>
  </main>
</template>
