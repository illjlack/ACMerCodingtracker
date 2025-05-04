<template>
  <div ref="chartContainer" class="chart-container" />
</template>

<script>
import echarts from 'echarts'
import 'echarts/theme/macarons'
import resize from './mixins/resize'
import { fetchDailyPbStats } from '@/api/dashboard'

export default {
  name: 'MixChart',
  mixins: [resize],
  data() {
    return {
      chart: null,
      // 默认从路由参数获取 username
      username: this.$route.params.username
    }
  },
  watch: {
    // 监听路由 username 变化，重新加载数据
    '$route.params.username': {
      immediate: true,
      handler(newName) {
        this.username = newName
        this.loadDailyPbStats()
      }
    }
  },
  mounted() {
    this.chart = echarts.init(this.$refs.chartContainer, 'macarons')
    this.onResize()
  },
  methods: {
    async loadDailyPbStats() {
      this.chart.clear()
      try {
        // 传递 username 参数
        const res = await fetchDailyPbStats({ username: this.username })
        const stats = res.data // [{ date, acCount, waCount }, ...]
        const dates = stats.map(item => item.date)
        const acData = stats.map(item => item.acCount)
        const waData = stats.map(item => item.waCount)

        this.chart.setOption({
          backgroundColor: '#344b58',
          title: {
            text: `${this.username} 的每日做题情况`,
            left: 'center',
            textStyle: { color: '#fff' }
          },
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }},
          legend: {
            data: ['AC数', 'WA数'],
            top: 20,
            textStyle: { color: '#90979c' }
          },
          grid: {
            left: '5%',
            right: '5%',
            bottom: 95,
            top: 60,
            containLabel: true
          },
          xAxis: {
            type: 'category',
            data: dates,
            axisLine: { lineStyle: { color: '#90979c' }},
            axisTick: { show: false }
          },
          yAxis: {
            type: 'value',
            axisLine: { lineStyle: { color: '#90979c' }},
            splitLine: { lineStyle: { color: '#3b4a59' }}
          },
          dataZoom: [
            {
              show: true,
              height: 30,
              xAxisIndex: [0],
              bottom: 20,
              start: 10,
              end: 80,
              handleIcon:
                'path://M306.1,413c0,2.2-1.8,4-4,4h-59.8c-2.2,0-4-1.8-4-4V200.8c0-2.2,1.8-4,4-4h59.8c2.2,0,4,1.8,4,4V413z',
              handleSize: '110%',
              handleStyle: { color: '#d3dee5' },
              textStyle: { color: '#fff' },
              borderColor: '#90979c'
            },
            {
              type: 'inside',
              show: true,
              height: 15,
              start: 10,
              end: 80
            }
          ],
          series: [
            {
              name: 'AC数',
              type: 'bar',
              stack: '总量',
              barMaxWidth: 35,
              data: acData,
              itemStyle: { color: 'rgba(255,144,128,1)' }
            },
            {
              name: 'WA数',
              type: 'bar',
              stack: '总量',
              barMaxWidth: 35,
              data: waData,
              itemStyle: { color: 'rgba(0,191,183,1)' }
            }
          ]
        })
      } catch (err) {
        console.error('加载每日做题数据失败', err)
      }
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
