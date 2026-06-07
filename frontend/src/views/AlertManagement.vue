<template>
  <el-card shadow="hover">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="告警规则" name="rules">
        <template #label>
          <span><el-icon style="vertical-align: middle"><Warning /></el-icon> 告警规则</span>
        </template>
        <div style="margin-bottom: 16px">
          <el-button type="primary" size="small" @click="openRuleDialog(null)">新增规则</el-button>
        </div>
        <el-table :data="rules" stripe v-loading="loading">
          <el-table-column prop="ruleName" label="规则名称" min-width="150" />
          <el-table-column prop="service" label="服务" width="140" />
          <el-table-column label="指标" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ metricLabels[row.metric] || row.metric }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="threshold" label="阈值" width="80" />
          <el-table-column prop="windowMinutes" label="窗口(分钟)" width="100" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-switch :model-value="row.enabled" @change="(v) => toggleRule(row, v)" />
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="180" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openRuleDialog(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="deleteRule(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="告警记录" name="logs">
        <template #label>
          <span><el-icon style="vertical-align: middle"><List /></el-icon> 告警记录</span>
        </template>
        <el-table :data="logs" stripe v-loading="loadingLogs">
          <el-table-column prop="ruleName" label="规则" width="140" />
          <el-table-column prop="service" label="服务" width="120" />
          <el-table-column label="指标" width="100">
            <template #default="{ row }">{{ metricLabels[row.metric] || row.metric }}</template>
          </el-table-column>
          <el-table-column label="当前值" width="100">
            <template #default="{ row }">
              <el-tag type="danger" size="small">{{ row.currentValue?.toFixed(2) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="阈值" width="80">
            <template #default="{ row }">{{ row.threshold }}</template>
          </el-table-column>
          <el-table-column prop="message" label="消息" min-width="300" show-overflow-tooltip />
          <el-table-column prop="createTime" label="时间" width="180" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="ruleDialog" :title="editingRule ? '编辑规则' : '新增规则'" width="550px">
      <el-form :model="ruleForm" label-width="120px">
        <el-form-item label="规则名称">
          <el-input v-model="ruleForm.ruleName" />
        </el-form-item>
        <el-form-item label="目标服务">
          <el-input v-model="ruleForm.service" placeholder="service-name" />
        </el-form-item>
        <el-form-item label="指标">
          <el-select v-model="ruleForm.metric">
            <el-option label="错误数 (error_count)" value="error_count" />
            <el-option label="错误率 (error_rate)" value="error_rate" />
            <el-option label="平均耗时 (avg_duration)" value="avg_duration" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="ruleForm.threshold" :min="1" :max="100000" />
        </el-form-item>
        <el-form-item label="统计窗口(分钟)">
          <el-input-number v-model="ruleForm.windowMinutes" :min="1" :max="60" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="ruleForm.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialog = false">取消</el-button>
        <el-button type="primary" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listAlertRules, saveAlertRule, updateAlertRule, deleteAlertRule, listAlertLogs } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('rules')
const rules = ref([])
const logs = ref([])
const loading = ref(false)
const loadingLogs = ref(false)
const ruleDialog = ref(false)
const editingRule = ref(null)
const ruleForm = ref({ ruleName: '', service: '', metric: 'error_count', threshold: 10, windowMinutes: 5, description: '' })

const metricLabels = { error_count: '错误数', error_rate: '错误率(%)', avg_duration: '平均耗时(ms)' }

async function fetchRules() {
  loading.value = true
  try {
    const res = await listAlertRules()
    rules.value = res.data
  } finally {
    loading.value = false
  }
}

async function fetchLogs() {
  loadingLogs.value = true
  try {
    const res = await listAlertLogs()
    logs.value = res.data
  } finally {
    loadingLogs.value = false
  }
}

function openRuleDialog(row) {
  editingRule.value = row
  ruleForm.value = row ? { ...row } : { ruleName: '', service: '', metric: 'error_count', threshold: 10, windowMinutes: 5, description: '' }
  ruleDialog.value = true
}

async function saveRule() {
  try {
    if (editingRule.value) {
      await updateAlertRule(editingRule.value.id, ruleForm.value)
    } else {
      await saveAlertRule(ruleForm.value)
    }
    ElMessage.success('保存成功')
    ruleDialog.value = false
    fetchRules()
  } catch (e) { console.error(e) }
}

async function deleteRule(id) {
  try {
    await ElMessageBox.confirm('确定删除该规则吗？')
    await deleteAlertRule(id)
    ElMessage.success('删除成功')
    fetchRules()
  } catch (e) { if (e !== 'cancel') console.error(e) }
}

async function toggleRule(row, enabled) {
  try {
    await updateAlertRule(row.id, { ...row, enabled })
    ElMessage.success(enabled ? '已启用' : '已禁用')
  } catch (e) {
    fetchRules()
  }
}

onMounted(() => {
  fetchRules()
  fetchLogs()
})
</script>
