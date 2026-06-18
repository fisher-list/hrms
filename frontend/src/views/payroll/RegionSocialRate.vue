<script setup lang="ts">
/**
 * 多地区社保政策管理页面。
 * 支持按城市配置不同的社保基数/比例。
 */
import { ref, reactive, onMounted, nextTick } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  listRegionSocialRates,
  createRegionSocialRate,
  updateRegionSocialRate,
  disableRegionSocialRate,
} from '@/api/payroll';

interface RegionRate {
  id: number;
  city: string;
  pensionPersonal: number;
  pensionCompany: number;
  medicalPersonal: number;
  medicalFixedFee: number;
  medicalCompany: number;
  unemploymentPersonal: number;
  unemploymentCompany: number;
  injuryCompany?: number;
  maternityCompany?: number;
  housingFundPersonal: number;
  housingFundCompany: number;
  socialBaseFloor?: number;
  socialBaseCeil?: number;
  fundBaseFloor?: number;
  fundBaseCeil?: number;
  enabled: boolean;
}

const list = ref<RegionRate[]>([]);
const loading = ref(false);

// ---- 新建/编辑表单 ----
const formRef = ref<FormInstance>();
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);

const form = reactive({
  city: '',
  pensionPersonal: 0.08,
  pensionCompany: 0.16,
  medicalPersonal: 0.02,
  medicalFixedFee: 3,
  medicalCompany: 0.098,
  unemploymentPersonal: 0.005,
  unemploymentCompany: 0.005,
  injuryCompany: 0.004,
  maternityCompany: 0.008,
  housingFundPersonal: 0.12,
  housingFundCompany: 0.12,
  socialBaseFloor: undefined as number | undefined,
  socialBaseCeil: undefined as number | undefined,
  fundBaseFloor: undefined as number | undefined,
  fundBaseCeil: undefined as number | undefined,
});

const rules: FormRules = {
  city: [{ required: true, message: '请输入城市名称', trigger: 'blur' }],
};

async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    list.value = await listRegionSocialRates();
  } catch {
    ElMessage.error('加载地区社保政策失败');
  } finally {
    loading.value = false;
  }
}

function openCreate(): void {
  editingId.value = null;
  Object.assign(form, {
    city: '',
    pensionPersonal: 0.08,
    pensionCompany: 0.16,
    medicalPersonal: 0.02,
    medicalFixedFee: 3,
    medicalCompany: 0.098,
    unemploymentPersonal: 0.005,
    unemploymentCompany: 0.005,
    injuryCompany: 0.004,
    maternityCompany: 0.008,
    housingFundPersonal: 0.12,
    housingFundCompany: 0.12,
    socialBaseFloor: undefined,
    socialBaseCeil: undefined,
    fundBaseFloor: undefined,
    fundBaseCeil: undefined,
  });
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

function openEdit(row: RegionRate): void {
  editingId.value = row.id;
  Object.assign(form, {
    city: row.city,
    pensionPersonal: row.pensionPersonal,
    pensionCompany: row.pensionCompany,
    medicalPersonal: row.medicalPersonal,
    medicalFixedFee: row.medicalFixedFee,
    medicalCompany: row.medicalCompany,
    unemploymentPersonal: row.unemploymentPersonal,
    unemploymentCompany: row.unemploymentCompany,
    injuryCompany: row.injuryCompany,
    maternityCompany: row.maternityCompany,
    housingFundPersonal: row.housingFundPersonal,
    housingFundCompany: row.housingFundCompany,
    socialBaseFloor: row.socialBaseFloor,
    socialBaseCeil: row.socialBaseCeil,
    fundBaseFloor: row.fundBaseFloor,
    fundBaseCeil: row.fundBaseCeil,
  });
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

async function onSubmit(): Promise<void> {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  try {
    if (editingId.value) {
      await updateRegionSocialRate(editingId.value, { ...form });
      ElMessage.success('社保政策已更新');
    } else {
      await createRegionSocialRate({ ...form });
      ElMessage.success('社保政策已创建');
    }
    dialogVisible.value = false;
    void fetchList();
  } catch {
    // toast already shown
  }
}

async function onDisable(id: number): Promise<void> {
  try {
    await disableRegionSocialRate(id);
    ElMessage.success('已禁用');
    void fetchList();
  } catch {
    // toast already shown
  }
}

/** 格式化百分比显示 */
function formatRate(val?: number): string {
  if (val == null) return '-';
  return (val * 100).toFixed(1) + '%';
}

onMounted(() => void fetchList());
</script>

<template>
  <div class="region-rate-page">
    <div class="header">
      <h2 class="title">多地区社保政策</h2>
      <el-button type="primary" @click="openCreate">新增城市政策</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="city" label="城市" width="100" />
      <el-table-column label="养老(个人/企业)" width="140">
        <template #default="{ row }">
          {{ formatRate(row.pensionPersonal) }} / {{ formatRate(row.pensionCompany) }}
        </template>
      </el-table-column>
      <el-table-column label="医疗(个人/企业)" width="140">
        <template #default="{ row }">
          {{ formatRate(row.medicalPersonal) }} / {{ formatRate(row.medicalCompany) }}
        </template>
      </el-table-column>
      <el-table-column label="失业(个人/企业)" width="140">
        <template #default="{ row }">
          {{ formatRate(row.unemploymentPersonal) }} / {{ formatRate(row.unemploymentCompany) }}
        </template>
      </el-table-column>
      <el-table-column label="公积金(个人/企业)" width="150">
        <template #default="{ row }">
          {{ formatRate(row.housingFundPersonal) }} / {{ formatRate(row.housingFundCompany) }}
        </template>
      </el-table-column>
      <el-table-column label="社保基数" width="180">
        <template #default="{ row }">
          {{ row.socialBaseFloor ?? '-' }} ~ {{ row.socialBaseCeil ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定禁用该政策？" @confirm="onDisable(row.id)">
            <template #reference>
              <el-button size="small" type="danger" :disabled="!row.enabled">禁用</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑社保政策' : '新增社保政策'" width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" placeholder="如：北京、上海、深圳" />
        </el-form-item>
        <el-divider content-position="left">养老保险比例</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="个人比例">
              <el-input-number v-model="form.pensionPersonal" :min="0" :max="1" :step="0.005" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="企业比例">
              <el-input-number v-model="form.pensionCompany" :min="0" :max="1" :step="0.005" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">医疗保险比例</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="个人比例">
              <el-input-number v-model="form.medicalPersonal" :min="0" :max="1" :step="0.005" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="企业比例">
              <el-input-number v-model="form.medicalCompany" :min="0" :max="1" :step="0.005" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="固定费用">
              <el-input-number v-model="form.medicalFixedFee" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">失业保险比例</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="个人比例">
              <el-input-number v-model="form.unemploymentPersonal" :min="0" :max="1" :step="0.005" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="企业比例">
              <el-input-number v-model="form.unemploymentCompany" :min="0" :max="1" :step="0.005" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">公积金比例</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="个人比例">
              <el-input-number v-model="form.housingFundPersonal" :min="0" :max="1" :step="0.005" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="企业比例">
              <el-input-number v-model="form.housingFundCompany" :min="0" :max="1" :step="0.005" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">基数上下限</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="社保基数下限">
              <el-input-number v-model="form.socialBaseFloor" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="社保基数上限">
              <el-input-number v-model="form.socialBaseCeil" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="公积金基数下限">
              <el-input-number v-model="form.fundBaseFloor" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公积金基数上限">
              <el-input-number v-model="form.fundBaseCeil" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">{{ editingId ? '更新' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.region-rate-page {
  padding: 24px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
</style>
