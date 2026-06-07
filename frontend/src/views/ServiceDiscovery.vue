<template>
  <el-card shadow="hover">
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between">
        <span style="font-weight: 600">服务注册列表</span>
        <el-tag type="info">共 {{ total }} 个服务</el-tag>
      </div>
    </template>
    <el-table :data="services" stripe style="width: 100%" v-loading="loading">
      <el-table-column type="expand">
        <template #default="{ row }">
          <el-table :data="row.instances" size="small">
            <el-table-column prop="ip" label="IP" width="200" />
            <el-table-column prop="port" label="端口" width="100" />
            <el-table-column label="状态" width="120">
              <template #default="{ row: inst }">
                <el-tag :type="inst.healthy ? 'success' : 'danger'" size="small">
                  {{ inst.healthy ? '健康' : '异常' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="元数据" min-width="200" show-overflow-tooltip>
              <template #default="{ row: inst }">
                <code style="font-size: 12px">{{ JSON.stringify(inst.metadata) }}</code>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="服务名" min-width="250" />
      <el-table-column prop="instanceCount" label="实例数" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.instanceCount > 0 ? 'success' : 'info'">
            {{ row.instanceCount > 0 ? '运行中' : '无实例' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listServices } from '@/api'

const services = ref([])
const total = ref(0)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await listServices()
    services.value = res.data.services || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>
