<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background: #304156">
      <div class="logo">
        <h2>ObsPlatform</h2>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/">
          <el-icon><DataBoard /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/logs">
          <el-icon><Document /></el-icon>
          <span>日志中心</span>
        </el-menu-item>
        <el-menu-item index="/services">
          <el-icon><Connection /></el-icon>
          <span>服务发现</span>
        </el-menu-item>
        <el-menu-item index="/routes">
          <el-icon><Share /></el-icon>
          <span>动态路由</span>
        </el-menu-item>
        <el-menu-item index="/sampling">
          <el-icon><Filter /></el-icon>
          <span>采样配置</span>
        </el-menu-item>
        <el-menu-item index="/flow-rules">
          <el-icon><Warning /></el-icon>
          <span>流控规则</span>
        </el-menu-item>
        <el-menu-item index="/alerts">
          <el-icon><Bell /></el-icon>
          <span>告警管理</span>
        </el-menu-item>
        <el-menu-item index="/grayscale">
          <el-icon><Sort /></el-icon>
          <span>灰度路由</span>
        </el-menu-item>
        <el-menu-item index="/stress-test">
          <el-icon><DataAnalysis /></el-icon>
          <span>压力测试</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: space-between">
        <span style="font-size: 18px; font-weight: 600">微服务可观测性平台</span>
        <el-tag type="info">Gateway + Nacos + Sentinel</el-tag>
      </el-header>
      <el-main style="background: #f0f2f5">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style>
body { margin: 0; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; }
.logo h2 { color: #fff; font-size: 18px; letter-spacing: 2px; }
.el-menu { border-right: none; }
</style>
