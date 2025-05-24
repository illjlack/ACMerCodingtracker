<template>
  <div>
    <contribution-calendar :data="history" />
  </div>
</template>

<script>
import ContributionCalendar from 'vue-contribution-calendar'
import { fetchDailyPbStats } from '@/api/usertry'

export default {
  components: {
    ContributionCalendar
  },
  props: {
    name: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      history: [] // 存储贡献数据
    }
  },
  watch: {
    // 当 name 变化时，重新加载数据
    name: {
      immediate: true,
      handler(newName) {
        this.loadContributionData(newName)
      }
    }
  },
  mounted() {
    this.loadContributionData(this.name) // 初始化完成后再加载一次
  },
  methods: {
    // 加载贡献数据并格式化
    async loadContributionData(name) {
      try {
        console.log('正在加载用户:', name)

        // 请求贡献数据
        const res = await fetchDailyPbStats({ username: name })
        const stats = res.data

        // 如果返回的数据为空，直接返回
        if (!stats || stats.length === 0) {
          console.log('没有找到贡献数据')
          return
        }

        console.log('返回的贡献数据:', stats)

        // 创建一个新的非响应式数据对象
        // const formattedData = []
        //
        // for (let i = 0; i < stats.length; i++) {
        //   const item = stats[i]
        //   console.log('item:', item)
        //   // 创建普通对象而不是响应式对象
        //   const newItem = Object.freeze({
        //     date: item.date, // 保持原日期
        //     count: item.acCount // 将 AC 数量作为贡献数
        //   })
        //
        //   formattedData.push(newItem)
        // }

        this.history = {
          '01-05-2025': 5,
          '02-05-2025': 3,
          '03-05-2025': 7,
          '04-05-2025': 5,
          '05-05-2025': 5,
          '06-05-2025': 7
        }
        console.log('格式化后的贡献数据:', this.history)
      } catch (err) {
        console.error('加载贡献数据失败', err)
      }
    }
  }
}
</script>

<style scoped>
/* 你可以根据需要添加样式 */
</style>

<!--<template>-->
<!--  <div>-->
<!--    <div>-->
<!--      <h2>{{ name }} 的贡献日历</h2>-->
<!--      &lt;!&ndash; 使用自定义的 contribution-chart 组件渲染贡献数据 &ndash;&gt;-->
<!--      <contribution-chart :year="year" :start-date="startDate" :end-date="endDate" :data-sources="contributionData" :description="'贡献数据展示'" :show-footer="true" />-->
<!--    </div>-->
<!--  </div>-->
<!--</template>-->

<!--<script>-->
<!--import ContributionChart from './ContributionChart/index.vue' // 请根据路径调整-->
<!--import { fetchDailyPbStats } from '@/api/dashboard'-->

<!--export default {-->
<!--  components: {-->
<!--    ContributionChart-->
<!--  },-->
<!--  props: {-->
<!--    name: {-->
<!--      type: String,-->
<!--      required: true-->
<!--    }-->
<!--  },-->
<!--  data() {-->
<!--    return {-->
<!--      contributionData: [], // 存储贡献数据-->
<!--      year: new Date().getFullYear(), // 当前年份-->
<!--      startDate: '2023-01-01', // 开始日期-->
<!--      endDate: '2023-12-31' // 结束日期-->
<!--    }-->
<!--  },-->
<!--  watch: {-->
<!--    name: {-->
<!--      immediate: true,-->
<!--      handler(newName) {-->
<!--        this.loadContributionData(newName)-->
<!--      }-->
<!--    }-->
<!--  },-->
<!--  mounted() {-->
<!--    this.loadContributionData(this.name)-->
<!--  },-->
<!--  methods: {-->
<!--    // 加载贡献数据并格式化-->
<!--    async loadContributionData(name) {-->
<!--      try {-->
<!--        console.log('正在加载用户:', name)-->

<!--        // 请求贡献数据-->
<!--        const res = await fetchDailyPbStats({ username: name })-->
<!--        const stats = res.data-->

<!--        // 如果返回的数据为空，直接返回-->
<!--        if (!stats || stats.length === 0) {-->
<!--          console.log('没有找到贡献数据')-->
<!--          return-->
<!--        }-->

<!--        console.log('返回的贡献数据:', stats)-->

<!--        // 格式化贡献数据-->
<!--        this.contributionData = stats.map(item => ({-->
<!--          date: item.date, // 日期-->
<!--          count: item.acCount + item.waCount // 贡献数 = AC数 + WA数-->
<!--        }))-->

<!--        console.log('格式化后的贡献数据:', this.contributionData)-->
<!--      } catch (err) {-->
<!--        console.error('加载贡献数据失败', err)-->
<!--      }-->
<!--    }-->
<!--  }-->
<!--}-->
<!--</script>-->

<!--<style scoped>-->
<!--/* 根据需要添加样式 */-->
<!--</style>-->
