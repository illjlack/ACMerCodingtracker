<template>
  <el-form>
    <!-- 头像上传 -->
    <el-form-item label="头像">
      <el-upload
        class="avatar-uploader"
        :show-file-list="false"
        :before-upload="beforeAvatarUpload"
      >
        <img
          :src="avatarUrl"
          class="avatar"
          @error="onAvatarError"
        >
      </el-upload>
    </el-form-item>

    <!-- 用户名（不可修改） -->
    <el-form-item label="用户名">
      <el-input v-model.trim="localUser.name" disabled />
    </el-form-item>

    <!-- 真实姓名 -->
    <el-form-item label="真实姓名">
      <el-input v-model.trim="localUser.realName" />
    </el-form-item>

    <!-- 班级 -->
    <el-form-item label="班级">
      <el-input v-model.trim="localUser.className" />
    </el-form-item>

    <!-- 邮箱 -->
    <el-form-item label="Email">
      <el-input v-model.trim="localUser.email" />
    </el-form-item>

    <!-- Luogu 账号 -->
    <el-form-item label="Luogu 账号(多个账号用;隔开)">
      <el-input v-model.trim="localUser.luogu" placeholder="例如：tourist" />
    </el-form-item>

    <!-- Codeforces 账号 -->
    <el-form-item label="Codeforces 账号(多个账号用;隔开)">
      <el-input v-model.trim="localUser.codeforces" placeholder="例如：Benq" />
    </el-form-item>

    <!-- POJ 账号 -->
    <el-form-item label="POJ 账号(多个账号用;隔开)">
      <el-input v-model.trim="localUser.poj" placeholder="例如：123456" />
    </el-form-item>

    <!-- 提交按钮 -->
    <el-form-item>
      <el-button type="primary" @click="submit">更新信息</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import defaultAvatar from '@/assets/default-avatar.png'

export default {
  name: 'Account',
  props: {
    user: {
      type: Object,
      default: () => ({
        name: '',
        email: '',
        realName: '',
        className: '',
        avatar: '',
        luogu: '',
        codeforces: '',
        poj: ''
      })
    }
  },
  data() {
    return {
      localUser: { ...this.user },
      defaultAvatar,
      avatarError: false // 标志是否需要使用默认头像
    }
  },
  computed: {
    baseApiUrl() {
      return process.env.VUE_APP_BASE_API || 'http://localhost:8080'
    },
    avatarUrl() {
      const avatar = this.localUser.avatar
      if (!avatar || this.avatarError) return this.defaultAvatar
      if (avatar.startsWith('http')) return avatar
      return this.baseApiUrl + avatar
    }
  },
  methods: {
    onAvatarError() {
      this.avatarError = true
    },
    beforeAvatarUpload(file) {
      const isImage = file.type.startsWith('image/')
      if (!isImage) {
        this.$message.error('上传头像只能是图片文件')
        return false
      }

      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isLt2M) {
        this.$message.error('图片大小不能超过 2 MB')
        return false
      }

      this.$store.dispatch('user/uploadAvatar', file)
        .then(url => {
          this.localUser.avatar = url
          this.avatarError = false // 重置错误标志
          this.$message.success('头像上传成功')
        })
        .catch(() => {
          this.$message.error('头像上传失败')
        })

      return false
    },
    submit() {
      this.$emit('update', { ...this.localUser })
      this.$message({
        message: '用户信息已更新成功',
        type: 'success',
        duration: 5000
      })
    }
  }
}
</script>

<style scoped>
.avatar-uploader {
  display: inline-block;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
}
.avatar {
  width: 100px;
  height: 100px;
  display: block;
  border-radius: 6px;
  cursor: pointer;
}
</style>
