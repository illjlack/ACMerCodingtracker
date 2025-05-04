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
          v-if="user.avatar"
          :src="user.avatar"
          class="avatar"
        >
        <i
          v-else
          class="el-icon-plus avatar-uploader-icon"
        />
      </el-upload>
    </el-form-item>

    <!-- 登录名 -->
    <el-form-item label="用户名">
      <el-input v-model.trim="user.name" />
    </el-form-item>
    <!-- 真实姓名 -->
    <el-form-item label="真实姓名">
      <el-input v-model.trim="user.realName" />
    </el-form-item>
    <!-- 班级 -->
    <el-form-item label="班级">
      <el-input v-model.trim="user.className" />
    </el-form-item>
    <!-- 邮箱 -->
    <el-form-item label="Email">
      <el-input v-model.trim="user.email" />
    </el-form-item>
    <!-- Luogu 账号 -->
    <el-form-item label="Luogu 账号(多个账号用;隔开)">
      <el-input v-model.trim="user.luogu" placeholder="例如：tourist" />
    </el-form-item>
    <!-- Codeforces 账号 -->
    <el-form-item label="Codeforces 账号(多个账号用;隔开)">
      <el-input v-model.trim="user.codeforces" placeholder="例如：Benq" />
    </el-form-item>
    <!-- POJ 账号 -->
    <el-form-item label="POJ 账号(多个账号用;隔开)">
      <el-input v-model.trim="user.poj" placeholder="例如：123456" />
    </el-form-item>
    <!-- 提交按钮 -->
    <el-form-item>
      <el-button type="primary" @click="submit">更新信息</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: 'Account', // 组件名称
  props: {
    user: {
      type: Object,
      default: () => ({
        name: '', // 登录名
        email: '', // 邮箱
        realName: '', // 真实姓名
        className: '', // 班级
        avatar: '', // 头像 URL/Base64
        luogu: '', // Luogu 账号
        codeforces: '', // Codeforces 账号
        poj: '' // POJ 账号
      })
    }
  },
  methods: {
    /**
     * 在选择头像文件后，读取为 Base64 并赋值给 user.avatar
     * @param {File} file
     * @returns {Boolean} 返回 false 阻止 el-upload 自动上传
     */
    beforeAvatarUpload(file) {
      // 只允许图片格式
      const isImage = file.type.startsWith('image/')
      if (!isImage) {
        this.$message.error('上传头像只能是图片文件')
        return false
      }
      // 最大 2MB
      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isLt2M) {
        this.$message.error('图片大小不能超过 2MB')
        return false
      }
      // 读取为 Base64
      const reader = new FileReader()
      reader.onload = e => {
        this.user.avatar = e.target.result
        // 可选：立即通知父组件更新
        this.$emit('update', this.user)
      }
      reader.readAsDataURL(file)
      // 阻止 el-upload 自动提交
      return false
    },
    submit() {
      // 模拟更新成功提示，实际可调用 API 保存 user 对象
      this.$message({
        message: '用户信息已更新成功',
        type: 'success',
        duration: 5000
      })
      // 将最新的 user 数据传递给父组件
      this.$emit('update', this.user)
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
