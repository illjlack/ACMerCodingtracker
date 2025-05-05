<template>
  <div class="dashboard-editor-container">
    <h1>{{ name }} 的做题信息</h1>
    <!-- Line Chart Section -->
    <el-row style="background:#fff;padding:16px 16px 0;margin-bottom:32px;">
      <line-chart :name="name" />
    </el-row>

    <!-- Contribution Calendar Section -->
    <el-row :gutter="32">
      <el-col :xs="24" :sm="24" :lg="12">
        <div class="contribution-calendar-wrapper">
          <contribution-calendar :name="name" />
        </div>
      </el-col>

      <el-col :xs="24" :sm="24" :lg="12">
        <div class="chart-wrapper">
          <pie-chart />
        </div>
      </el-col>
    </el-row>

    <!--    &lt;!&ndash; Pie Chart Section &ndash;&gt;-->
    <!--    <el-row :gutter="32">-->
    <!--      <el-col :xs="24" :sm="24" :lg="12">-->
    <!--        <div class="chart-wrapper">-->
    <!--          <pie-chart />-->
    <!--        </div>-->
    <!--      </el-col>-->
    <!--    </el-row>-->
  </div>
</template>

<script>
import LineChart from './components/LineChart.vue'
import PieChart from './components/PieChart.vue'
import ContributionCalendar from './components/contribution-calendar.vue'

export default {
  name: 'DashboardAdmin',
  components: {
    LineChart,
    PieChart,
    ContributionCalendar
  },
  props: {
    name: {
      type: String,
      required: true
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-editor-container {
  padding: 32px;
  background-color: rgb(240, 242, 245);
  position: relative;

  .github-corner {
    position: absolute;
    top: 0px;
    border: 0;
    right: 0;
  }

  .chart-wrapper {
    background: #fff;
    padding: 16px 16px 0;
    margin-bottom: 32px;
  }
}

@media (max-width:1024px) {
  .chart-wrapper {
    padding: 8px;
  }
}
</style>
