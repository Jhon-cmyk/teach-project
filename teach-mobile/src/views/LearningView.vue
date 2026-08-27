<script setup lang="ts">
import { onMounted } from 'vue'
import { useStudentHomeStore } from '@/stores/studentHome'

const home = useStudentHomeStore()

onMounted(() => {
  if (!home.profile) home.load()
})
</script>

<template>
  <main class="page">
    <header class="diagnosis-head">
      <p>Learning Portrait</p>
      <h1>学习诊断</h1>
    </header>

    <section class="score-card panel">
      <div class="score-ring">
        <strong>{{ home.overallScore }}</strong>
        <span>综合分</span>
      </div>
      <div>
        <h2>{{ home.profile?.insight?.title || '画像正在生成' }}</h2>
        <p>{{ home.profile?.insight?.body || '完成更多课程学习、练习和社区互动后，会形成更稳定的学习建议。' }}</p>
      </div>
    </section>

    <section class="section-title">
      <h2>行动计划</h2>
      <span>优先处理</span>
    </section>
    <section class="plan-list">
      <article
        v-for="plan in home.profile?.actionPlans?.slice(0, 4)"
        :key="plan.title"
        class="plan-card panel"
      >
        <div>
          <h3>{{ plan.title }}</h3>
          <p>{{ plan.reason }}</p>
        </div>
        <span>{{ plan.minutes }} min</span>
      </article>
      <div v-if="!home.profile?.actionPlans?.length" class="empty-state panel">
        暂无行动计划，先从今日推荐开始。
      </div>
    </section>

    <section class="section-title">
      <h2>学习建议</h2>
      <span>来自近期行为</span>
    </section>
    <section class="advice-list">
      <article v-for="advice in home.profile?.advices?.slice(0, 4)" :key="advice.title" class="advice-card panel">
        <h3>{{ advice.title }}</h3>
        <p>{{ advice.body }}</p>
      </article>
      <div v-if="!home.profile?.advices?.length" class="empty-state panel">暂无建议。</div>
    </section>
  </main>
</template>

<style scoped>
.diagnosis-head p {
  margin: 0 0 6px;
  color: var(--green-deep);
  font-size: 12px;
  font-weight: 900;
}

.diagnosis-head h1 {
  margin: 0 0 18px;
  font-size: 32px;
}

.score-card {
  display: grid;
  grid-template-columns: 108px 1fr;
  gap: 14px;
  align-items: center;
  padding: 16px;
}

.score-ring {
  display: grid;
  width: 96px;
  height: 96px;
  place-items: center;
  align-content: center;
  border: 8px solid rgba(31, 122, 91, 0.22);
  border-top-color: var(--green);
  border-radius: 50%;
}

.score-ring strong {
  font-size: 30px;
  color: var(--green-deep);
}

.score-ring span {
  color: var(--muted);
  font-size: 11px;
  font-weight: 800;
}

.score-card h2 {
  margin: 0 0 8px;
  font-size: 19px;
}

.score-card p {
  margin: 0;
  color: var(--muted);
  line-height: 1.65;
  font-size: 13px;
}

.plan-list,
.advice-list {
  display: grid;
  gap: 12px;
}

.plan-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
}

.plan-card h3,
.advice-card h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.plan-card p,
.advice-card p {
  margin: 0;
  color: var(--muted);
  line-height: 1.65;
  font-size: 13px;
}

.plan-card span {
  flex: 0 0 auto;
  color: var(--coral);
  font-weight: 900;
}

.advice-card {
  padding: 16px;
}
</style>
