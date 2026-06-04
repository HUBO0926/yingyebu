<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getData, type DashboardMetrics } from '../api';

const metrics = ref<DashboardMetrics>({
  inquiryCount: 0,
  quoteCount: 0,
  pendingInquiryCount: 0,
  sentQuoteCount: 0,
  browseCount: 0,
  aiQuestionCount: 0,
  recentInquiries: [],
  recentQuotes: []
});

const loading = ref(false);

async function refresh() {
  loading.value = true;
  try {
    metrics.value = await getData<DashboardMetrics>('/admin/dashboard');
  } finally {
    loading.value = false;
  }
}

onMounted(refresh);
</script>

<template>
  <section v-loading="loading">
    <div class="metric-grid section">
      <div class="metric">
        <span>客户询价</span>
        <strong>{{ metrics.inquiryCount }}</strong>
      </div>
      <div class="metric">
        <span>报价记录</span>
        <strong>{{ metrics.quoteCount }}</strong>
      </div>
      <div class="metric warn">
        <span>待分配询价</span>
        <strong>{{ metrics.pendingInquiryCount }}</strong>
      </div>
      <div class="metric success">
        <span>已发送报价</span>
        <strong>{{ metrics.sentQuoteCount }}</strong>
      </div>
    </div>

    <el-row :gutter="16" class="section">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>销售报价流程</template>
          <el-steps :active="4" finish-status="success" align-center>
            <el-step title="客户询价" />
            <el-step title="分配销售" />
            <el-step title="配置测项" />
            <el-step title="生成报价" />
            <el-step title="发送/下载" />
          </el-steps>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>运营概览</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="客户浏览">{{ metrics.browseCount }}</el-descriptions-item>
            <el-descriptions-item label="AI 问答">{{ metrics.aiQuestionCount }}</el-descriptions-item>
            <el-descriptions-item label="当前阶段">Mock 闭环联调</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>最近询价</template>
          <el-table :data="metrics.recentInquiries" border height="290" empty-text="暂无询价">
            <el-table-column prop="projectName" label="项目" min-width="160" />
            <el-table-column prop="company" label="客户" min-width="140" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="assignedSalesName" label="销售" width="110" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>最近报价</template>
          <el-table :data="metrics.recentQuotes" border height="290" empty-text="暂无报价">
            <el-table-column prop="quoteNo" label="编号" min-width="150" />
            <el-table-column prop="projectName" label="项目" min-width="160" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="totalAmount" label="金额" width="120" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>
