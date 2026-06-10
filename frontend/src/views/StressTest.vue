<template>
  <div>
    <el-card shadow="hover" style="margin-bottom: 20px">
      <template #header><span>压力测试配置</span></template>
      <el-form :inline="true" :model="form" label-width="80px">
        <el-form-item label="目标 URL">
          <el-input v-model="form.url" style="width: 400px" placeholder="http://gateway:8080/api/demo/hello" />
        </el-form-item>
        <el-form-item label="请求方法">
          <el-select v-model="form.method" style="width: 100px">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标 QPS">
          <el-input-number v-model="form.qps" :min="1" :max="5000" />
        </el-form-item>
        <el-form-item label="持续时间">
          <el-select v-model="form.durationSeconds" style="width: 120px">
            <el-option :value="10" label="10秒" />
            <el-option :value="30" label="30秒" />
            <el-option :value="60" label="1分钟" />
            <el-option :value="120" label="2分钟" />
            <el-option :value="300" label="5分钟" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button v-if="!running" type="danger" @click="start" :loading="starting">开始压测</el-button>
          <el-button v-else type="warning" @click="stop">停止</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6" v-for="s in summaryCards" :key="s.label">
        <el-card shadow="hover">
          <div style="text-align:center;padding:8px 0">
            <div style="font-size:28px;font-weight:bold;color:#409eff">{{ s.value }}</div>
            <div style="font-size:13px;color:#909399;margin-top:6px">{{ s.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>QPS 实时曲线</span></template>
          <div id="stressQpsChart" style="height: 260px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>P50 / P95 / P99 延迟 (ms)</span></template>
          <div id="stressLatencyChart" style="height: 260px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover">
      <template #header><span>压测历史</span></template>
      <el-table :data="history" stripe v-loading="loadingHistory" style="width:100%">
        <el-table-column prop="url" label="URL" min-width="350" show-overflow-tooltip />
        <el-table-column prop="targetQps" label="目标QPS" width="90" />
        <el-table-column prop="totalRequests" label="总请求" width="90" />
        <el-table-column prop="errorCount" label="错误数" width="80" />
        <el-table-column prop="errorRate" label="错误率" width="80" />
        <el-table-column prop="timestamp" label="时间" width="180">
          <template #default="{ row }">{{ new Date(row.timestamp).toLocaleString() }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const STORAGE_KEY = 'stress_task_id'

const form = reactive({
  url: 'http://gateway:8080/api/demo/hello',
  method: 'GET',
  qps: 10,
  durationSeconds: 30
})

const running = ref(false)
const starting = ref(false)
const loadingHistory = ref(false)
const history = ref([])

const latestStats = reactive({
  secondRequests: 0, secondErrors: 0, currentQps: 0,
  avgDuration: '0.0', p50: '0.0', p95: '0.0', p99: '0.0',
  totalRequests: 0, totalSuccess: 0, totalErrors: 0
})

let qpsChart = null
let latencyChart = null
let eventSource = null

const qpsData = []
const p50Data = []
const p95Data = []
const p99Data = []
const timeLabels = []

const summaryCards = computed(() => [
  { label: '当前 QPS', value: latestStats.currentQps },
  { label: '总请求数', value: latestStats.totalRequests },
  { label: '错误数', value: latestStats.totalErrors },
  { label: 'P99 延迟(ms)', value: latestStats.p99 }
])

function initCharts() {
  const qpsEl = document.getElementById('stressQpsChart')
  const latEl = document.getElementById('stressLatencyChart')
  if (!qpsEl || !latEl) return false

  qpsChart = echarts.init(qpsEl)
  latencyChart = echarts.init(latEl)

  // 初始化空图表
  qpsChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, bottom: 30 },
    xAxis: { type: 'category', data: [] },
    yAxis: { type: 'value', min: 0 },
    series: [{ type: 'line', smooth: true, data: [], lineStyle: { color: '#409eff' }, areaStyle: { color: 'rgba(64,158,255,0.15)' } }]
  }, true)
  latencyChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['P50', 'P95', 'P99'], bottom: 0 },
    grid: { left: 50, right: 20, bottom: 40 },
    xAxis: { type: 'category', data: [] },
    yAxis: { type: 'value', name: 'ms' },
    series: [
      { name: 'P50', type: 'line', smooth: true, data: [], lineStyle: { color: '#67c23a' } },
      { name: 'P95', type: 'line', smooth: true, data: [], lineStyle: { color: '#e6a23c' } },
      { name: 'P99', type: 'line', smooth: true, data: [], lineStyle: { color: '#f56c6c' } }
    ]
  }, true)

  // 浏览器窗口变化时自适应
  window.addEventListener('resize', () => { qpsChart?.resize(); latencyChart?.resize() })
  return true
}

function updateCharts() {
  if (!qpsChart || !latencyChart) return
  qpsChart.setOption({
    xAxis: { data: [...timeLabels] },
    series: [{ data: [...qpsData] }]
  })
  latencyChart.setOption({
    xAxis: { data: [...timeLabels] },
    series: [
      { data: [...p50Data] },
      { data: [...p95Data] },
      { data: [...p99Data] }
    ]
  })
}

function addData(qps, p50, p95, p99) {
  qpsData.push(qps)
  p50Data.push(p50)
  p95Data.push(p95)
  p99Data.push(p99)
  timeLabels.push(timeLabels.length + 's')
  if (timeLabels.length > 60) {
    qpsData.shift(); p50Data.shift(); p95Data.shift(); p99Data.shift(); timeLabels.shift()
  }
}

function clearData() {
  qpsData.length = 0; p50Data.length = 0; p95Data.length = 0; p99Data.length = 0; timeLabels.length = 0
}

function onTestEnd() {
  running.value = false
  sessionStorage.removeItem(STORAGE_KEY)
  if (eventSource) { eventSource.close(); eventSource = null }
  fetchHistory()
}

function subscribeSse(id) {
  if (eventSource) eventSource.close()
  eventSource = new EventSource(`/api/stress/subscribe/${id}`)
  eventSource.onmessage = (event) => {
    const data = JSON.parse(event.data)
    if (data.finished) { onTestEnd(); return }
    Object.assign(latestStats, data)
    addData(data.currentQps || 0, parseFloat(data.p50 || 0), parseFloat(data.p95 || 0), parseFloat(data.p99 || 0))
    updateCharts()
    if (!data.running) onTestEnd()
  }
  eventSource.onerror = () => {
    const savedId = sessionStorage.getItem(STORAGE_KEY)
    if (savedId && savedId !== id) return
    if (savedId && eventSource) {
      eventSource.close()
      eventSource = null
      subscribeSse(savedId)
    } else {
      onTestEnd()
    }
  }
}

async function start() {
  if (!form.url) { ElMessage.warning('请输入目标 URL'); return }
  starting.value = true
  try {
    const res = await fetch('/api/stress/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...form })
    }).then(r => r.json())
    if (res.code !== 20000) { ElMessage.error(res.message || '启动失败'); return }
    taskId.value = res.data.taskId
    running.value = true
    starting.value = false
    sessionStorage.setItem(STORAGE_KEY, res.data.taskId)
    clearData()
    ElMessage.success('压测已启动')
    subscribeSse(res.data.taskId)
  } catch (e) {
    starting.value = false
    ElMessage.error('启动失败: ' + e.message)
  }
}

const taskId = ref('')

async function stop() {
  try {
    await fetch(`/api/stress/stop/${taskId.value}`, { method: 'POST' })
    ElMessage.success('压测已停止')
  } catch (e) { console.error(e) }
}

async function fetchHistory() {
  loadingHistory.value = true
  try {
    const res = await fetch('/api/stress/history').then(r => r.json())
    if (res.code === 20000) history.value = res.data.records || []
  } catch (e) { console.error(e) }
  finally { loadingHistory.value = false }
}

onMounted(() => {
  fetchHistory()

  // 图表 DOM 始终存在，直接初始化
  initCharts()

  // 恢复正在运行的任务
  const savedId = sessionStorage.getItem(STORAGE_KEY)
  if (savedId) {
    taskId.value = savedId
    running.value = true
    subscribeSse(savedId)
  }
})

onUnmounted(() => {
  if (eventSource) { eventSource.close(); eventSource = null }
  if (qpsChart) { qpsChart.dispose(); qpsChart = null }
  if (latencyChart) { latencyChart.dispose(); latencyChart = null }
})
</script>
