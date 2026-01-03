<template>
  <div class="card">
    <h2>定时任务后端 Demo 测试页</h2>
    <div class="row">
      <label>任务组ID：
        <input v-model.number="groupId" type="number" min="1" />
      </label>
      <button @click="loadConfig">加载配置</button>
      <button @click="evaluate">评估条件</button>
      <button @click="run" :disabled="!overallPass">执行任务</button>
    </div>

    <section v-if="cfg">
      <h3>任务组</h3>
      <p><strong>{{ cfg.taskGroup.name }}</strong> (ID: {{ cfg.taskGroup.id }}) | cron: {{ cfg.taskGroup.cron }} | misfire: {{ cfg.taskGroup.misfirePolicy }}</p>
      <p>描述：{{ cfg.taskGroup.descText || '无' }}</p>
      <p>时间规则：{{ cfg.taskGroup.timeRule.businessTime?.join(', ') }}；周{{ cfg.taskGroup.timeRule.weekdays?.join(',') }}；单双周：{{ cfg.taskGroup.timeRule.weekParity }}；日期：{{ cfg.taskGroup.timeRule.dateRange?.join(' ~ ') }}</p>
    </section>

    <section v-if="cfg">
      <h3>数据组</h3>
      <table>
        <thead><tr><th>ID</th><th>名称</th><th>类型</th><th>Mock值</th><th>启用</th></tr></thead>
        <tbody>
          <tr v-for="d in cfg.dataGroups" :key="d.id">
            <td>{{ d.id }}</td>
            <td>{{ d.name }}</td>
            <td>{{ d.type }}</td>
            <td>{{ d.mockValue }}</td>
            <td>{{ d.enable }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section v-if="evalRes">
      <h3>条件组 ({{ cfg.conditionGroup.logic }})</h3>
      <ul>
        <li v-for="c in evalRes.details" :key="c.id">
          条件 #{{ c.id }} - {{ c.desc }}: <strong :class="c.result ? 'ok' : 'fail'">{{ c.result }}</strong>
        </li>
      </ul>
      <p>汇总：<strong :class="overallPass ? 'ok' : 'fail'">{{ overallPass ? '通过' : '未通过' }}</strong></p>
    </section>

    <section v-if="runRes">
      <h3>执行结果</h3>
      <p>通过：<strong :class="runRes.passed ? 'ok' : 'fail'">{{ runRes.passed }}</strong></p>
      <ul>
        <li v-for="(l,i) in runRes.actionLogs" :key="i">{{ l }}</li>
      </ul>
    </section>

    <section v-if="cfg">
      <h3>动作</h3>
      <table>
        <thead>
        <tr><th>ID</th><th>名称</th><th>类型</th><th>顺序</th><th>重试</th><th>退避(ms)</th><th>超时(ms)</th><th>并行标签</th><th>启用</th></tr>
        </thead>
        <tbody>
        <tr v-for="a in cfg.actions" :key="a.id">
          <td>{{ a.id }}</td>
          <td>{{ a.name }}</td>
          <td>{{ a.type }}</td>
          <td>{{ a.order }}</td>
          <td>{{ a.retryTimes }}</td>
          <td>{{ a.retryBackoffMs }}</td>
          <td>{{ a.timeoutMs }}</td>
          <td>{{ a.parallelTag }}</td>
          <td>{{ a.enable }}</td>
        </tr>
        </tbody>
      </table>
    </section>

    <section v-if="cfg && cfg.alerts">
      <h3>告警</h3>
      <p>启用：{{ cfg.alerts.strategy?.enable }}；节流(ms)：{{ cfg.alerts.strategy?.throttleMs }}；连续失败阈值：{{ cfg.alerts.strategy?.continuousFailThreshold }}；绑定：{{ cfg.alerts.strategy?.bindChannels?.join(',') }}</p>
      <ul>
        <li v-for="ch in cfg.alerts.channels" :key="ch.id">
          #{{ ch.id }} {{ ch.type }} | {{ ch.config }}
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup>
import axios from 'axios'
import { ref, computed } from 'vue'

const cfg = ref(null)
const evalRes = ref(null)
const runRes = ref(null)
const groupId = ref(1)

const overallPass = computed(() => evalRes.value?.passed)

async function loadConfig() {
  const { data } = await axios.get('/api/demo/config', { params: { groupId: groupId.value } })
  cfg.value = data
}

async function evaluate(groupId = 1) {
  if (!cfg.value) await loadConfig()
  const { data } = await axios.post('/api/demo/evaluate', null, { params: { groupId: groupId.value } })
  evalRes.value = data
}

async function run(groupId = 1) {
  const { data } = await axios.post('/api/demo/run', null, { params: { groupId: groupId.value } })
  runRes.value = data
}
</script>

<style scoped>
.card { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.row { display: flex; gap: 8px; margin-bottom: 12px; }
button { padding: 6px 12px; cursor: pointer; }
table { width: 100%; border-collapse: collapse; }
th, td { border: 1px solid #eee; padding: 6px; text-align: left; }
.ok { color: #16a34a; }
.fail { color: #dc2626; }
section { margin-top: 12px; }
</style>

