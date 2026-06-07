import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 20000) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export function getDashboard() {
  return request.get('/logs/dashboard')
}

export function getChartData(days) {
  return request.get('/logs/charts', { params: { days } })
}

export function queryLogs(params) {
  return request.get('/logs', { params })
}

export function listServices() {
  return request.get('/services')
}

export function listRoutes(params) {
  return request.get('/routes', { params })
}

export function saveRoute(data) {
  return request.post('/routes', data)
}

export function updateRoute(data) {
  return request.put('/routes', data)
}

export function deleteRoute(id) {
  return request.delete(`/routes/${id}`)
}

export function toggleRoute(id, enabled) {
  return request.put(`/routes/${id}/toggle`, null, { params: { enabled } })
}

export function listSamplingRules() {
  return request.get('/sampling')
}

export function saveSamplingRule(data) {
  return request.post('/sampling', data)
}

export function deleteSamplingRule(id) {
  return request.delete(`/sampling/${id}`)
}

export function syncSamplingRules() {
  return request.post('/sampling/sync')
}

export function listSentinelRules() {
  return request.get('/sentinel/rules')
}

export function saveSentinelRule(data) {
  return request.post('/sentinel/rules', data)
}

export function updateSentinelRule(id, data) {
  return request.put(`/sentinel/rules/${id}`, data)
}

export function deleteSentinelRule(id) {
  return request.delete(`/sentinel/rules/${id}`)
}

export function listAlertRules() {
  return request.get('/alerts/rules')
}

export function saveAlertRule(data) {
  return request.post('/alerts/rules', data)
}

export function updateAlertRule(id, data) {
  return request.put(`/alerts/rules/${id}`, data)
}

export function deleteAlertRule(id) {
  return request.delete(`/alerts/rules/${id}`)
}

export function listAlertLogs(limit) {
  return request.get('/alerts/logs', { params: { limit } })
}

export function listGrayscaleRules() {
  return request.get('/grayscale')
}

export function saveGrayscaleRule(data) {
  return request.post('/grayscale', data)
}

export function updateGrayscaleRule(id, data) {
  return request.put(`/grayscale/${id}`, data)
}

export function deleteGrayscaleRule(id) {
  return request.delete(`/grayscale/${id}`)
}

export function syncGrayscaleRules() {
  return request.post('/grayscale/sync')
}

export default request
