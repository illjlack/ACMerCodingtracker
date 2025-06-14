<template>
  <div class="problem-import">
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
        支持 Excel(.xlsx/.xls)、CSV、JSON 格式文件，且不超过 50MB
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
        <el-table-column prop="ojName" label="OJ平台" width="100" />
        <el-table-column prop="pid" label="题目ID" width="120" />
        <el-table-column prop="name" label="题目名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="points" label="难度分" width="80" />
        <el-table-column prop="tags" label="标签" width="120">
          <template #default="{ row }">
            <span v-if="row.tags && row.tags.length > 0">
              {{ row.tags.length }}个标签
            </span>
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="url" label="链接" width="100">
          <template #default>
            <span>有链接</span>
          </template>
        </el-table-column>
      </el-table>
      <p class="preview-info">
        共解析到 <strong>{{ previewData.length }}</strong> 条题目数据
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
import { batchUpdateProblems } from '@/api/problem'
import { getAllTags as getAllProblemTags } from '@/api/tag'

export default {
  name: 'ProblemImport',
  data() {
    return {
      selectedFile: null,
      importing: false,
      downloading: false,
      importProgress: 0,
      importStatus: null,
      previewData: [],
      errors: [],
      allTags: []
    }
  },
  created() {
    this.fetchTags()
  },
  methods: {
    async fetchTags() {
      try {
        const res = await getAllProblemTags()
        if (res.success) {
          this.allTags = res.data || []
        }
      } catch (error) {
        console.error('获取标签列表失败:', error)
      }
    },
    handleFileChange(file) {
      // 检查文件大小
      if (file.size > 50 * 1024 * 1024) {
        this.$message.error('文件大小不能超过 50MB!')
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
        const problem = {}
        const rowErrors = []

        // OJ平台验证
        if (!row.ojName && !row['OJ平台'] && !row['平台']) {
          rowErrors.push('OJ平台不能为空')
        } else {
          problem.ojName = row.ojName || row['OJ平台'] || row['平台']
          // 验证平台是否支持
          const supportedPlatforms = ['CODEFORCES', 'LEETCODE', 'LUOGU', 'HDU', 'POJ']
          if (!supportedPlatforms.includes(problem.ojName.toUpperCase())) {
            rowErrors.push(`不支持的OJ平台: ${problem.ojName}`)
          } else {
            problem.ojName = problem.ojName.toUpperCase()
          }
        }

        // 题目ID验证
        if (!row.pid && !row['题目ID'] && !row['ID']) {
          rowErrors.push('题目ID不能为空')
        } else {
          problem.pid = String(row.pid || row['题目ID'] || row['ID'])
        }

        // 题目名称验证
        if (!row.name && !row['题目名称'] && !row['名称']) {
          rowErrors.push('题目名称不能为空')
        } else {
          problem.name = row.name || row['题目名称'] || row['名称']
        }

        // 可选字段
        problem.type = row.type || row['类型'] || 'PROGRAMMING'
        problem.points = row.points || row['难度分'] || row['分数'] || null
        if (problem.points) {
          problem.points = parseInt(problem.points)
          if (isNaN(problem.points)) {
            problem.points = null
          }
        }

        problem.url = row.url || row['链接'] || row['URL'] || ''

        // 解析标签 - 格式: "数学,图论,动态规划"
        problem.tags = []
        const tagsStr = row.tags || row['标签'] || ''
        if (tagsStr) {
          try {
            const tagNames = tagsStr.split(',').map(s => s.trim()).filter(s => s)
            for (const tagName of tagNames) {
              const existingTag = this.allTags.find(t => t.name === tagName)
              if (existingTag) {
                problem.tags.push(existingTag.id)
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
          validData.push(problem)
        }
      })

      this.previewData = validData
      this.errors = errors

      if (errors.length > 0) {
        this.$message.warning(`数据解析完成，发现 ${errors.length} 个错误，请检查后重新上传`)
      } else {
        this.$message.success(`数据解析成功，共 ${validData.length} 条题目数据`)
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
          `确认导入 ${this.previewData.length} 条题目数据吗？`,
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

        // 批量创建题目
        try {
          await batchUpdateProblems(this.previewData)
          this.importProgress = 100
          this.importStatus = 'success'
          this.$message.success(`题目导入成功！共导入 ${this.previewData.length} 个题目`)
          this.$emit('import-success', {
            successCount: this.previewData.length,
            failCount: 0
          })
          this.clearFile()
        } catch (error) {
          this.importStatus = 'exception'
          console.error('导入失败:', error)
          this.$message.error('导入失败: ' + (error.response?.data?.message || error.message))
        }
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
        const tagExamples = this.allTags.slice(0, 3).map(tag => tag.name).join(',') || '数学,图论,动态规划'

        // 创建模板数据
        const templateData = [
          {
            'OJ平台': 'CODEFORCES',
            '题目ID': '1A',
            '题目名称': 'Theatre Square',
            '类型': 'PROGRAMMING',
            '难度分': 800,
            '链接': 'https://codeforces.com/problem/1/A',
            '标签': tagExamples
          },
          {
            'OJ平台': 'LEETCODE',
            '题目ID': '1',
            '题目名称': 'Two Sum',
            '类型': 'PROGRAMMING',
            '难度分': 1000,
            '链接': 'https://leetcode.com/problems/two-sum/',
            '标签': this.allTags.slice(1, 4).map(tag => tag.name).join(',') || '哈希表,数组'
          },
          {
            'OJ平台': 'LUOGU',
            '题目ID': 'P1000',
            '题目名称': '超级玛丽游戏',
            '类型': 'PROGRAMMING',
            '难度分': 100,
            '链接': 'https://www.luogu.com.cn/problem/P1000',
            '标签': '入门,模拟'
          },
          {
            'OJ平台': 'HDU',
            '题目ID': '1000',
            '题目名称': 'A + B Problem',
            '类型': 'PROGRAMMING',
            '难度分': 500,
            '链接': 'http://acm.hdu.edu.cn/showproblem.php?pid=1000',
            '标签': ''
          }
        ]

        // 创建工作簿
        const ws = XLSX.utils.json_to_sheet(templateData)
        const wb = XLSX.utils.book_new()
        XLSX.utils.book_append_sheet(wb, ws, '题目数据')

        // 设置列宽
        const colWidths = [
          { wch: 12 }, // OJ平台
          { wch: 15 }, // 题目ID
          { wch: 25 }, // 题目名称
          { wch: 12 }, // 类型
          { wch: 10 }, // 难度分
          { wch: 40 }, // 链接
          { wch: 25 } // 标签
        ]
        ws['!cols'] = colWidths

        // 添加说明信息
        const descRow = 6
        ws[`A${descRow}`] = { v: '支持的OJ平台: CODEFORCES, LEETCODE, LUOGU, HDU, POJ', t: 's' }
        ws[`A${descRow + 1}`] = { v: '标签格式说明: 标签1,标签2,标签3 (多个标签用逗号分隔)', t: 's' }
        ws[`A${descRow + 2}`] = { v: `可用标签: ${this.allTags.map(t => t.name).join(', ')}`, t: 's' }

        // 下载文件
        XLSX.writeFile(wb, `题目导入模板_${new Date().toISOString().slice(0, 10)}.xlsx`)

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
.problem-import {
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
