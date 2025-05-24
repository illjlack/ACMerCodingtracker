<template>
  <div class="acmer-login-container">
    <el-form
      ref="loginForm"
      :model="loginForm"
      :rules="loginRules"
      class="login-form"
      autocomplete="on"
      label-position="left"
    >
      <!-- 标题 -->
      <div class="title-container">
        <h3 class="title">
          ACMerCodingtracker<br>
          登录
        </h3>
      </div>

      <!-- 用户名输入 -->
      <el-form-item prop="username">
        <span class="svg-container">
          <svg-icon icon-class="user" />
        </span>
        <el-input
          ref="username"
          v-model="loginForm.username"
          placeholder="请输入 ACMer 用户名"
          name="username"
          type="text"
          tabindex="1"
          autocomplete="username"
        />
      </el-form-item>

      <!-- 密码输入 -->
      <el-tooltip
        v-model="capsTooltip"
        content="大写锁定已开启"
        placement="right"
        manual
      >
        <el-form-item prop="password">
          <span class="svg-container">
            <svg-icon icon-class="password" />
          </span>
          <el-input
            :key="passwordType"
            ref="password"
            v-model="loginForm.password"
            :type="passwordType"
            placeholder="请输入密码"
            name="password"
            tabindex="2"
            autocomplete="current-password"
            @keyup.native="checkCapslock"
            @blur="capsTooltip = false"
            @keyup.enter.native="handleLogin"
          />
          <span class="show-pwd" @click="togglePasswordVisibility">
            <svg-icon
              :icon-class="passwordType === 'password' ? 'eye' : 'eye-open'"
            />
          </span>
        </el-form-item>
      </el-tooltip>

      <!-- 登录按钮 -->
      <el-button
        :loading="loading"
        type="primary"
        style="width: 100%; margin-bottom: 10px;"
        @click.native.prevent="handleLogin"
      >
        登录
      </el-button>

      <div style="display: flex; justify-content: space-between; margin-top: 10px;">
        <el-button
          type="text"
          style="width: 48%; padding: 0;"
          @click="goRegister"
        >
          还没有账号？去注册
        </el-button>

        <el-button
          type="text"
          style="width: 48%; padding: 0;"
          @click="goForgotPassword"
        >
          忘记密码？
        </el-button>
      </div>

    </el-form>
  </div>
</template>

<script>
import { validUsername } from '@/utils/validate'

export default {
  name: 'ACMerLogin',
  data() {
    const validateUsername = (rule, value, callback) => {
      if (!validUsername(value)) {
        callback(new Error('请输入有效的用户名'))
      } else {
        callback()
      }
    }
    const validatePassword = (rule, value, callback) => {
      if (!value || value.length < 6) {
        callback(new Error('密码长度至少 6 位'))
      } else {
        callback()
      }
    }

    return {
      loginForm: {
        username: '',
        password: ''
      },
      loginRules: {
        username: [{ required: true, trigger: 'blur', validator: validateUsername }],
        password: [{ required: true, trigger: 'blur', validator: validatePassword }]
      },
      passwordType: 'password',
      capsTooltip: false,
      loading: false,
      redirect: null,
      otherQuery: {}
    }
  },
  watch: {
    $route: {
      handler(route) {
        const { redirect, ...rest } = route.query
        this.redirect = redirect
        this.otherQuery = rest
      },
      immediate: true
    }
  },
  mounted() {
    if (!this.loginForm.username) {
      this.$refs.username.focus()
    } else if (!this.loginForm.password) {
      this.$refs.password.focus()
    }
  },
  methods: {
    checkCapslock(e) {
      const { key } = e
      this.capsTooltip = key && key.length === 1 && key === key.toUpperCase()
    },
    togglePasswordVisibility() {
      this.passwordType = this.passwordType === 'password' ? 'text' : 'password'
      this.$nextTick(() => {
        this.$refs.password.focus()
      })
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return

        this.loading = true
        this.$store
          .dispatch('user/login', this.loginForm)
          .then(() => {
            this.$router.push({
              path: this.redirect || '/',
              query: this.otherQuery
            })
          })
          .finally(() => {
            this.loading = false
          })
      })
    },
    goRegister() {
      this.$router.push('/register')
    },
    goForgotPassword() {
      this.$router.push('/forgot-password')
    }
  }
}
</script>

<style lang="scss" scoped>
/* （保持之前你写的样式不变） */
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
    width: calc(100% - 40px);
    display: inline-block;

    input {
      background: transparent;
      border: none;
      padding: 12px 10px 12px 15px;
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

  .svg-container {
    width: 30px;
    display: inline-block;
    vertical-align: middle;
    color: #889aa4;
    padding-right: 5px;
  }

  .show-pwd {
    position: absolute;
    right: 15px;
    top: 50%;
    transform: translateY(-50%);
    cursor: pointer;
    color: #889aa4;
    user-select: none;
  }
}
</style>
