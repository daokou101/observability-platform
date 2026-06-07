<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" style="margin-bottom: 20px">
          <div class="card-content">
            <div class="card-value">{{ card.value }}</div>
            <div class="card-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card shadow="hover" style="margin-bottom: 20px">
          <template #header><span>请求趋势（近7天）</span></template>
          <div ref="trendChart" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" style="margin-bottom: 20px">
          <template #header><span>状态码分布</span></template>
          <div ref="pieChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover">
      <template #header><span>活跃服务</span></template>
      <el-table :data="activeServices" stripe style="width: 100%">
        <el-table-column prop="name" label="服务名" />
        <el-table-column prop="instanceCount" label="实例数" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.instanceCount > 0 ? 'success' : 'danger'">
              {{ row.instanceCount > 0 ? '健康' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getDashboard, getChartData, listServices } from '@/api'
import * as echarts from 'echarts'

const cards = ref([
  { label: '24h 请求总数', value: 0 },
  { label: '24h 错误数', value: 0 },
  { label: '平均耗时 (ms)', value: 0 },
  { label: '活跃服务', value: 0 }
])

const activeServices = ref([])
const trendChart = ref(null)
const pieChart = ref(null)

onMounted(async () => {
  try {
    const [dashRes, chartRes, svcRes] = await Promise.all([
      getDashboard(),
      getChartData(7),
      listServices()
    ])

    const data = dashRes.data
    cards.value[0].value = data.totalRequests24h || 0
    cards.value[1].value = data.errorCount24h || 0
    cards.value[2].value = (data.avgDuration24h || 0).toFixed(1)
    cards.value[3].value = (data.activeServices || []).length
    activeServices.value = svcRes.data.services || []
    cards.value[3].value = svcRes.data.total || 0

    nextTick(() => {
      renderTrendChart(chartRes.data.trend || [])
      renderPieChart(chartRes.data.statusDistribution || [])
    })
  } catch (e) {
    console.error('加载仪表盘数据失败', e)
  }
})

function renderTrendChart(data) {
  if (!trendChart.value) return
  const chart = echarts.init(trendChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.label) },
    yAxis: { type: 'value' },
    series: [{
      type: 'line', smooth: true, data: data.map(d => d.value),
      lineStyle: { color: '#409eff' },
      areaStyle: { color: 'rgba(64,158,255,0.15)' }
    }]
  })
}

function renderPieChart(data) {
  if (!pieChart.value) return
  const colorMap = { '2xx': '#67c23a', '4xx': '#e6a23c', '5xx': '#f56c6c' }
  const chart = echarts.init(pieChart.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: data.map(d => ({
        name: d.category,
        value: d.value,
        itemStyle: { color: colorMap[d.category] || '#909399' }
      })),
      label: { show: true, formatter: '{b}: {c}' }
    }]
  })
}
</script>

<style scoped>
.card-content { text-align: center; padding: 10px 0; }
.card-value { font-size: 32px; font-weight: bold; color: #409eff; }
.card-label { font-size: 14px; color: #909399; margin-top: 8px; }
</style>
