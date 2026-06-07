<template>
  <el-card shadow="hover">
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between">
        <span style="font-weight: 600">动态路由管理</span>
        <el-button type="primary" size="small" @click="openDialog(null)">新增路由</el-button>
      </div>
    </template>
    <el-table :data="routes" stripe style="width: 100%" v-loading="loading">
      <el-table-column prop="routeId" label="路由 ID" min-width="180" />
      <el-table-column prop="uri" label="目标 URI" min-width="250" />
      <el-table-column prop="order" label="优先级" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled" @change="(v) => toggle(row.id, v)" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="doDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑路由' : '新增路由'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="路由 ID">
          <el-input v-model="form.routeId" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="目标 URI">
          <el-input v-model="form.uri" placeholder="lb://service-name 或 http://..." />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.order" :min="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
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
import { listRoutes, saveRoute, updateRoute, deleteRoute, toggleRoute } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const routes = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ routeId: '', uri: '', order: 0, description: '' })

async function fetchRoutes() {
  loading.value = true
  try {
    const res = await listRoutes({ page: 1, pageSize: 100 })
    routes.value = res.data
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
    form.value = { routeId: '', uri: '', order: 0, description: '' }
  }
  dialogVisible.value = true
}

async function save() {
  try {
    if (isEdit.value) {
      await updateRoute(form.value)
    } else {
      await saveRoute(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchRoutes()
  } catch (e) {
    console.error(e)
  }
}

async function doDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该路由吗？')
    await deleteRoute(id)
    ElMessage.success('删除成功')
    fetchRoutes()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function toggle(id, enabled) {
  try {
    await toggleRoute(id, enabled)
    ElMessage.success(enabled ? '已启用' : '已禁用')
  } catch (e) {
    console.error(e)
    fetchRoutes()
  }
}

onMounted(fetchRoutes)
</script>
