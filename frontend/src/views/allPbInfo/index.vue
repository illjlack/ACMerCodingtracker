<template>
  <div class="dashboard-ranking-container">
    <div class="update-info" style="margin-bottom: 12px; display: flex; align-items: center;">
      <div>
        上次爬虫数据更新时间：
        <span v-if="lastUpdate">{{ lastUpdate }}</span>
        <span v-else>加载中...</span>
        <el-button
          size="mini"
          type="warning"
          :loading="updating"
          style="margin-left: 12px;"
          @click="manualUpdate"
        >
          马上更新
        </el-button>
      </div>
    </div>

    <div class="controls">
      <div class="time-label1">开始时间：</div>
      <el-date-picker
        v-model="startDate"
        type="date"
        placeholder="开始日期"
        :picker-options="pickerOptions"
        :clearable="false"
        style="margin-right: 8px;"
      />
      <div class="time-label1">结束时间：</div>
      <el-date-picker
        v-model="endDate"
        type="date"
        placeholder="结束日期"
        :picker-options="pickerOptions"
        :clearable="false"
        style="margin-right: 24px;"
      />

      <el-select
        v-model="selectedPlatforms"
        multiple
        collapse-tags
        placeholder="筛选平台"
        style="width: 200px; margin-right: 16px;"
        clearable
      >
        <el-option
          v-for="platform in allPlatforms"
          :key="platform.code"
          :label="platform.name"
          :value="platform.code"
        />
      </el-select>

      <el-select
        v-model="selectedTags"
        multiple
        collapse-tags
        placeholder="筛选标签"
        style="width: 200px; margin-right: 16px;"
        clearable
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

      <el-input
        v-model="search"
        placeholder="搜索用户名或真实姓名"
        clearable
        prefix-icon="el-icon-search"
        class="search-input"
        style="width: 200px; margin-right: 16px;"
      />

      <el-button type="primary" icon="el-icon-refresh" @click="onRefresh">
        更新数据
      </el-button>

      <el-button
        type="success"
        icon="el-icon-document"
        style="margin-left: 12px;"
        @click="exportExcel"
      >
        导出表格
      </el-button>

      <el-button
        type="info"
        icon="el-icon-refresh-left"
        style="margin-left: 12px;"
        @click="resetSettings"
      >
        重置筛选
      </el-button>
    </div>

    <div class="table-wrapper">
      <el-table
        v-loading="loading"
        :data="filteredList"
        border
        stripe
        style="width: 100%;"
        :default-sort="{ prop: 'acCount', order: 'descending' }"
        fit
      >
        <el-table-column type="index" label="序号" min-width="60" />

        <el-table-column label="用户名" min-width="120">
          <template #default="{ row }">
            <router-link :to="`/userPbInfo/${row.username}`" style="color: #409EFF;">
              {{ row.username }}
            </router-link>
          </template>
        </el-table-column>

        <el-table-column prop="realName" label="真实姓名" min-width="120" />

        <el-table-column prop="tags" label="标签" min-width="120">
          <template #default="{ row }">
            <el-tag
              v-for="tag in row.tags"
              :key="tag.id"
              :color="tag.color"
              :style="{ color: getTextColor(tag.color) }"
              style="margin-right: 5px; margin-bottom: 2px;"
              size="mini"
            >
              {{ tag.name }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="总数(AC / 尝试)" min-width="120" :sortable="true" :sort-method="sortNumber">
          <template #default="{ row }">
            {{ row.acCount }} / {{ row.tryCount }}
          </template>
        </el-table-column>

        <el-table-column
          v-for="platform in displayPlatforms"
          :key="platform.code"
          :label="platform.name"
          min-width="100"
          :sortable="true"
          :sort-method="sortNumber"
        >
          <template #default="{ row }">
            {{ (row.acCounts[platform.code] || 0) + ' / ' + (row.tryCounts[platform.code] || 0) }}
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import { fetchAcCounts, fetchTryCounts, fetchLastUpdate, manualRebuild, forceUpdateDB } from '@/api/usertry'
import { getAllUserTags, getAllUsers } from '@/api/user'
import { getOJPlatforms } from '@/api/user'
import { export_json_to_excel } from '@/vendor/Export2Excel'
import { mapGetters } from 'vuex'

export default {
  name: 'UserStatsDashboard',
  data() {
    return {
      loading: false,
      userStatsMap: {},
      list: [],
      allUsers: [], // 存储所有用户信息
      allPlatforms: [], // 所有可用平台
      selectedPlatforms: [], // 选中的平台
      allTags: [], // 所有标签
      selectedTags: [], // 选中的标签
      pickerOptions: {
        disabledDate(time) {
          const earliest = new Date(2021, 0, 1).getTime()
          const now = Date.now()
          return time.getTime() < earliest || time.getTime() > now
        }
      },
      lastUpdate: null,
      updating: false
    }
  },
  computed: {
    ...mapGetters([
      'allPbInfoStartDate',
      'allPbInfoEndDate',
      'allPbInfoSelectedPlatforms',
      'allPbInfoSelectedTags',
      'allPbInfoSearch',
      'roles'
    ]),
    startDate: {
      get() {
        return this.allPbInfoStartDate
      },
      set(value) {
        this.$store.dispatch('allPbInfo/setStartDate', value)
      }
    },
    endDate: {
      get() {
        return this.allPbInfoEndDate
      },
      set(value) {
        this.$store.dispatch('allPbInfo/setEndDate', value)
      }
    },
    search: {
      get() {
        return this.allPbInfoSearch
      },
      set(value) {
        this.$store.dispatch('allPbInfo/setSearch', value)
      }
    },
    isAdmin() {
      return this.roles.includes('ADMIN') || this.roles.includes('SUPER_ADMIN')
    },
    // 显示的平台列表（如果有选中的平台，只显示选中的；否则显示所有）
    displayPlatforms() {
      if (this.selectedPlatforms.length > 0) {
        return this.allPlatforms.filter(p => this.selectedPlatforms.includes(p.code))
      }
      return this.allPlatforms
    },
    filteredList() {
      let filtered = [...this.list]

      // 按搜索关键词筛选
      const kw = this.search.trim().toLowerCase()
      if (kw) {
        filtered = filtered.filter(
          item =>
            (item.username && item.username.toLowerCase().includes(kw)) ||
            (item.realName && item.realName.toLowerCase().includes(kw))
        )
      }

      // 按标签筛选
      if (this.selectedTags.length > 0) {
        filtered = filtered.filter(user => {
          return user.tags && user.tags.some(tag => this.selectedTags.includes(tag.id))
        })
      }

      return filtered
    }
  },
  watch: {
    startDate: {
      immediate: true,
      handler(newVal) {
        if (newVal) {
          this.fetchData()
        }
      }
    },
    endDate: {
      immediate: true,
      handler(newVal) {
        if (newVal) {
          this.fetchData()
        }
      }
    },
    selectedPlatforms(newVal) {
      this.$store.dispatch('allPbInfo/setSelectedPlatforms', newVal)
    },
    selectedTags(newVal) {
      this.$store.dispatch('allPbInfo/setSelectedTags', newVal)
    }
  },
  created() {
    this.initializeData()
  },
  methods: {
    async initializeData() {
      try {
        // 从 store 获取已保存的设置
        this.selectedPlatforms = [...this.allPbInfoSelectedPlatforms]
        this.selectedTags = [...this.allPbInfoSelectedTags]

        // 初始化各种数据
        await Promise.all([
          this.fetchPlatforms(),
          this.fetchTags(),
          this.fetchAllUsers(),
          this.fetchLastUpdateTime()
        ])

        // 确保有日期后再获取数据
        if (this.startDate && this.endDate) {
          await this.fetchData()
        }
      } catch (error) {
        console.error('初始化数据失败:', error)
        this.$message.error('初始化数据失败')
      }
    },

    async fetchPlatforms() {
      try {
        const res = await getOJPlatforms()
        if (res.success) {
          this.allPlatforms = res.data || []
        } else {
          this.$message.error(res.message || '获取平台列表失败')
          this.allPlatforms = []
        }
      } catch (error) {
        console.error('获取平台列表失败:', error)
        this.$message.error('获取平台列表失败')
        this.allPlatforms = []
      }
    },

    async fetchTags() {
      try {
        const res = await getAllUserTags()
        if (res.success) {
          this.allTags = res.data || []
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

    async fetchAllUsers() {
      try {
        const res = await getAllUsers()
        if (res.success) {
          this.allUsers = res.data || []
        } else {
          this.$message.error(res.message || '获取用户列表失败')
          this.allUsers = []
        }
      } catch (error) {
        console.error('获取用户列表失败:', error)
        this.$message.error('获取用户列表失败')
        this.allUsers = []
      }
    },

    fillPlatformsCounts(counts = {}) {
      const result = {}
      this.allPlatforms.forEach(p => {
        result[p.code] = counts[p.code] || 0
      })
      return result
    },

    async fetchData() {
      const params = {
        start: this.startDate.toISOString().slice(0, 10) + 'T00:00:00',
        end: this.endDate.toISOString().slice(0, 10) + 'T23:59:59'
      }

      this.loading = true
      try {
        const [acRes, tryRes] = await Promise.all([fetchAcCounts(params), fetchTryCounts(params)])

        this.userStatsMap = {}

        // 首先初始化所有用户的数据
        this.allUsers.forEach(user => {
          this.userStatsMap[user.id] = {
            userId: user.id,
            username: user.username,
            realName: user.realName,
            acCount: 0,
            tryCount: 0,
            acCounts: this.fillPlatformsCounts(),
            tryCounts: this.fillPlatformsCounts(),
            tags: user.tags || []
          }
        })

        // 然后更新有解题记录的用户数据
        ;(acRes.data || []).forEach(u => {
          const acCount = Object.values(u.counts || {}).reduce((sum, val) => sum + (parseInt(val) || 0), 0)
          if (this.userStatsMap[u.userId]) {
            this.userStatsMap[u.userId].acCount = acCount
            this.userStatsMap[u.userId].acCounts = this.fillPlatformsCounts(u.counts)
          }
        })
        ;(tryRes.data || []).forEach(u => {
          const tryCount = Object.values(u.counts || {}).reduce((sum, val) => sum + (parseInt(val) || 0), 0)
          if (this.userStatsMap[u.userId]) {
            this.userStatsMap[u.userId].tryCount = tryCount
            this.userStatsMap[u.userId].tryCounts = this.fillPlatformsCounts(u.counts)
          }
        })

        this.list = Object.values(this.userStatsMap)
      } catch (e) {
        console.error('请求错误:', e)
      } finally {
        this.loading = false
      }
    },

    onRefresh() {
      this.fetchData()
    },

    exportExcel() {
      const header = ['序号', '用户名', '真实姓名', '总AC', '总尝试']
      this.displayPlatforms.forEach(p => {
        header.push(`${p.name} AC`, `${p.name} 尝试`)
      })

      const data = this.filteredList.map((row, idx) => {
        const base = [idx + 1, row.username, row.realName, row.acCount, row.tryCount]
        this.displayPlatforms.forEach(p => {
          base.push(row.acCounts[p.code] || 0, row.tryCounts[p.code] || 0)
        })
        return base
      })

      export_json_to_excel({
        header,
        data,
        filename: '用户题数统计',
        autoWidth: true,
        bookType: 'xlsx'
      })
    },

    resetSettings() {
      this.$store.dispatch('allPbInfo/resetSettings')
      this.selectedPlatforms = []
      this.selectedTags = []
      this.$message.success('设置已重置')
    },

    // 获取文本颜色（根据背景色）
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

    async fetchLastUpdateTime() {
      try {
        const res = await fetchLastUpdate()
        if (res && res.data && res.data.lastUpdate) {
          this.lastUpdate = new Date(res.data.lastUpdate).toLocaleString()
        }
      } catch (error) {
        this.lastUpdate = '获取失败'
      }
    },

    async manualUpdate() {
      this.updating = true
      try {
        const res = await manualRebuild()
        if (res && res.success) {
          this.$message.success(res.message)
          this.fetchData()
          this.fetchLastUpdateTime()
        } else if (res && !res.success && res.data && res.data.tokenValidation) {
          // 检测到token失效
          this.handleTokenExpired(res.data.tokenValidation)
        }
      } catch (e) {
        this.$message.error(e.message)
      } finally {
        this.updating = false
      }
    },

    handleTokenExpired(tokenValidation) {
      const invalidPlatforms = tokenValidation.platforms
        .filter(p => !p.valid)
        .map(p => `${p.platform}: ${p.message}`)
        .join('\n')

      this.$confirm(
        `检测到以下平台的认证已失效：\n\n${invalidPlatforms}\n\n是否继续强制更新？`,
        '认证失效警告',
        {
          confirmButtonText: '继续更新',
          cancelButtonText: '管理Token',
          type: 'warning',
          customClass: 'token-expired-dialog',
          beforeClose: (action, instance, done) => {
            if (action === 'confirm') {
              this.performForceUpdate()
            } else if (action === 'cancel') {
              this.openTokenManagement()
            }
            done()
          }
        }
      ).catch(() => {
        // 用户取消操作
      })
    },

    async performForceUpdate() {
      this.updating = true
      try {
        const res = await forceUpdateDB()
        if (res && res.success) {
          this.$message.success('强制更新已开始，这需要几分钟')
          this.fetchData()
          this.fetchLastUpdateTime()
        }
      } catch (e) {
        this.$message.error('强制更新失败: ' + e.message)
      } finally {
        this.updating = false
      }
    },

    openTokenManagement() {
      // 这里可以跳转到token管理页面或打开模态框
      this.$router.push('/token-management')
    },

    sortNumber(a, b, column) {
      // 对于 "总数(AC / 尝试)" 列，按 acCount 排序
      if (column && column.label === '总数(AC / 尝试)') {
        return (a.acCount || 0) - (b.acCount || 0)
      }
      // 对于平台列，按对应平台的AC数排序
      if (column && column.label && this.displayPlatforms.some(p => column.label === p.name)) {
        const platform = this.displayPlatforms.find(p => column.label === p.name)
        if (platform) {
          return (a.acCounts[platform.code] || 0) - (b.acCounts[platform.code] || 0)
        }
      }
      // 默认数字排序
      return (parseInt(a) || 0) - (parseInt(b) || 0)
    }
  }
}
</script>

<style scoped lang="scss">
.dashboard-ranking-container {
  padding: 20px;
  background: #f5f7fa;

  .update-info {
    font-size: 14px;
    color: #606266;
  }

  .time-label1 {
    font-weight: 600;
    line-height: 36px;
    margin-right: 6px;
    color: #606266;
    user-select: none;
  }

      .controls {
      margin-bottom: 16px;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;

      .search-input {
        width: 200px;
      }
    }

  .table-wrapper {
    width: 100%;
    overflow-x: auto; /* 超出宽度时显示横向滚动条 */
  }
}

/* Token过期对话框样式 */
::v-deep .token-expired-dialog {
  .el-message-box__message {
    white-space: pre-line;
    font-family: monospace;
    font-size: 13px;
    line-height: 1.6;
  }

  .el-message-box__btns {
    .el-button--primary {
      background-color: #e6a23c;
      border-color: #e6a23c;

      &:hover {
        background-color: #ebb563;
        border-color: #ebb563;
      }
    }
  }
}
</style>
