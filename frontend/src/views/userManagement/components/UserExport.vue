<template>
  <div class="user-export">
    <el-button type="primary" @click="handleExport">
      <i class="el-icon-download" /> 导出用户数据
    </el-button>
  </div>
</template>

<script>
import { getToken } from '@/utils/auth'

export default {
  name: 'UserExport',
  methods: {
    handleExport() {
      const token = getToken()
      const link = document.createElement('a')
      link.href = `/api/admin/users/export?token=${token}`
      link.download = `用户数据_${new Date().toLocaleDateString()}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    }
  }
}
</script>

<style lang="scss" scoped>
.user-export {
  display: inline-block;
}
</style>
