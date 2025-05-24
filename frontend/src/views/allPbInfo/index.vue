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

      <el-input
        v-model="search"
        placeholder="搜索用户名或真实姓名"
        clearable
        prefix-icon="el-icon-search"
        class="search-input"
        style="margin-right: 16px;"
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
        <el-table-column type="index" label="排名" min-width="60" />

        <el-table-column prop="username" label="用户名" min-width="120" />

        <el-table-column prop="realName" label="真实姓名" min-width="120" />

        <el-table-column label="总数(AC / 尝试)" sortable min-width="120">
          <template #default="{ row }">
            {{ row.acCount }} / {{ row.tryCount }}
          </template>
        </el-table-column>

        <el-table-column
          v-for="platform in platforms"
          :key="platform"
          :label="platform"
          min-width="100"
        >
          <template #default="{ row }">
            {{ (row.acCounts[platform] || 0) + ' / ' + (row.tryCounts[platform] || 0) }}
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import { fetchAcCounts, fetchTryCounts, fetchLastUpdate, manualRebuild } from '@/api/usertry'
import { export_json_to_excel } from '@/vendor/Export2Excel'

export default {
  name: 'UserStatsDashboard',
  data() {
    return {
      startDate: new Date(2021, 0, 1), // 2021年1月1日
      endDate: new Date(), // 当前时间
      search: '',
      loading: false,
      userStatsMap: {},
      list: [],
      platforms: ['CODEFORCES', 'HDU', 'POJ', 'LUOGU'],
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
    filteredList() {
      const kw = this.search.trim().toLowerCase()
      if (!kw) return this.list
      return this.list.filter(
        item =>
          (item.username && item.username.toLowerCase().includes(kw)) ||
          (item.realName && item.realName.toLowerCase().includes(kw))
      )
    }
  },
  watch: {
    startDate(newVal, oldVal) {
      if (newVal !== oldVal) this.fetchData()
    },
    endDate(newVal, oldVal) {
      if (newVal !== oldVal) this.fetchData()
    }
  },
  created() {
    this.fetchData()
    this.fetchLastUpdateTime()
  },
  methods: {
    fillPlatformsCounts(counts = {}) {
      const result = {}
      this.platforms.forEach(p => {
        result[p] = counts[p] || 0
      })
      return result
    },

    async fetchData() {
      if (!this.startDate || !this.endDate) return

      const params = {
        start: this.startDate.toISOString().slice(0, 10) + 'T00:00:00',
        end: this.endDate.toISOString().slice(0, 10) + 'T23:59:59'
      }

      this.loading = true
      try {
        const [acRes, tryRes] = await Promise.all([
          fetchAcCounts(params),
          fetchTryCounts(params)
        ])

        this.userStatsMap = {}

        ;(acRes.data || []).forEach(u => {
          const acCount = Object.values(u.counts || {}).reduce((sum, val) => sum + val, 0)
          this.userStatsMap[u.userId] = {
            userId: u.userId,
            username: u.username,
            realName: u.realName,
            acCount,
            tryCount: 0,
            acCounts: this.fillPlatformsCounts(u.counts),
            tryCounts: this.fillPlatformsCounts()
          }
        })
        ;(tryRes.data || []).forEach(u => {
          const tryCount = Object.values(u.counts || {}).reduce((sum, val) => sum + val, 0)
          if (this.userStatsMap[u.userId]) {
            this.userStatsMap[u.userId].tryCount = tryCount
            this.userStatsMap[u.userId].tryCounts = this.fillPlatformsCounts(u.counts)
          } else {
            this.userStatsMap[u.userId] = {
              userId: u.userId,
              username: u.username,
              realName: u.realName,
              acCount: 0,
              tryCount,
              acCounts: this.fillPlatformsCounts(),
              tryCounts: this.fillPlatformsCounts(u.counts)
            }
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
      const header = ['排名', '用户名', '真实姓名', '总AC', '总尝试']
      this.platforms.forEach(p => {
        header.push(`${p} AC`, `${p} 尝试`)
      })

      const data = this.filteredList.map((row, idx) => {
        const base = [idx + 1, row.username, row.realName, row.acCount, row.tryCount]
        this.platforms.forEach(p => {
          base.push(row.acCounts[p], row.tryCounts[p])
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
        await manualRebuild()
        this.$message.success('手动更新成功')
        await this.fetchLastUpdateTime()
        this.fetchData()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '手动更新失败')
      } finally {
        this.updating = false
      }
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

    .search-input {
      width: 200px;
      margin-left: auto;
    }
  }

  .table-wrapper {
    width: 100%;
    overflow-x: auto; /* 超出宽度时显示横向滚动条 */
  }
}
</style>
