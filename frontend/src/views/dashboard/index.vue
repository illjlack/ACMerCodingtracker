<template>
  <div class="dashboard-container">
    <div class="dashboard-text">欢迎来到 ACMer 刷题追踪系统</div>

    <!-- 统计卡片 -->
    <el-row :gutter="10" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div slot="header" class="card-header">
            <span>今日刷题数</span>
          </div>
          <div class="card-body">
            <h2>{{ todayProblems }}</h2>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div slot="header" class="card-header">
            <span>本周刷题数</span>
          </div>
          <div class="card-body">
            <h2>{{ weekProblems }}</h2>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div slot="header" class="card-header">
            <span>本月刷题数</span>
          </div>
          <div class="card-body">
            <h2>{{ monthProblems }}</h2>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div slot="header" class="card-header">
            <span>总刷题数</span>
          </div>
          <div class="card-body">
            <h2>{{ totalProblems }}</h2>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 开发进度 -->
    <el-card class="progress-card">
      <div slot="header">
        <span>系统开发进度</span>
      </div>
      <div class="progress-list">
        <div v-for="(item, index) in progressItems" :key="index" class="progress-item">
          <el-tag :type="item.type" size="medium" class="progress-tag">{{ item.status }}</el-tag>
          <span class="progress-text">{{ item.content }}</span>
        </div>
      </div>
    </el-card>

    <!-- 支持平台 -->
    <el-card class="platform-card">
      <div slot="header">
        <span>支持平台</span>
      </div>
      <div class="platform-list">
        <el-tag
          v-for="platform in supportedPlatforms"
          :key="platform.name"
          :type="platform.type"
          effect="dark"
          class="platform-tag"
        >
          <i :class="platform.icon" />
          {{ platform.name }}
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'Dashboard',
  data() {
    return {
      todayProblems: 0,
      weekProblems: 0,
      monthProblems: 0,
      totalProblems: 0,
      progressItems: [
        {
          content: '系统基础功能开发完成',
          status: '已完成',
          type: 'success'
        },
        {
          content: '支持洛谷和 Codeforces 平台',
          status: '已完成',
          type: 'success'
        },
        {
          content: '题目管理功能开发中',
          status: '开发中',
          type: 'warning'
        },
        {
          content: '更多平台支持开发中',
          status: '计划中',
          type: 'info'
        }
      ],
      supportedPlatforms: [
        {
          name: '洛谷',
          type: 'success',
          icon: 'el-icon-s-platform'
        },
        {
          name: 'Codeforces',
          type: 'primary',
          icon: 'el-icon-s-platform'
        }
      ]
    }
  },
  created() {
    // TODO: 从后端获取数据
    // 这里先使用模拟数据
    this.todayProblems = 5
    this.weekProblems = 25
    this.monthProblems = 100
    this.totalProblems = 500
  },
  methods: {
    getStatusIcon(type) {
      const icons = {
        success: 'el-icon-check',
        primary: 'el-icon-check',
        warning: 'el-icon-loading',
        info: 'el-icon-more'
      }
      return icons[type] || 'el-icon-info'
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  margin: 20px;
}

.dashboard-text {
  font-size: 24px;
  line-height: 36px;
  margin-bottom: 20px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  .card-header {
    padding: 10px;
    font-size: 14px;
  }

  .card-body {
    padding: 10px;
    text-align: center;

    h2 {
      margin: 0;
      font-size: 20px;
      color: #409EFF;
    }
  }
}

.progress-card {
  margin-bottom: 20px;
}

.progress-list {
  padding: 10px 0;
}

.progress-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  padding: 8px;
  border-radius: 4px;
  background-color: #f5f7fa;

  &:last-child {
    margin-bottom: 0;
  }
}

.progress-tag {
  margin-right: 15px;
  min-width: 60px;
  text-align: center;
}

.progress-text {
  font-size: 14px;
  color: #303133;
}

.platform-card {
  margin-bottom: 20px;
}

.platform-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.platform-tag {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 15px;
  font-size: 14px;
}
</style>
