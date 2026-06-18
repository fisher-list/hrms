<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  downloadSocialInsuranceReport,
  downloadIitReport,
  downloadHousingFundReport,
} from '@/api/statutory-report';

const periodMonth = ref<string>('');
const exporting = ref(false);

function getCurrentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

async function onExportSocialInsurance(): Promise<void> {
  if (!periodMonth.value) { ElMessage.warning('请选择申报月份'); return; }
  exporting.value = true;
  try {
    await downloadSocialInsuranceReport(periodMonth.value);
    ElMessage.success('社保申报表导出成功');
  } catch {
    ElMessage.error('导出失败');
  } finally {
    exporting.value = false;
  }
}

async function onExportIit(): Promise<void> {
  if (!periodMonth.value) { ElMessage.warning('请选择申报月份'); return; }
  exporting.value = true;
  try {
    await downloadIitReport(periodMonth.value);
    ElMessage.success('个税申报表导出成功');
  } catch {
    ElMessage.error('导出失败');
  } finally {
    exporting.value = false;
  }
}

async function onExportHousingFund(): Promise<void> {
  if (!periodMonth.value) { ElMessage.warning('请选择申报月份'); return; }
  exporting.value = true;
  try {
    await downloadHousingFundReport(periodMonth.value);
    ElMessage.success('公积金申报表导出成功');
  } catch {
    ElMessage.error('导出失败');
  } finally {
    exporting.value = false;
  }
}

// 默认当前月份
periodMonth.value = getCurrentMonth();
</script>

<template>
  <div class="page-container">
    <div class="header">
      <h2 class="title">法定报表导出</h2>
    </div>

    <el-card shadow="never">
      <template #header>报表参数</template>
      <el-form label-width="100px" inline>
        <el-form-item label="申报月份">
          <el-date-picker
            v-model="periodMonth"
            type="month"
            value-format="YYYY-MM"
            placeholder="选择月份"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="8">
        <el-card shadow="hover" class="report-card">
          <div class="report-icon">🏥</div>
          <h3>社保申报表</h3>
          <p>包含养老保险、医疗保险、失业保险的个人及单位缴费明细</p>
          <el-button type="primary" :loading="exporting" @click="onExportSocialInsurance">
            导出 Excel
          </el-button>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="report-card">
          <div class="report-icon">💰</div>
          <h3>个税申报表</h3>
          <p>包含应发工资、社保扣除、应纳税所得额、个税金额</p>
          <el-button type="primary" :loading="exporting" @click="onExportIit">
            导出 Excel
          </el-button>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="report-card">
          <div class="report-icon">🏠</div>
          <h3>公积金申报表</h3>
          <p>包含缴存基数、个人缴存、单位缴存明细</p>
          <el-button type="primary" :loading="exporting" @click="onExportHousingFund">
            导出 Excel
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page-container { padding: 24px; }
.header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.title { margin: 0; font-size: 18px; font-weight: 600; }
.report-card { text-align: center; padding: 24px 16px; }
.report-icon { font-size: 48px; margin-bottom: 12px; }
.report-card h3 { margin: 0 0 8px; }
.report-card p { color: #909399; font-size: 13px; margin-bottom: 16px; }
</style>
