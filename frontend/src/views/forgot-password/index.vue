<template>
  <div class="acmer-login-container">
    <el-form
      ref="forgotForm"
      :model="forgotForm"
      :rules="forgotRules"
      class="login-form"
      autocomplete="on"
      label-position="left"
      label-width="80px"
    >
      <div class="title-container">
        <h3 class="title">找回密码</h3>
      </div>

      <!-- 用户名输入 -->
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="forgotForm.username"
          placeholder="请输入用户名"
          name="username"
          tabindex="1"
          autocomplete="username"
        />
      </el-form-item>

      <!-- 邮箱输入 -->
      <el-form-item label="邮箱" prop="email">
        <el-input
          v-model="forgotForm.email"
          placeholder="请输入注册时绑定的邮箱"
          name="email"
          type="email"
          tabindex="2"
          autocomplete="email"
        />
      </el-form-item>

      <!-- 验证码和发送按钮 -->
      <el-form-item label="验证码" prop="code" class="code-item">
        <div class="code-wrap">
          <el-input
            v-model="forgotForm.code"
            placeholder="请输入邮箱验证码"
            name="code"
            tabindex="3"
            class="code-input"
          />
          <el-button
            :loading="loading"
            type="primary"
            :disabled="countdown > 0"
            class="send-code-btn"
            @click="sendCode"
          >
            {{ countdown > 0 ? `${countdown}秒后可重新发送` : '发送验证码' }}
          </el-button>
        </div>
      </el-form-item>

      <!-- 新密码输入 -->
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="forgotForm.newPassword"
          placeholder="请输入新密码"
          name="newPassword"
          type="password"
          tabindex="4"
          autocomplete="new-password"
        />
      </el-form-item>

      <!-- 重置按钮 -->
      <el-button
        :loading="loading"
        type="primary"
        style="width: 100%; margin-top: 10px;"
        @click="handleResetPassword"
      >
        重置密码
      </el-button>

      <!-- 返回登录 -->
      <el-button
        type="text"
        style="width: 100%; margin-top: 15px;"
        @click="goLogin"
      >
        想起来了(或者我觉得还能再试试)？去登录
      </el-button>
    </el-form>
  </div>
</template>

<script>
import { sendEmailCode, resetPasswordByEmail } from '@/api/user'

export default {
  name: 'ForgotPassword',
  data() {
    const validateUsername = (rule, value, callback) => {
      if (!value || value.trim().length === 0) {
        callback(new Error('请输入用户名'))
      } else {
        callback()
      }
    }
    const validateEmail = (rule, value, callback) => {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!value || !emailRegex.test(value)) {
        callback(new Error('请输入有效的邮箱地址'))
      } else {
        callback()
      }
    }
    const validateCode = (rule, value, callback) => {
      if (!value || value.length !== 6) {
        callback(new Error('请输入6位验证码'))
      } else {
        callback()
      }
    }
    const validatePassword = (rule, value, callback) => {
      if (!value || value.length < 6) {
        callback(new Error('密码长度至少6位'))
      } else {
        callback()
      }
    }

    return {
      forgotForm: {
        username: '',
        email: '',
        code: '',
        newPassword: ''
      },
      forgotRules: {
        username: [{ required: true, trigger: 'blur', validator: validateUsername }],
        email: [{ required: true, trigger: 'blur', validator: validateEmail }],
        code: [{ required: true, trigger: 'blur', validator: validateCode }],
        newPassword: [{ required: true, trigger: 'blur', validator: validatePassword }]
      },
      loading: false,
      countdown: 0,
      countdownTimer: null
    }
  },
  beforeDestroy() {
    if (this.countdownTimer) clearInterval(this.countdownTimer)
  },
  methods: {
    // 发送验证码前先校验表单，校验未通过时给出提示
    sendCode() {
      this.$refs.forgotForm.validateField('email', errorMsg => {
        if (errorMsg) {
          this.$message.error('请输入有效的邮箱后再获取验证码')
          return
        }
        this.loading = true
        sendEmailCode({ email: this.forgotForm.email })
          .then(() => {
            this.$message.success('验证码已发送，请注意查收')
            this.startCountdown()
          })
          .catch(() => {
            this.$message.error('发送失败，请稍后重试')
          })
          .finally(() => {
            this.loading = false
          })
      })
    },
    startCountdown() {
      this.countdown = 60
      this.countdownTimer = setInterval(() => {
        if (this.countdown <= 0) {
          clearInterval(this.countdownTimer)
          this.countdownTimer = null
        } else {
          this.countdown--
        }
      }, 1000)
    },
    // 重置密码前也做完整表单校验
    handleResetPassword() {
      this.$refs.forgotForm.validate(valid => {
        if (!valid) {
          this.$message.error('请检查表单信息是否完整且正确')
          return
        }
        this.loading = true
        resetPasswordByEmail({
          username: this.forgotForm.username,
          email: this.forgotForm.email,
          code: this.forgotForm.code,
          newPassword: this.forgotForm.newPassword
        }).then(() => {
          this.$message.success('密码重置成功，请重新登录')
          this.goLogin()
        }).catch(err => {
          this.$message.error(err.response?.data?.message || '重置失败')
        }).finally(() => {
          this.loading = false
        })
      })
    },
    goLogin() {
      this.$router.push('/login')
    }
  }
}
</script>

<style lang="scss" scoped>
.acmer-login-container {
  min-height: 100vh;
  background-color: #2d3a4b;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;

  .login-form {
    width: 400px;
    padding: 40px 30px;
    background: #1f2d3d;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
    position: relative;
  }

  .title-container {
    text-align: center;
    margin-bottom: 30px;

    .title {
      font-size: 28px;
      color: #eee;
      margin: 0;
      font-weight: bold;
    }
  }

  .el-input {
    width: 100%;

    input {
      background: transparent;
      border: none;
      padding: 12px 15px;
      color: #eee;
      caret-color: #fff;

      &:-webkit-autofill {
        box-shadow: 0 0 0px 1000px #2d3a4b inset !important;
        -webkit-text-fill-color: #eee !important;
      }
    }
  }

  .el-form-item {
    margin-bottom: 20px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 4px;
    background: rgba(0, 0, 0, 0.1);
    padding-left: 10px;
    position: relative;
  }

  .code-item {
    .code-wrap {
      display: flex;
      align-items: center;
      width: 100%;

      .code-input {
        flex: 2;
      }

      .send-code-btn {
        flex: 1;
        margin-left: 10px;
        white-space: nowrap;
      }
    }
  }
}
</style>
