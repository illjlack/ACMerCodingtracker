<template>
  <div class="dashboard-ranking-container">
    <!-- 操作按钮 & 搜索框 区域 -->
    <div class="controls">
      <el-button type="primary" icon="el-icon-refresh" @click="onRefresh">
        更新数据
      </el-button>
      <el-button type="success" icon="el-icon-document" @click="exportExcel">
        导出表格
      </el-button>
      <el-input
        v-model="search"
        placeholder="Search"
        clearable
        prefix-icon="el-icon-search"
        class="search-input"
      />
    </div>

    <!-- 排行榜数据表格 -->
    <el-table
      v-loading="listLoading"
      :data="filteredList"
      border
      stripe
      highlight-current-row
      style="width: 100%;"
    >
      <!-- 排名列 -->
      <el-table-column type="index" label="排名" width="80" />

      <!-- 姓名列，可跳转到用户主页 -->
      <el-table-column prop="name" label="姓名" sortable>
        <template #default="{ row }">
          <router-link class="link" :to="`/userPbInfo/${row.name}`">{{ row.name }}</router-link>
        </template>
      </el-table-column>

      <!-- 所属队伍列，可跳转到队伍主页 -->
      <el-table-column prop="team" label="所属队伍" sortable>
        <template #default="{ row }">
          <router-link class="link" :to="`/teamPbInfo/${row.team}`">{{ row.team }}</router-link>
        </template>
      </el-table-column>

      <!-- 班级列 -->
      <el-table-column prop="clazz" label="班级" sortable />
      <!-- 总题数列 -->
      <el-table-column prop="total" label="总题数" sortable />
      <!-- HDU 列 -->
      <el-table-column prop="hdu" label="HDU" sortable />
      <!-- POJ 列 -->
      <el-table-column prop="poj" label="POJ" sortable />
      <!-- LUOGU 列 -->
      <el-table-column prop="luogu" label="LUOGU" sortable />
      <!-- CF 列 -->
      <el-table-column prop="cf" label="CF" sortable />
      <!-- Rating 列 -->
      <el-table-column prop="rating" label="Rating(评分)" sortable />
    </el-table>
  </div>
</template>

<script>
import { fetchDashboardRanking, refreshDashboardData } from '@/api/dashboard'
import { export_json_to_excel } from '@/vendor/Export2Excel'

export default {
  name: 'DashboardRanking',
  data() {
    return {
      list: [],
      listLoading: false,
      search: ''
    }
  },
  computed: {
    filteredList() {
      const kw = this.search.toLowerCase()
      return this.search
        ? this.list.filter(item =>
          item.name.toLowerCase().includes(kw) ||
          item.team.toLowerCase().includes(kw) ||
          (item.clazz + '').toLowerCase().includes(kw)
        )
        : this.list
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      this.listLoading = true
      fetchDashboardRanking({ search: this.search })
        .then(res => {
          this.list = res.data
        })
        .finally(() => {
          this.listLoading = false
        })
    },
    onRefresh() {
      this.listLoading = true
      refreshDashboardData()
        .then(() => this.fetchData())
        .finally(() => {
          this.listLoading = false
        })
    },
    exportExcel() {
      const header = ['排名', '姓名', '所属队伍', '班级', '总题数', 'HDU', 'POJ', 'LUOGU', 'CF', 'Rating']
      const data = this.filteredList.map((row, idx) => [
        idx + 1,
        row.name,
        row.team,
        row.clazz,
        row.total,
        row.hdu,
        row.poj,
        row.luogu,
        row.cf,
        row.rating
      ])
      export_json_to_excel({ header, data, filename: '队员做题统计', autoWidth: true, bookType: 'xlsx' })
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-ranking-container {
  padding: 20px;
  background: #f5f7fa;
  .controls {
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    .search-input {
      margin-left: auto;
      width: 200px;
    }
  }
  /* 自定义链接颜色 */
  .link {
    color: #409EFF;
    text-decoration: none;
  }
  .link:hover {
    color: #66b1ff;
    text-decoration: underline;
  }
}
</style>
