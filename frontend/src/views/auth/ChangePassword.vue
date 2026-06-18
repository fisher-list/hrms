<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { changePasswordApi } from '@/api/auth';
import { useAuthStore } from '@/store/auth';

interface ChangePasswordForm {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

const router = useRouter();
const auth = useAuthStore();

const formRef = ref<FormInstance>();
const submitting = ref<boolean>(false);
const form = reactive<ChangePasswordForm>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

/** Frontend password-strength checker (mirrors backend PasswordValidator rules). */
function validatePasswordStrength(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  if (!value) {
    callback(new Error('请输入新密码'));
    return;
  }
  const errors: string[] = [];
  if (value.length < 8) errors.push('密码长度不能少于 8 位');
  if (!/[A-Z]/.test(value)) errors.push('密码必须包含至少一个大写字母');
  if (!/[a-z]/.test(value)) errors.push('密码必须包含至少一个小写字母');
  if (!/[0-9]/.test(value)) errors.push('密码必须包含至少一个数字');
  if (auth.user?.username && value.toLowerCase().includes(auth.user.username.toLowerCase())) {
    errors.push('密码不能包含用户名');
  }
  if (errors.length > 0) {
    callback(new Error(errors[0]));
  } else {
    callback();
  }
}

function validateConfirmPassword(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  if (!value) {
    callback(new Error('请确认新密码'));
    return;
  }
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
}

const rules: FormRules<ChangePasswordForm> = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, validator: validatePasswordStrength, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' },
  ],
};

async function onSubmit(): Promise<void> {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  if (submitting.value) return;
  submitting.value = true;
  try {
    await changePasswordApi({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword,
    });
    ElMessage.success('密码修改成功，请重新登录');
    auth.setForceChangePassword(false);
    await auth.logout();
    await router.replace('/login');
  } catch {
    // Error toast already raised by axios interceptor.
  } finally {
    submitting.value = false;
  }
}

function onCancel(): void {
  if (auth.forceChangePassword) {
    // Can't cancel if forced — must change password. Logout instead.
    auth.logout();
    router.replace('/login');
  } else {
    router.back();
  }
}
</script>

<template>
  <div class="change-pwd-page">
    <el-card class="change-pwd-card" shadow="always">
      <template #header>
        <h2 class="change-pwd-title">修改密码</h2>
        <p v-if="auth.forceChangePassword" class="change-pwd-hint">
          首次登录请先修改初始密码
        </p>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        @submit.prevent="onSubmit"
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            placeholder="请输入旧密码"
            autocomplete="current-password"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="至少8位，含大小写字母和数字"
            autocomplete="new-password"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            autocomplete="new-password"
            show-password
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            class="change-pwd-btn"
            @click="onSubmit"
          >
            确认修改
          </el-button>
          <el-button
            v-if="!auth.forceChangePassword"
            class="change-pwd-btn"
            @click="onCancel"
          >
            取消
          </el-button>
          <el-button
            v-else
            class="change-pwd-btn"
            @click="onCancel"
          >
            退出登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.change-pwd-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.change-pwd-card {
  width: 420px;
}
.change-pwd-title {
  margin: 0;
  font-size: 18px;
  text-align: center;
}
.change-pwd-hint {
  margin: 4px 0 0;
  font-size: 13px;
  color: #e6a23c;
  text-align: center;
}
.change-pwd-btn {
  min-width: 100px;
}
</style>
