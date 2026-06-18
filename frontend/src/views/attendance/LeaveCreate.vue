<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { listLeaveTypes, createLeaveRequest, submitLeaveRequest } from '@/api/leave';
import type { LeaveTypeVo } from '@/types/hr';

const formRef = ref<FormInstance>();
const submitting = ref(false);

const types = ref<LeaveTypeVo[]>([]);

const form = reactive({
  leaveTypeId: undefined as number | undefined,
  range: [] as string[],
  days: 1,
  reason: '',
});

const rules: FormRules = {
  leaveTypeId: [{ required: true, message: '请选择假期类型', trigger: 'change' }],
  range: [
    {
      type: 'array',
      required: true,
      message: '请选择起止日期',
      trigger: 'change',
    },
  ],
  days: [
    { required: true, message: '请输入天数', trigger: 'blur' },
    {
      validator: (_r, v: number, cb) => {
        if (v == null || v <= 0) cb(new Error('天数必须大于 0'));
        else if ((v * 2) % 1 !== 0) cb(new Error('仅支持 0.5 天的倍数'));
        else cb();
      },
      trigger: 'blur',
    },
  ],
};

const computedDays = computed(() => {
  const [start, end] = form.range;
  if (!start || !end) return 0;
  const diff = (new Date(end).getTime() - new Date(start).getTime()) / (24 * 3600 * 1000);
  return Math.max(1, diff + 1);
});

async function loadTypes(): Promise<void> {
  try {
    types.value = await listLeaveTypes();
  } catch {
    ElMessage.error('加载假期类型失败');
  }
}

async function onSubmit(): Promise<void> {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const created = await createLeaveRequest({
      leaveTypeId: form.leaveTypeId!,
      startDate: form.range[0],
      endDate: form.range[1],
      days: form.days,
      reason: form.reason,
    });
    await submitLeaveRequest(created.id);
    ElMessage.success('请假已提交审批');
    onReset();
  } catch {
    // backend error toast handled by http interceptor
  } finally {
    submitting.value = false;
  }
}

function onReset(): void {
  form.leaveTypeId = undefined;
  form.range = [];
  form.days = 1;
  form.reason = '';
  formRef.value?.clearValidate();
}

onMounted(() => void loadTypes());
</script>

<template>
  <div class="leave-create">
    <h2 class="title">请假申请</h2>

    <el-card shadow="never" class="form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="假期类型" prop="leaveTypeId">
          <el-select v-model="form.leaveTypeId" placeholder="请选择" style="width: 320px">
            <el-option
              v-for="t in types"
              :key="t.id"
              :label="t.name"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="请假日期" prop="range">
          <el-date-picker
            v-model="form.range"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 320px"
          />
          <span class="hint">日历天数：{{ computedDays }}</span>
        </el-form-item>
        <el-form-item label="天数" prop="days">
          <el-input-number
            v-model="form.days"
            :min="0.5"
            :step="0.5"
            :precision="1"
            style="width: 200px"
          />
          <span class="hint">支持 0.5 天的倍数</span>
        </el-form-item>
        <el-form-item label="原因">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请填写请假原因"
            style="width: 480px"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            @click="onSubmit"
          >
            提交申请
          </el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.leave-create {
  padding: 24px;
}
.title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
.form-card {
  max-width: 720px;
}
.hint {
  color: #909399;
  margin-left: 12px;
  font-size: 13px;
}
</style>
