<template>
  <div ref="chartContainer" class="chart-container" />
</template>

<script>
import echarts from 'echarts'
import 'echarts/theme/macarons'
import resize from './mixins/resize'
import { fetchDailyPbStats } from '@/api/usertry'

export default {
  name: 'MixChart',
  mixins: [resize],
  props: {
    // 接收来自父组件的 name 作为 props
    name: {
      type: String,
      required: true
    }
  },
  watch: {
    name: {
      immediate: true,
      handler(newName) {
        if (this.chart) {
          this.loadDailyPbStats(newName)
        }
      }
    }
  },
  mounted() {
    this.chart = echarts.init(this.$refs.chartContainer, 'macarons')
    this.loadDailyPbStats(this.name) // 初始化完成后再加载一次
  },
  methods: {
    async loadDailyPbStats(name) {
      if (!this.chart) {
        console.warn('chart 未初始化，跳过加载')
        return
      }

      this.chart.clear()
      try {
        console.log(`正在加载用户: ${name} 的数据...`)
        const res = await fetchDailyPbStats({ username: name })

        console.log('返回的原始数据:', res)

        const stats = res.data
        const dates = stats.map(item => item.date)
        const acData = stats.map(item => item.acCount)

        this.chart.setOption({
          backgroundColor: '#344b58',
          title: {
            text: `${name} 的每日做题情况`,
            left: 'center',
            textStyle: { color: '#fff' }
          },
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }},
          legend: {
            data: ['AC数'],
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
              itemStyle: { color: 'rgba(0,255,0,1)' } // 绿色
            }
          ]
        })
      } catch (err) {
        console.error('加载每日做题数据失败', err)
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
