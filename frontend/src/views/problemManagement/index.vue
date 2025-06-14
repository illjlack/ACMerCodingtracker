<template>
  <div class="problem-management-container">
    <div class="controls">
      <el-input
        v-model="search"
        placeholder="搜索题目ID或题目名称"
        clearable
        prefix-icon="el-icon-search"
        class="search-input"
        style="width: 250px; margin-right: 16px;"
      />

      <el-select
        v-model="selectedOjPlatforms"
        multiple
        collapse-tags
        placeholder="按OJ平台筛选"
        style="width: 200px; margin-right: 16px;"
        @change="filterData"
      >
        <el-option
          v-for="platform in ojPlatforms"
          :key="platform.code"
          :label="platform.name"
          :value="platform.code"
        />
      </el-select>

      <el-select
        v-model="selectedTags"
        multiple
        collapse-tags
        placeholder="按标签筛选"
        style="width: 280px; margin-right: 16px;"
        @change="filterData"
      >
        <el-option
          v-for="tag in allTags"
          :key="tag.id"
          :label="tag.name"
          :value="tag.id"
        >
          <span :style="{ color: tag.color }">{{ tag.name }}</span>
        </el-option>
      </el-select>

      <el-button v-if="isAdmin" type="info" icon="el-icon-collection-tag" @click="handleTagManagement">
        标签管理
      </el-button>

      <el-button v-if="isAdmin" type="success" icon="el-icon-upload2" @click="showImportDialog">
        批量导入
      </el-button>

      <problem-export v-if="isAdmin" :problem-data="displayList" :filters="exportFilters" />

      <el-button v-if="isAdmin" type="success" icon="el-icon-refresh" @click="refreshData">
        刷新数据
      </el-button>
    </div>

    <div class="stats-info">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ displayList.length }}</div>
              <div class="stat-label">当前显示题目</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ totalProblems }}</div>
              <div class="stat-label">总题目数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ ojPlatforms.length }}</div>
              <div class="stat-label">支持平台</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ allTags.length }}</div>
              <div class="stat-label">标签数量</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div class="table-wrapper">
      <el-table
        v-loading="loading"
        :data="paginatedList"
        border
        stripe
        style="width: 100%;"
        fit
        :row-key="row => `${row.ojName}-${row.pid}`"
      >
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="ojName" label="OJ平台" width="120">
          <template #default="{ row }">
            <el-tag :type="getOjTagType(row.ojName)">
              {{ getOjDisplayName(row.ojName) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pid" label="题目ID" width="120" />
        <el-table-column prop="name" label="题目名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="mini" type="info">{{ row.type || 'PROGRAMMING' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="难度分" width="100">
          <template #default="{ row }">
            <span v-if="row.points" :style="{ color: getDifficultyColor(row.points) }">
              {{ row.points }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="150">
          <template #default="{ row }">
            <el-tag
              v-for="tag in row.tags || []"
              :key="tag.id"
              :color="tag.color"
              :style="{ color: getTextColor(tag.color) }"
              style="margin-right: 5px; margin-bottom: 2px;"
              size="mini"
            >
              {{ tag.name }}
            </el-tag>
            <span v-if="!row.tags || row.tags.length === 0" class="text-muted">暂无标签</span>
          </template>
        </el-table-column>
        <el-table-column prop="url" label="链接" width="80">
          <template #default="{ row }">
            <el-button
              v-if="row.url"
              type="text"
              size="mini"
              @click="openProblemUrl(row.url)"
            >
              <i class="el-icon-link" />
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="isAdmin"
              size="mini"
              type="primary"
              @click="handleEditTags(row)"
            >
              编辑标签
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          :current-page="currentPage"
          :page-sizes="[20, 50, 100, 200]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="displayList.length"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 标签管理对话框 -->
    <el-dialog
      title="标签管理"
      :visible.sync="tagDialogVisible"
      width="800px"
    >
      <div class="tag-management">
        <div class="tag-controls">
          <el-button v-if="isAdmin" type="primary" icon="el-icon-plus" @click="handleAddTag">
            新建标签
          </el-button>
        </div>

        <el-table :data="allTags" border style="width: 100%; margin-top: 20px;">
          <el-table-column prop="name" label="标签名称" min-width="120" />
          <el-table-column prop="color" label="颜色" width="80">
            <template #default="{ row }">
              <div :style="{ backgroundColor: row.color, width: '20px', height: '20px', borderRadius: '3px' }" />
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="150" />
          <el-table-column label="使用统计" width="100">
            <template #default="{ row }">
              {{ getTagUsageCount(row.id) || 0 }} 题
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button v-if="isAdmin" size="mini" type="primary" @click="handleEditTag(row)">编辑</el-button>
              <el-button v-if="isAdmin" size="mini" type="danger" @click="handleDeleteTag(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 标签表单对话框 -->
    <el-dialog
      :title="tagFormTitle"
      :visible.sync="tagFormDialogVisible"
      width="400px"
    >
      <el-form
        ref="tagForm"
        :model="tagForm"
        :rules="tagRules"
        label-width="80px"
      >
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="tagForm.name" />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-color-picker v-model="tagForm.color" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="tagForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="tagFormDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitTagForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog
      title="批量导入题目"
      :visible.sync="importDialogVisible"
      width="500px"
    >
      <problem-import @import-success="handleImportSuccess" />
    </el-dialog>

    <!-- 题目标签编辑对话框 -->
    <el-dialog
      title="编辑题目标签"
      :visible.sync="problemTagDialogVisible"
      width="500px"
    >
      <div v-if="currentProblem">
        <p><strong>题目：</strong>{{ currentProblem.name || currentProblem.pid }}</p>
        <p><strong>平台：</strong>{{ getOjDisplayName(currentProblem.ojName) }}</p>

        <el-form label-width="80px" style="margin-top: 20px;">
          <el-form-item label="标签">
            <el-select v-model="problemTags" multiple style="width: 100%" placeholder="选择标签">
              <el-option
                v-for="tag in allTags"
                :key="tag.id"
                :label="tag.name"
                :value="tag.id"
              >
                <span :style="{ color: tag.color }">{{ tag.name }}</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="problemTagDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitProblemTags">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import {
  getAllTags as getAllProblemTags,
  createTag,
  updateTag,
  deleteTag
} from '@/api/tag'
import { getOJPlatforms } from '@/api/user'
import {
  getAllPbInfo,
  updateProblemTags
} from '@/api/problem'
import ProblemImport from './components/ProblemImport'
import ProblemExport from './components/ProblemExport'

export default {
  name: 'ProblemManagement',
  components: {
    ProblemImport,
    ProblemExport
  },
  data() {
    return {
      loading: false,
      search: '',
      list: [],
      filteredList: [],
      selectedOjPlatforms: [],
      selectedTags: [],
      allTags: [],
      tagUsageMap: {},
      ojPlatforms: [],
      currentPage: 1,
      pageSize: 50,

      // 对话框控制
      tagDialogVisible: false,
      tagFormDialogVisible: false,
      problemTagDialogVisible: false,
      importDialogVisible: false,
      isTagEdit: false,

      // 表单数据
      tagForm: {
        name: '',
        color: '#409EFF',
        description: ''
      },
      currentProblem: null,
      problemTags: [],

      // 验证规则
      tagRules: {
        name: [
          { required: true, message: '请输入标签名称', trigger: 'blur' },
          { min: 1, max: 20, message: '长度在 1 到 20 个字符', trigger: 'blur' }
        ],
        color: [
          { required: true, message: '请选择标签颜色', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    ...mapGetters(['roles']),
    isAdmin() {
      return this.roles.includes('ADMIN') || this.roles.includes('SUPER_ADMIN')
    },
    tagFormTitle() {
      return this.isTagEdit ? '编辑标签' : '新建标签'
    },
    displayList() {
      let filtered = [...this.list]

      // 按搜索关键词筛选
      const kw = this.search.trim().toLowerCase()
      if (kw) {
        filtered = filtered.filter(item =>
          (item.pid && item.pid.toLowerCase().includes(kw)) ||
          (item.name && item.name.toLowerCase().includes(kw))
        )
      }

      // 按OJ平台筛选
      if (this.selectedOjPlatforms.length > 0) {
        filtered = filtered.filter(item =>
          this.selectedOjPlatforms.includes(item.ojName)
        )
      }

      // 按标签筛选
      if (this.selectedTags.length > 0) {
        filtered = filtered.filter(item =>
          item.tags && item.tags.some(tag => this.selectedTags.includes(tag.id))
        )
      }

      return filtered
    },
    paginatedList() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.displayList.slice(start, end)
    },
    totalProblems() {
      return this.list.length
    },
    exportFilters() {
      return {
        search: this.search,
        selectedOjPlatforms: this.selectedOjPlatforms,
        selectedTags: this.selectedTags
      }
    }
  },
  created() {
    this.fetchData()
    this.fetchTags()
    this.fetchOJPlatforms()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const res = await getAllPbInfo()
        if (res.success) {
          this.list = res.data || []
        } else {
          this.$message.error(res.message || '获取题目列表失败')
          this.list = []
        }
      } catch (error) {
        console.error('获取题目列表失败:', error)
        this.$message.error('获取题目列表失败')
        this.list = []
      } finally {
        this.loading = false
      }
    },

    async fetchTags() {
      try {
        const res = await getAllProblemTags()
        if (res.success) {
          this.allTags = res.data || []
          // 计算标签使用统计
          this.calculateTagUsage()
        } else {
          this.$message.error(res.message || '获取标签列表失败')
          this.allTags = []
        }
      } catch (error) {
        console.error('获取标签列表失败:', error)
        this.$message.error('获取标签列表失败')
        this.allTags = []
      }
    },

    async fetchOJPlatforms() {
      try {
        const res = await getOJPlatforms()
        if (res.success) {
          this.ojPlatforms = res.data || []
        } else {
          this.$message.error(res.message || '获取OJ平台列表失败')
          this.ojPlatforms = []
        }
      } catch (error) {
        console.error('获取OJ平台列表失败:', error)
        this.$message.error('获取OJ平台列表失败')
        this.ojPlatforms = []
      }
    },

    refreshData() {
      this.fetchData()
      this.fetchTags()
    },

    filterData() {
      // 重置分页到第一页
      this.currentPage = 1
    },

    // 导入相关
    showImportDialog() {
      this.importDialogVisible = true
    },

    handleImportSuccess() {
      this.importDialogVisible = false
      this.refreshData()
      this.$message.success('题目导入成功，数据已刷新')
    },

    // 分页相关
    handleSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
    },

    handleCurrentChange(val) {
      this.currentPage = val
    },

    // 标签管理
    handleTagManagement() {
      this.tagDialogVisible = true
    },

    handleAddTag() {
      this.isTagEdit = false
      this.resetTagForm()
      this.tagFormDialogVisible = true
    },

    handleEditTag(tag) {
      this.isTagEdit = true
      this.tagForm = { ...tag }
      this.tagFormDialogVisible = true
    },

    async handleDeleteTag(tag) {
      try {
        await this.$confirm(`确认要删除标签"${tag.name}"吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteTag(tag.id)
        this.$message.success('删除成功')
        this.fetchTags()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
          this.$message.error('删除失败')
        }
      }
    },

    resetTagForm() {
      this.tagForm = {
        name: '',
        color: '#409EFF',
        description: ''
      }
      if (this.$refs.tagForm) {
        this.$refs.tagForm.resetFields()
      }
    },

    async submitTagForm() {
      try {
        await this.$refs.tagForm.validate()
        if (this.isTagEdit) {
          await updateTag(this.tagForm.id, this.tagForm)
        } else {
          await createTag(this.tagForm)
        }
        this.$message.success(this.isTagEdit ? '更新成功' : '创建成功')
        this.tagFormDialogVisible = false
        this.fetchTags()
      } catch (error) {
        console.error('提交失败:', error)
        this.$message.error('提交失败')
      }
    },

    // 题目标签编辑
    handleEditTags(problem) {
      this.currentProblem = problem
      this.problemTags = problem.tags ? problem.tags.map(tag => tag.id) : []
      this.problemTagDialogVisible = true
    },

    async submitProblemTags() {
      try {
        await updateProblemTags(this.currentProblem.id, {
          tagIds: this.problemTags
        })
        this.$message.success('标签更新成功')
        this.problemTagDialogVisible = false
        this.fetchData() // 重新获取数据以更新标签
      } catch (error) {
        console.error('更新标签失败:', error)
        this.$message.error('更新标签失败')
      }
    },

    // 工具方法
    calculateTagUsage() {
      this.tagUsageMap = {}
      for (const tag of this.allTags) {
        const count = this.list.filter(problem =>
          problem.tags && problem.tags.some(t => t.id === tag.id)
        ).length
        this.$set(this.tagUsageMap, tag.id, count)
      }
    },

    getTagUsageCount(tagId) {
      return this.tagUsageMap[tagId] || 0
    },

    getTextColor(bgColor) {
      if (!bgColor) return '#000000'
      let color = bgColor.startsWith('#') ? bgColor.substring(1, 7) : bgColor
      if (color.length === 3) {
        color = color.split('').map(char => char + char).join('')
      }
      if (color.length !== 6) {
        return '#000000'
      }

      const r = parseInt(color.substring(0, 2), 16)
      const g = parseInt(color.substring(2, 4), 16)
      const b = parseInt(color.substring(4, 6), 16)
      const brightness = (r * 299 + g * 587 + b * 114) / 1000
      return brightness > 125 ? '#000000' : '#FFFFFF'
    },

    getOjDisplayName(ojCode) {
      const platform = this.ojPlatforms.find(p => p.code === ojCode)
      return platform ? platform.name : ojCode
    },

    getOjTagType(ojCode) {
      const types = {
        'CODEFORCES': 'primary',
        'LEETCODE': 'success',
        'LUOGU': 'warning',
        'HDU': 'info',
        'POJ': 'danger'
      }
      return types[ojCode] || 'info'
    },

    getDifficultyColor(points) {
      if (points < 1000) return '#67C23A' // 绿色 - 简单
      if (points < 1500) return '#E6A23C' // 橙色 - 中等
      if (points < 2000) return '#F56C6C' // 红色 - 困难
      return '#9966CC' // 紫色 - 非常困难
    },

    openProblemUrl(url) {
      window.open(url, '_blank')
    }
  }
}
</script>

<style lang="scss" scoped>
.problem-management-container {
  padding: 20px;

  .controls {
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;

    .search-input {
      min-width: 200px;
    }

    @media (max-width: 1200px) {
      .search-input {
        width: 200px !important;
      }

      .el-select {
        width: 180px !important;
      }
    }
  }

  .stats-info {
    margin-bottom: 20px;

    .stat-card {
      text-align: center;

      .stat-item {
        .stat-value {
          font-size: 24px;
          font-weight: bold;
          color: #409EFF;
          margin-bottom: 5px;
        }

        .stat-label {
          font-size: 14px;
          color: #606266;
        }
      }
    }
  }

  .table-wrapper {
    .el-table {
      .el-tag {
        margin: 2px;
      }

      .text-muted {
        color: #909399;
        font-style: italic;
      }
    }
  }

  .pagination-wrapper {
    margin-top: 20px;
    text-align: center;
  }

  .tag-management {
    .tag-controls {
      margin-bottom: 20px;
    }
  }
}
</style>
