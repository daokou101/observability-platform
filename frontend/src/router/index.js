import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import LogCenter from '../views/LogCenter.vue'
import ServiceDiscovery from '../views/ServiceDiscovery.vue'
import RouteManagement from '../views/RouteManagement.vue'
import SamplingConfig from '../views/SamplingConfig.vue'
import FlowRules from '../views/FlowRules.vue'
import AlertManagement from '../views/AlertManagement.vue'
import GrayscaleRouting from '../views/GrayscaleRouting.vue'

const routes = [
  { path: '/', name: 'Dashboard', component: Dashboard },
  { path: '/logs', name: 'LogCenter', component: LogCenter },
  { path: '/services', name: 'ServiceDiscovery', component: ServiceDiscovery },
  { path: '/routes', name: 'RouteManagement', component: RouteManagement },
  { path: '/sampling', name: 'SamplingConfig', component: SamplingConfig },
  { path: '/flow-rules', name: 'FlowRules', component: FlowRules },
  { path: '/alerts', name: 'AlertManagement', component: AlertManagement },
  { path: '/grayscale', name: 'GrayscaleRouting', component: GrayscaleRouting }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
