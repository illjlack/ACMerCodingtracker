<template>
  <div class="problem-export">
    <el-dropdown trigger="click" @command="handleExport">
      <el-button
        type="primary"
        :loading="exporting"
      >
        <i class="el-icon-download" /> 导出题目数据
        <i class="el-icon-arrow-down el-icon--right" />
      </el-button>
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item command="excel">导出为 Excel</el-dropdown-item>
        <el-dropdown-item command="csv">导出为 CSV</el-dropdown-item>
        <el-dropdown-item command="json">导出为 JSON</el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
  </div>
</template>

<script>
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

export default {
  name: 'ProblemExport',
  props: {
    // 传入要导出的题目数据
    problemData: {
      type: Array,
      default: () => []
    },
    // 可选：传入筛选条件
    filters: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      exporting: false
    }
  },
  methods: {
    async handleExport(format) {
      if (this.problemData.length === 0) {
        this.$message.warning('没有可导出的数据')
        return
      }

      this.exporting = true
      try {
        // 准备导出数据
        const exportData = this.prepareExportData()

        switch (format) {
          case 'excel':
            this.exportToExcel(exportData)
            break
          case 'csv':
            this.exportToCSV(exportData)
            break
          case 'json':
            this.exportToJSON(exportData)
            break
          default:
            throw new Error('不支持的导出格式')
        }

        this.$message.success(`导出${format.toUpperCase()}成功`)
      } catch (error) {
        console.error('导出失败:', error)
        this.$message.error('导出失败: ' + error.message)
      } finally {
        this.exporting = false
      }
    },

    prepareExportData() {
      return this.problemData.map(problem => ({
        'OJ平台': problem.ojName,
        '题目ID': problem.pid,
        '题目名称': problem.name,
        '类型': problem.type || 'PROGRAMMING',
        '难度分': problem.points || '',
        '链接': problem.url || '',
        '标签': this.formatTags(problem.tags),
        '创建时间': problem.createTime ? new Date(problem.createTime).toLocaleString() : '',
        '更新时间': problem.updateTime ? new Date(problem.updateTime).toLocaleString() : ''
      }))
    },

    formatTags(tags) {
      if (!tags || tags.length === 0) return ''
      return tags.map(tag => tag.name).join(', ')
    },

    exportToExcel(data) {
      try {
        // 创建工作簿
        const ws = XLSX.utils.json_to_sheet(data)
        const wb = XLSX.utils.book_new()
        XLSX.utils.book_append_sheet(wb, ws, '题目数据')

        // 设置列宽
        const colWidths = [
          { wch: 12 }, // OJ平台
          { wch: 15 }, // 题目ID
          { wch: 30 }, // 题目名称
          { wch: 12 }, // 类型
          { wch: 10 }, // 难度分
          { wch: 40 }, // 链接
          { wch: 25 }, // 标签
          { wch: 20 }, // 创建时间
          { wch: 20 } // 更新时间
        ]
        ws['!cols'] = colWidths

        // 生成文件并下载
        XLSX.writeFile(wb, `题目数据_${new Date().toISOString().slice(0, 10)}.xlsx`)
      } catch (error) {
        throw new Error('Excel导出失败: ' + error.message)
      }
    },

    exportToCSV(data) {
      try {
        // 创建CSV内容
        const headers = Object.keys(data[0])
        const csvContent = [
          headers.join(','), // 表头
          ...data.map(row =>
            headers.map(header => {
              const value = row[header] || ''
              // 处理包含逗号、换行符或引号的值
              if (value.includes(',') || value.includes('\n') || value.includes('"')) {
                return `"${value.replace(/"/g, '""')}"`
              }
              return value
            }).join(',')
          )
        ].join('\n')

        // 添加BOM以支持Excel打开中文
        const BOM = '\uFEFF'
        const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8' })

        saveAs(blob, `题目数据_${new Date().toISOString().slice(0, 10)}.csv`)
      } catch (error) {
        throw new Error('CSV导出失败: ' + error.message)
      }
    },

    exportToJSON(data) {
      try {
        // 导出原始数据结构（更适合程序处理）
        const jsonData = {
          exportTime: new Date().toISOString(),
          filters: this.filters,
          totalCount: this.problemData.length,
          data: this.problemData
        }

        const jsonString = JSON.stringify(jsonData, null, 2)
        const blob = new Blob([jsonString], { type: 'application/json;charset=utf-8' })

        saveAs(blob, `题目数据_${new Date().toISOString().slice(0, 10)}.json`)
      } catch (error) {
        throw new Error('JSON导出失败: ' + error.message)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.problem-export {
  display: inline-block;

  .el-button {
    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
    }

    &.is-loading {
      transform: none;
      box-shadow: none;
    }
  }
}
</style>
