<template>
  <div class="user-import">
    <el-upload
      ref="upload"
      class="upload-demo"
      drag
      :auto-upload="false"
      :on-change="handleFileChange"
      :on-remove="handleFileRemove"
      accept=".xlsx,.xls,.csv,.json"
      :limit="1"
      :on-exceed="handleExceed"
    >
      <i class="el-icon-upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击选择文件</em></div>
      <div slot="tip" class="el-upload__tip">
        支持 Excel(.xlsx/.xls)、CSV、JSON 格式文件，且不超过 10MB
      </div>
    </el-upload>

    <div v-if="selectedFile" class="file-actions">
      <el-button
        type="primary"
        :loading="importing"
        :disabled="!selectedFile"
        @click="parseAndImport"
      >
        <i class="el-icon-upload2" /> 解析并导入
      </el-button>
      <el-button @click="clearFile">清除文件</el-button>
    </div>

    <div class="template-download">
      <el-button type="text" :loading="downloading" @click="downloadTemplate">
        <i class="el-icon-download" /> 下载导入模板
      </el-button>
    </div>

    <!-- 解析进度 -->
    <el-progress
      v-if="importing"
      :percentage="importProgress"
      :status="importStatus"
      style="margin-top: 20px;"
    />

    <!-- 预览数据 -->
    <div v-if="previewData.length > 0" class="preview-section">
      <h4>数据预览 (前5条记录)</h4>
      <el-table :data="previewData.slice(0, 5)" border size="mini" style="margin: 10px 0;">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="major" label="专业" width="150" />
        <el-table-column prop="roles" label="角色" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.roles && row.roles.includes('ADMIN')" type="warning">管理员</el-tag>
            <el-tag v-else type="info">普通用户</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ojAccounts" label="OJ账号" width="150">
          <template #default="{ row }">
            <span v-if="row.ojAccounts && row.ojAccounts.length > 0">
              {{ row.ojAccounts.length }}个账号
            </span>
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" width="120">
          <template #default="{ row }">
            <span v-if="row.tags && row.tags.length > 0">
              {{ row.tags.length }}个标签
            </span>
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="password" label="密码" width="100">
          <template #default>
            <span>******</span>
          </template>
        </el-table-column>
      </el-table>
      <p class="preview-info">
        共解析到 <strong>{{ previewData.length }}</strong> 条用户数据
        <span v-if="errors.length > 0" style="color: #f56c6c;">
          ，发现 <strong>{{ errors.length }}</strong> 个错误
        </span>
      </p>

      <!-- 错误信息 -->
      <div v-if="errors.length > 0" class="error-section">
        <h4 style="color: #f56c6c;">数据错误</h4>
        <ul class="error-list">
          <li v-for="(error, index) in errors.slice(0, 10)" :key="index" class="error-item">
            第 {{ error.row }} 行: {{ error.message }}
          </li>
          <li v-if="errors.length > 10" class="error-item">
            还有 {{ errors.length - 10 }} 个错误...
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script>
import * as XLSX from 'xlsx'
import { createUser, getOJPlatforms, getAllUserTags } from '@/api/user'

export default {
  name: 'UserImport',
  data() {
    return {
      selectedFile: null,
      importing: false,
      downloading: false,
      importProgress: 0,
      importStatus: null,
      previewData: [],
      errors: [],
      allTags: [],
      ojPlatforms: []
    }
  },
  created() {
    this.fetchTags()
    this.fetchOJPlatforms()
  },
  methods: {
    async fetchTags() {
      try {
        const res = await getAllUserTags()
        if (res.success) {
          this.allTags = res.data || []
        }
      } catch (error) {
        console.error('获取标签列表失败:', error)
      }
    },

    async fetchOJPlatforms() {
      try {
        const res = await getOJPlatforms()
        if (res.success) {
          this.ojPlatforms = res.data || []
        }
      } catch (error) {
        console.error('获取OJ平台列表失败:', error)
      }
    },
    handleFileChange(file) {
      // 检查文件大小
      if (file.size > 10 * 1024 * 1024) {
        this.$message.error('文件大小不能超过 10MB!')
        this.$refs.upload.clearFiles()
        return
      }

      this.selectedFile = file
      this.previewFile(file)
    },

    handleFileRemove() {
      this.clearFile()
    },

    handleExceed() {
      this.$message.warning('只能选择一个文件')
    },

    clearFile() {
      this.selectedFile = null
      this.previewData = []
      this.errors = []
      this.$refs.upload.clearFiles()
    },

    async previewFile(file) {
      try {
        const extension = file.name.split('.').pop().toLowerCase()
        let data = []

        if (extension === 'json') {
          data = await this.parseJSON(file.raw)
        } else if (extension === 'csv') {
          data = await this.parseCSV(file.raw)
        } else if (['xlsx', 'xls'].includes(extension)) {
          data = await this.parseExcel(file.raw)
        } else {
          throw new Error('不支持的文件格式')
        }

        this.validateAndPreview(data)
      } catch (error) {
        console.error('文件解析失败:', error)
        this.$message.error('文件解析失败: ' + error.message)
        this.clearFile()
      }
    },

    parseJSON(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = (e) => {
          try {
            const json = JSON.parse(e.target.result)
            const data = Array.isArray(json) ? json : [json]
            resolve(data)
          } catch (error) {
            reject(new Error('JSON格式错误'))
          }
        }
        reader.onerror = () => reject(new Error('文件读取失败'))
        reader.readAsText(file)
      })
    },

    parseCSV(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = (e) => {
          try {
            const csv = e.target.result
            const lines = csv.split('\n').filter(line => line.trim())
            if (lines.length < 2) {
              reject(new Error('CSV文件格式错误，至少需要表头和一行数据'))
              return
            }

            const headers = lines[0].split(',').map(h => h.trim().replace(/"/g, ''))
            const data = []

            for (let i = 1; i < lines.length; i++) {
              const values = lines[i].split(',').map(v => v.trim().replace(/"/g, ''))
              const row = {}
              headers.forEach((header, index) => {
                row[header] = values[index] || ''
              })
              data.push(row)
            }

            resolve(data)
          } catch (error) {
            reject(new Error('CSV解析失败'))
          }
        }
        reader.onerror = () => reject(new Error('文件读取失败'))
        reader.readAsText(file)
      })
    },

    parseExcel(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = (e) => {
          try {
            const data = new Uint8Array(e.target.result)
            const workbook = XLSX.read(data, { type: 'array' })
            const firstSheetName = workbook.SheetNames[0]
            const worksheet = workbook.Sheets[firstSheetName]
            const jsonData = XLSX.utils.sheet_to_json(worksheet)
            resolve(jsonData)
          } catch (error) {
            reject(new Error('Excel解析失败'))
          }
        }
        reader.onerror = () => reject(new Error('文件读取失败'))
        reader.readAsArrayBuffer(file)
      })
    },

    validateAndPreview(rawData) {
      const validData = []
      const errors = []

      rawData.forEach((row, index) => {
        const rowNum = index + 2 // Excel行号从2开始（第1行是表头）
        const user = {}
        const rowErrors = []

        // 用户名验证
        if (!row.username && !row['用户名']) {
          rowErrors.push('用户名不能为空')
        } else {
          user.username = row.username || row['用户名']
          if (user.username.length < 3 || user.username.length > 20) {
            rowErrors.push('用户名长度应在3-20个字符之间')
          }
        }

        // 真实姓名验证
        if (!row.realName && !row['真实姓名'] && !row['姓名']) {
          rowErrors.push('真实姓名不能为空')
        } else {
          user.realName = row.realName || row['真实姓名'] || row['姓名']
        }

        // 邮箱验证
        if (!row.email && !row['邮箱']) {
          rowErrors.push('邮箱不能为空')
        } else {
          user.email = row.email || row['邮箱']
          const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
          if (!emailRegex.test(user.email)) {
            rowErrors.push('邮箱格式不正确')
          }
        }

        // 专业
        user.major = row.major || row['专业'] || ''
        if (!user.major) {
          rowErrors.push('专业不能为空')
        }

        // 密码
        user.password = row.password || row['密码'] || '123456' // 默认密码
        if (user.password.length < 6) {
          rowErrors.push('密码长度不能少于6位')
        }

        // 角色
        user.roles = ['USER'] // 默认普通用户

        // 检查角色字段，但不允许设置超级管理员
        const roleStr = row.role || row['角色'] || ''
        if (roleStr) {
          const role = roleStr.toUpperCase()
          if (role === 'SUPER_ADMIN' || role === '超级管理员') {
            rowErrors.push('不允许通过导入设置超级管理员角色')
          } else if (role === 'ADMIN' || role === '管理员') {
            user.roles = ['ADMIN']
          } else {
            user.roles = ['USER']
          }
        }

        // 解析OJ账号 - 格式: "CODEFORCES:tourist; LEETCODE:petr"
        user.ojAccounts = []
        const ojAccountsStr = row.ojAccounts || row['OJ账号'] || row['oj账号'] || ''
        if (ojAccountsStr) {
          try {
            const ojAccountPairs = ojAccountsStr.split(';').map(s => s.trim()).filter(s => s)
            for (const pair of ojAccountPairs) {
              if (pair.includes(':')) {
                const [platform, accountName] = pair.split(':').map(s => s.trim())
                if (platform && accountName) {
                  // 验证平台是否支持
                  const platformExists = this.ojPlatforms.some(p =>
                    p.code.toUpperCase() === platform.toUpperCase()
                  )
                  if (platformExists) {
                    user.ojAccounts.push({
                      platform: platform.toUpperCase(),
                      accountName: accountName
                    })
                  } else {
                    rowErrors.push(`不支持的OJ平台: ${platform}`)
                  }
                }
              } else {
                rowErrors.push(`OJ账号格式错误: ${pair}，正确格式为"平台:账号名"`)
              }
            }
          } catch (error) {
            rowErrors.push('OJ账号解析失败')
          }
        }

        // 解析标签 - 格式: "算法,数据结构,动态规划"
        user.tags = []
        const tagsStr = row.tags || row['标签'] || ''
        if (tagsStr) {
          try {
            const tagNames = tagsStr.split(',').map(s => s.trim()).filter(s => s)
            for (const tagName of tagNames) {
              const existingTag = this.allTags.find(t => t.name === tagName)
              if (existingTag) {
                user.tags.push(existingTag.id)
              } else {
                rowErrors.push(`标签不存在: ${tagName}`)
              }
            }
          } catch (error) {
            rowErrors.push('标签解析失败')
          }
        }

        if (rowErrors.length > 0) {
          errors.push({
            row: rowNum,
            message: rowErrors.join('; ')
          })
        } else {
          validData.push(user)
        }
      })

      this.previewData = validData
      this.errors = errors

      if (errors.length > 0) {
        this.$message.warning(`数据解析完成，发现 ${errors.length} 个错误，请检查后重新上传`)
      } else {
        this.$message.success(`数据解析成功，共 ${validData.length} 条用户数据`)
      }
    },

    async parseAndImport() {
      if (this.errors.length > 0) {
        this.$message.error('请先修复数据错误后再导入')
        return
      }

      if (this.previewData.length === 0) {
        this.$message.error('没有可导入的数据')
        return
      }

      try {
        await this.$confirm(
          `确认导入 ${this.previewData.length} 条用户数据吗？`,
          '确认导入',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        this.importing = true
        this.importProgress = 0
        this.importStatus = null

        let successCount = 0
        let failCount = 0
        const failedUsers = []

        for (let i = 0; i < this.previewData.length; i++) {
          try {
            await createUser(this.previewData[i])
            successCount++
          } catch (error) {
            failCount++
            failedUsers.push({
              username: this.previewData[i].username,
              error: error.response?.data?.message || error.message
            })
          }

          // 更新进度
          this.importProgress = Math.round(((i + 1) / this.previewData.length) * 100)
        }

        this.importStatus = failCount === 0 ? 'success' : 'warning'

        // 显示结果
        if (failCount === 0) {
          this.$message.success(`导入成功！共导入 ${successCount} 个用户`)
        } else {
          this.$message.warning(`导入完成！成功 ${successCount} 个，失败 ${failCount} 个`)

          // 显示失败详情
          if (failedUsers.length > 0) {
            const failedInfo = failedUsers.slice(0, 5).map(u =>
              `${u.username}: ${u.error}`
            ).join('\n')
            this.$alert(
              failedInfo + (failedUsers.length > 5 ? '\n...' : ''),
              '导入失败详情',
              { type: 'warning' }
            )
          }
        }

        this.$emit('import-success', { successCount, failCount })
        this.clearFile()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('导入失败:', error)
          this.$message.error('导入失败')
          this.importStatus = 'exception'
        }
      } finally {
        this.importing = false
      }
    },

    downloadTemplate() {
      this.downloading = true
      try {
        // 生成标签示例
        const tagExamples = this.allTags.slice(0, 3).map(tag => tag.name).join(',') || '算法,数据结构,动态规划'

        // 生成OJ平台示例
        this.ojPlatforms.slice(0, 2).map(p => p.code).join('; ') || 'CODEFORCES; LEETCODE'

        const templateData = [
          {
            '用户名': 'user001',
            '真实姓名': '张三',
            '邮箱': 'zhangsan@example.com',
            '专业': '计算机科学与技术',
            '密码': '123456',
            '角色': 'USER',
            'OJ账号': 'CODEFORCES:tourist; LEETCODE:zhangsan',
            '标签': tagExamples
          },
          {
            '用户名': 'user002',
            '真实姓名': '李四',
            '邮箱': 'lisi@example.com',
            '专业': '软件工程',
            '密码': '123456',
            '角色': 'ADMIN',
            'OJ账号': 'LUOGU:lisi2024',
            '标签': this.allTags.slice(1, 3).map(tag => tag.name).join(',') || '编程,竞赛'
          },
          {
            '用户名': 'user003',
            '真实姓名': '王五',
            '邮箱': 'wangwu@example.com',
            '专业': '信息安全',
            '密码': '123456',
            '角色': 'USER',
            'OJ账号': '',
            '标签': ''
          }
        ]

        // 创建工作簿
        const ws = XLSX.utils.json_to_sheet(templateData)
        const wb = XLSX.utils.book_new()
        XLSX.utils.book_append_sheet(wb, ws, '用户数据')

        // 设置列宽
        const colWidths = [
          { wch: 12 }, // 用户名
          { wch: 15 }, // 真实姓名
          { wch: 25 }, // 邮箱
          { wch: 20 }, // 专业
          { wch: 8 }, // 密码
          { wch: 10 }, // 角色
          { wch: 35 }, // OJ账号
          { wch: 25 } // 标签
        ]
        ws['!cols'] = colWidths

        // 添加说明信息
        if (!ws['!merges']) ws['!merges'] = []

        // 在第5行添加说明
        const descRow = 5
        ws[`A${descRow}`] = { v: '角色说明: USER(普通用户) 或 ADMIN(管理员)，不支持SUPER_ADMIN', t: 's' }
        ws[`A${descRow + 1}`] = { v: 'OJ账号格式说明: 平台:账号名; 平台:账号名 (多个账号用分号分隔)', t: 's' }
        ws[`A${descRow + 2}`] = { v: `支持的平台: ${this.ojPlatforms.map(p => p.code).join(', ')}`, t: 's' }
        ws[`A${descRow + 3}`] = { v: '标签格式说明: 标签1,标签2,标签3 (多个标签用逗号分隔)', t: 's' }
        ws[`A${descRow + 4}`] = { v: `可用标签: ${this.allTags.map(t => t.name).join(', ')}`, t: 's' }

        // 下载文件
        XLSX.writeFile(wb, `用户导入模板_${new Date().toISOString().slice(0, 10)}.xlsx`)

        this.$message.success('模板下载成功')
      } catch (error) {
        console.error('下载模板失败:', error)
        this.$message.error('下载模板失败')
      } finally {
        this.downloading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.user-import {
  padding: 20px;

  .upload-demo {
    margin-bottom: 20px;
  }

  .file-actions {
    margin: 20px 0;
    text-align: center;

    .el-button {
      margin: 0 10px;
    }
  }

  .template-download {
    text-align: center;
    margin-top: 20px;

    .el-button {
      color: #409EFF;

      &:hover {
        color: #66b1ff;
      }
    }
  }

  .preview-section {
    margin-top: 20px;
    padding: 15px;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    background-color: #fafafa;

    h4 {
      margin-bottom: 10px;
      color: #606266;
    }

    .preview-info {
      margin: 10px 0;
      font-size: 14px;
      color: #606266;
    }
  }

  .error-section {
    margin-top: 15px;

    h4 {
      margin-bottom: 10px;
    }

    .error-list {
      max-height: 200px;
      overflow-y: auto;
      padding-left: 20px;

      .error-item {
        margin-bottom: 5px;
        font-size: 13px;
        color: #f56c6c;
      }
    }
  }

  .text-muted {
    color: #909399;
    font-style: italic;
  }
}
</style>
