<template>
  <div class="user-import">
    <el-upload
      class="upload-demo"
      drag
      action="/api/admin/users/import"
      :headers="headers"
      :on-success="handleSuccess"
      :on-error="handleError"
      :before-upload="beforeUpload"
      accept=".xlsx,.xls"
    >
      <i class="el-icon-upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <div slot="tip" class="el-upload__tip">只能上传 xlsx/xls 文件</div>
    </el-upload>

    <div class="template-download">
      <el-button type="text" @click="downloadTemplate">
        <i class="el-icon-download" /> 下载导入模板
      </el-button>
    </div>
  </div>
</template>

<script>
import { getToken } from '@/utils/auth'

export default {
  name: 'UserImport',
  data() {
    return {
      headers: {
        Authorization: `Bearer ${getToken()}`
      }
    }
  },
  methods: {
    beforeUpload(file) {
      const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
                      file.type === 'application/vnd.ms-excel'
      if (!isExcel) {
        this.$message.error('只能上传 Excel 文件!')
        return false
      }
      return true
    },
    handleSuccess(response) {
      if (response.code === 200) {
        this.$message.success('导入成功')
        this.$emit('import-success')
      } else {
        this.$message.error(response.message || '导入失败')
      }
    },
    handleError() {
      this.$message.error('导入失败')
    },
    downloadTemplate() {
      window.open('/api/admin/users/template', '_blank')
    }
  }
}
</script>

<style lang="scss" scoped>
.user-import {
  padding: 20px;

  .template-download {
    margin-top: 20px;
    text-align: center;
  }
}
</style>
