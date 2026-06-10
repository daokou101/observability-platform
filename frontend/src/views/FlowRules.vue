<template>
  <div>
    <el-card shadow="hover">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span style="font-weight: 600">Sentinel 流控规则</span>
          <el-button type="primary" size="small" @click="openDialog(null)">新增规则</el-button>
        </div>
      </template>
      <el-alert title="流控规则基于 Sentinel，支持 QPS/线程数限流。规则存储在 MySQL 并同步至 Sentinel 内存，生产环境可通过 Nacos 配置中心实现动态下发" type="info" show-icon :closable="false" style="margin-bottom: 16px" />
      <el-table :data="rules" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="resource" label="资源名" min-width="180" />
        <el-table-column label="阈值类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.grade === 1 ? 'primary' : 'warning'" size="small">
              {{ row.grade === 1 ? 'QPS' : '线程数' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="count" label="阈值" width="80" />
        <el-table-column label="流控模式" width="120">
          <template #default="{ row }">
            {{ { 0: '直接', 1: '关联', 2: '链路' }[row.strategy] || '直接' }}
          </template>
        </el-table-column>
        <el-table-column label="流控效果" width="120">
          <template #default="{ row }">
            {{ { 0: '快速失败', 1: 'Warm Up', 2: '排队等待' }[row.controlBehavior] || '快速失败' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small" style="cursor:pointer" @click="toggle(row, !row.enabled)">
              {{ row.enabled ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="150" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="doDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑规则' : '新增规则'" width="600px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="资源名">
          <el-input v-model="form.resource" placeholder="/api/xxx" />
        </el-form-item>
        <el-form-item label="阈值类型">
          <el-radio-group v-model="form.grade">
            <el-radio :value="1">QPS</el-radio>
            <el-radio :value="0">线程数</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="form.count" :min="1" :max="100000" />
        </el-form-item>
        <el-form-item label="流控模式">
          <el-select v-model="form.strategy">
            <el-option :value="0" label="直接" />
            <el-option :value="1" label="关联" />
            <el-option :value="2" label="链路" />
          </el-select>
        </el-form-item>
        <el-form-item label="流控效果">
          <el-select v-model="form.controlBehavior">
            <el-option :value="0" label="快速失败" />
            <el-option :value="1" label="Warm Up" />
            <el-option :value="2" label="排队等待" />
          </el-select>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listSentinelRules, saveSentinelRule, updateSentinelRule, deleteSentinelRule } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const rules = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  resource: '', grade: 1, count: 10,
  strategy: 0, controlBehavior: 0, description: ''
})

async function fetchRules() {
  loading.value = true
  try {
    const res = await listSentinelRules()
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
    form.value = { resource: '', grade: 1, count: 10, strategy: 0, controlBehavior: 0, description: '' }
  }
  dialogVisible.value = true
}

async function save() {
  try {
    if (isEdit.value) {
      await updateSentinelRule(form.value.id, form.value)
    } else {
      await saveSentinelRule(form.value)
    }
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
    await deleteSentinelRule(id)
    ElMessage.success('删除成功')
    fetchRules()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function toggle(row, enabled) {
  row.enabled = enabled
  try {
    await updateSentinelRule(row.id, { id: row.id, enabled })
    ElMessage.success(enabled ? '已启用' : '已禁用')
  } catch (e) {
    console.error(e)
    fetchRules()
  }
}

onMounted(fetchRules)
</script>
