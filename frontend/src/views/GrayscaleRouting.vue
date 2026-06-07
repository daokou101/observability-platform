<template>
  <el-card shadow="hover">
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between">
        <span style="font-weight: 600">灰度路由规则</span>
        <div>
          <el-button size="small" @click="sync">同步到 Redis</el-button>
          <el-button type="primary" size="small" @click="openDialog(null)">新增规则</el-button>
        </div>
      </div>
    </template>
    <el-alert title="灰度路由支持三种策略：按用户 ID 哈希、按客户端 IP 范围、按请求头匹配。匹配的请求会被添加 X-Gray-Version 头，由负载均衡器路由到指定版本实例" type="warning" show-icon :closable="false" style="margin-bottom: 16px" />
    <el-table :data="rules" stripe style="width: 100%" v-loading="loading">
      <el-table-column prop="service" label="目标服务" width="160" />
      <el-table-column label="策略" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ strategyLabels[row.strategy] || row.strategy }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ruleValue" label="规则值" min-width="200" />
      <el-table-column prop="targetVersion" label="目标版本" width="120" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled" @change="(v) => toggle(row, v)" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="doDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑规则' : '新增规则'" width="550px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="目标服务">
          <el-input v-model="form.service" placeholder="service-name" />
        </el-form-item>
        <el-form-item label="策略">
          <el-select v-model="form.strategy">
            <el-option label="用户哈希 (user-hash)" value="user-hash" />
            <el-option label="IP 范围 (ip-range)" value="ip-range" />
            <el-option label="请求头 (header)" value="header" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则值">
          <el-input v-model="form.ruleValue" placeholder="user-hash: 0-50 | ip-range: 192.168.1.0/24 | header: X-Canary:true" />
        </el-form-item>
        <el-form-item label="目标版本">
          <el-input v-model="form.targetVersion" placeholder="canary" />
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
import { listGrayscaleRules, saveGrayscaleRule, updateGrayscaleRule, deleteGrayscaleRule, syncGrayscaleRules } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const rules = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ service: '', strategy: 'user-hash', ruleValue: '', targetVersion: 'canary', description: '' })
const strategyLabels = { 'user-hash': '用户哈希', 'ip-range': 'IP 范围', 'header': '请求头' }

async function fetchRules() {
  loading.value = true
  try {
    const res = await listGrayscaleRules()
    rules.value = res.data
  } finally { loading.value = false }
}

function openDialog(row) {
  isEdit.value = !!row
  form.value = row ? { ...row } : { service: '', strategy: 'user-hash', ruleValue: '', targetVersion: 'canary', description: '' }
  dialogVisible.value = true
}

async function save() {
  try {
    if (isEdit.value) {
      await updateGrayscaleRule(form.value.id, form.value)
    } else {
      await saveGrayscaleRule(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchRules()
  } catch (e) { console.error(e) }
}

async function doDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该规则吗？')
    await deleteGrayscaleRule(id)
    ElMessage.success('删除成功')
    fetchRules()
  } catch (e) { if (e !== 'cancel') console.error(e) }
}

async function toggle(row, enabled) {
  try {
    await updateGrayscaleRule(row.id, { ...row, enabled })
    ElMessage.success(enabled ? '已启用' : '已禁用')
  } catch (e) { fetchRules() }
}

async function sync() {
  try {
    await syncGrayscaleRules()
    ElMessage.success('已同步到 Redis')
  } catch (e) { console.error(e) }
}

onMounted(fetchRules)
</script>
