<script setup lang="ts">
import { onMounted } from 'vue'
import { useStudentHomeStore } from '@/stores/studentHome'

const home = useStudentHomeStore()

onMounted(() => {
  if (!home.posts.length) home.load()
})
</script>

<template>
  <main class="page">
    <header class="community-head">
      <div>
        <p>Peer Support</p>
        <h1>学习互助</h1>
      </div>
      <button class="primary-button">提问</button>
    </header>

    <section class="community-note panel">
      <strong>先把问题说清楚</strong>
      <p>移动端首版会优先支持浏览、查看回复和发起求助；复杂富文本编辑仍建议在 Web 端完成。</p>
    </section>

    <section class="post-list">
      <article v-for="post in home.posts" :key="post.id" class="post-card panel">
        <h2>{{ post.title }}</h2>
        <p>{{ post.content || '查看同学和老师的讨论回复。' }}</p>
        <div>
          <span>{{ post.authorName || '同学' }}</span>
          <span>{{ post.answerCount ?? 0 }} 回复</span>
          <span>{{ post.viewCount ?? 0 }} 浏览</span>
        </div>
      </article>
      <div v-if="!home.posts.length" class="empty-state panel">暂无互助内容。</div>
    </section>
  </main>
</template>

<style scoped>
.community-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.community-head p {
  margin: 0 0 6px;
  color: var(--green-deep);
  font-size: 12px;
  font-weight: 900;
}

.community-head h1 {
  margin: 0;
  font-size: 32px;
}

.community-note {
  margin: 18px 0;
  padding: 16px;
}

.community-note strong {
  color: var(--gold);
}

.community-note p {
  margin: 8px 0 0;
  color: var(--muted);
  line-height: 1.7;
  font-size: 13px;
}

.post-list {
  display: grid;
  gap: 12px;
}

.post-card {
  padding: 16px;
}

.post-card h2 {
  margin: 0 0 8px;
  font-size: 17px;
}

.post-card p {
  display: -webkit-box;
  margin: 0 0 12px;
  overflow: hidden;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.post-card div {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--green-deep);
  font-size: 12px;
  font-weight: 800;
}
</style>
