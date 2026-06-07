<template>
  <el-card shadow="hover">
    <template #header>
      <div style="display: flex; align-items: center; gap: 16px; flex-wrap: wrap">
        <span style="font-weight: 600">网关日志</span>
        <el-input v-model="filter.traceId" placeholder="Trace ID" clearable style="width: 200px" size="small" />
        <el-input v-model="filter.service" placeholder="服务名" clearable style="width: 150px" size="small" />
        <el-select v-model="filter.statusCode" placeholder="状态码" clearable style="width: 120px" size="small">
          <el-option label="2xx" value="2" />
          <el-option label="4xx" value="4" />
          <el-option label="5xx" value="5" />
        </el-select>
        <el-button type="primary" size="small" @click="search">查询</el-button>
        <el-button size="small" @click="reset">重置</el-button>
      </div>
    </template>
    <el-table :data="logs" stripe style="width: 100%" v-loading="loading">
      <el-table-column prop="traceId" label="Trace ID" width="200" show-overflow-tooltip />
      <el-table-column prop="service" label="服务" width="140" />
      <el-table-column prop="path" label="路径" min-width="200" show-overflow-tooltip />
      <el-table-column prop="method" label="方法" width="80" />
      <el-table-column prop="statusCode" label="状态码" width="80">
        <template #default="{ row }">
          <el-tag :type="row.statusCode >= 500 ? 'danger' : row.statusCode >= 400 ? 'warning' : 'success'" size="small">
            {{ row.statusCode }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="duration" label="耗时(ms)" width="100">
        <template #default="{ row }">
          <span :style="{ color: row.duration > 1000 ? '#f56c6c' : '#67c23a' }">{{ row.duration }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="requestTime" label="请求时间" width="180" />
    </el-table>
    <div style="display: flex; justify-content: center; margin-top: 20px">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchLogs"
      />
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { queryLogs } from '@/api'

const logs = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const filter = ref({ traceId: '', service: '', statusCode: '' })

async function fetchLogs() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (filter.value.traceId) params.traceId = filter.value.traceId
    if (filter.value.service) params.service = filter.value.service
    if (filter.value.statusCode) params.statusCode = filter.value.statusCode
    const res = await queryLogs(params)
    logs.value = res.data
    total.value = res.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function search() { page.value = 1; fetchLogs() }
function reset() { filter.value = { traceId: '', service: '', statusCode: '' }; search() }

onMounted(fetchLogs)
</script>
