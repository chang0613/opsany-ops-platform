<script setup lang="ts">
import { computed } from 'vue'

import type { NavigationGroup, NavigationItem } from '../types/platform'

const props = defineProps<{
  activeLink: string
  groups: NavigationGroup[]
  favorites: NavigationItem[]
}>()

const emit = defineEmits<{
  close: []
  select: [item: NavigationItem]
}>()

const favoriteCodes = computed(() => new Set(props.favorites.map((item) => item.itemCode)))

const displayGroups = computed(() =>
  props.groups
    .map((group) => ({
      ...group,
      rows: group.rows.filter((item) => !favoriteCodes.value.has(item.itemCode)),
    }))
    .filter((group) => group.rows.length > 0),
)

function normalizeLink(link: string | undefined): string {
  if (!link) {
    return ''
  }
  const normalized = link.replace(/#/g, '/').replace(/\/+/g, '/').replace(/\/$/, '')
  return normalized || '/'
}

function isActive(item: NavigationItem): boolean {
  return normalizeLink(props.activeLink) === normalizeLink(item.link)
}

function handleSelect(item: NavigationItem): void {
  emit('select', item)
}
</script>

<template>
  <div class="platform-nav-mask" @click.self="emit('close')">
    <section class="platform-nav-panel">
      <header class="platform-nav-header compact">
        <div class="platform-nav-title-wrap">
          <div class="platform-nav-caption">平台导航</div>
          <h3>我的收藏</h3>
        </div>
      </header>

      <div v-if="favorites.length" class="platform-nav-favorites">
        <button
          v-for="item in favorites"
          :key="`favorite-${item.itemCode}`"
          class="platform-nav-link favorite"
          :class="{ active: isActive(item) }"
          @click="handleSelect(item)"
        >
          <span class="platform-nav-link-icon">{{ item.icon }}</span>
          <span class="platform-nav-link-body">
            <strong>{{ item.name }}</strong>
            <small>{{ item.desc }}</small>
          </span>
        </button>
      </div>

      <div class="platform-nav-sections">
        <section v-for="group in displayGroups" :key="group.title" class="platform-nav-section">
          <div class="platform-nav-section-title">{{ group.title }}</div>
          <div class="platform-nav-links">
            <button
              v-for="item in group.rows"
              :key="item.itemCode"
              class="platform-nav-link"
              :class="{ active: isActive(item) }"
              @click="handleSelect(item)"
            >
              <span class="platform-nav-link-icon">{{ item.icon }}</span>
              <span class="platform-nav-link-body">
                <strong>{{ item.name }}</strong>
                <small>{{ item.desc }}</small>
              </span>
            </button>
          </div>
        </section>
      </div>
    </section>
  </div>
</template>
