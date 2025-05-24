<template>
  <div :class="className" :style="{ height: height, width: width }" />
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons') // 使用 echarts 的 macarons 主题
import resize from './mixins/resize'

export default {
  mixins: [resize],
  props: {
    className: {
      type: String,
      default: 'chart'
    },
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '300px'
    }
  },
  data() {
    return {
      chart: null,
      ojData: { // 使用虚拟数据

        poj: 33,
        luogu: 90,
        codeforces: 153
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initChart()
      this.updateChartData() // 使用虚拟数据初始化图表
    })
  },
  beforeDestroy() {
    if (!this.chart) {
      return
    }
    this.chart.dispose()
    this.chart = null
  },
  methods: {
    // 更新图表数据
    updateChartData() {
      if (!this.chart || !this.ojData) return

      // 计算总数，用于计算百分比
      const total = Object.values(this.ojData).reduce((sum, value) => sum + value, 0)

      this.chart.setOption({
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            // 显示名称、数量和百分比
            return `${params.name} : ${params.value} (${((params.value / total) * 100).toFixed(2)}%)`
          }
        },
        legend: {
          left: 'center',
          bottom: '10',
          data: Object.keys(this.ojData), // 动态显示OJ平台名称
          textStyle: {
            color: '#333' // 设置图例字体颜色
          }
        },
        series: [
          {
            name: '通过题目数量',
            type: 'pie',
            roseType: 'radius',
            radius: [15, 95],
            center: ['50%', '38%'],
            data: Object.keys(this.ojData).map(oj => ({
              value: this.ojData[oj], // 获取每个平台的总通过题目数量
              name: oj.charAt(0).toUpperCase() + oj.slice(1) // Capitalize first letter
            })),
            itemStyle: {
              // 设置每个分区的颜色
              color: function(params) {
                const colorList = ['#36CFC9', '#B37FEB', '#5B8FF9', '#FF9F5B']
                return colorList[params.dataIndex] // 每个分区的颜色
              }
            },
            animationEasing: 'cubicInOut',
            animationDuration: 2600
          }
        ]
      })
    },

    initChart() {
      this.chart = echarts.init(this.$el, 'macarons')
      this.onResize()
    },

    onResize() {
      if (this.chart) {
        this.chart.resize()
      }
    }
  }
}
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: calc(100vh - 84px);
}
</style>
