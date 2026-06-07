<template>
  <el-card shadow="hover">
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between">
        <span style="font-weight: 600">采样率配置</span>
        <div>
          <el-button size="small" @click="sync">同步到 Redis</el-button>
          <el-button type="primary" size="small" @click="openDialog(null)">新增规则</el-button>
        </div>
      </div>
    </template>
    <el-alert title="采样率控制网关日志的采集比例，0=不采集，100=全量采集，可在不重启的情况下动态调整" type="info" show-icon :closable="false" style="margin-bottom: 16px" />
    <el-table :data="rules" stripe style="width: 100%" v-loading="loading">
      <el-table-column prop="service" label="服务名" min-width="200" />
      <el-table-column label="采样率" width="200">
        <template #default="{ row }">
          <el-progress :percentage="row.rate" :color="row.rate >= 80 ? '#67c23a' : row.rate >= 30 ? '#e6a23c' : '#f56c6c'" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="doDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑规则' : '新增规则'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="服务名">
          <el-input v-model="form.service" :disabled="isEdit" placeholder="service-name" />
        </el-form-item>
        <el-form-item label="采样率">
          <el-slider v-model="form.rate" :min="0" :max="100" show-input />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listSamplingRules, saveSamplingRule, deleteSamplingRule, syncSamplingRules } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const rules = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ service: '', rate: 100, description: '' })

async function fetchRules() {
  loading.value = true
  try {
    const res = await listSamplingRules()
    rules.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    isEdit.value = true
    form.value = { ...row }
  } else {
    isEdit.value = false
    form.value = { service: '', rate: 100, description: '' }
  }
  dialogVisible.value = true
}

async function save() {
  try {
    await saveSamplingRule(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchRules()
  } catch (e) {
    console.error(e)
  }
}

async function doDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该规则吗？')
    await deleteSamplingRule(id)
    ElMessage.success('删除成功')
    fetchRules()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function sync() {
  try {
    await syncSamplingRules()
    ElMessage.success('已同步到 Redis')
  } catch (e) {
    console.error(e)
  }
}

onMounted(fetchRules)
</script>
