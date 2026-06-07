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

    <el-card shadow="hover">
      <template #header>
        <span>活跃服务</span>
      </template>
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
import { ref, onMounted } from 'vue'
import { getDashboard, listServices } from '@/api'

const cards = ref([
  { label: '24h 请求总数', value: 0 },
  { label: '24h 错误数', value: 0 },
  { label: '平均耗时 (ms)', value: 0 },
  { label: '活跃服务', value: 0 }
])

const activeServices = ref([])

onMounted(async () => {
  try {
    const dashRes = await getDashboard()
    const data = dashRes.data
    cards.value[0].value = data.totalRequests24h || 0
    cards.value[1].value = data.errorCount24h || 0
    cards.value[2].value = (data.avgDuration24h || 0).toFixed(1)
    cards.value[3].value = (data.activeServices || []).length

    const svcRes = await listServices()
    activeServices.value = svcRes.data.services || []
    cards.value[3].value = svcRes.data.total || 0
  } catch (e) {
    console.error('加载仪表盘数据失败', e)
  }
})
</script>

<style scoped>
.card-content { text-align: center; padding: 10px 0; }
.card-value { font-size: 32px; font-weight: bold; color: #409eff; }
.card-label { font-size: 14px; color: #909399; margin-top: 8px; }
</style>
