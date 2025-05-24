<template>
  <div class="contribution-chart">
    <table>
      <thead class="thead">
        <tr>
          <th id="first-block" />
          <!-- 遍历 months，显示每个月的名称，并设置 colspan 来展示每个月的列数 -->
          <th v-for="item in months" :key="item.month" :colspan="item.colspan" class="label">
            <span>
              {{ monthNames[item.month - 1] }} <!-- 显示月份名称 -->
            </span>
          </th>
        </tr>
      </thead>
      <tbody class="tbody">
        <!-- 遍历日期数据 rows -->
        <tr v-for="(row, idx) in dates" :key="idx">
          <td class="label" :style="{ width: '30px' }">
            <!-- 只显示 星期 1，3，5 ，你也可以自行修改 -->
            <span v-if="idx % 2 !== 0">{{ weekNames[idx] }}</span>
          </td>
          <!-- 显示日期，根据是否隐藏来判断日期的背景色 -->
          <td v-for="date in row" :key="date.full" :data-date="date.full" :class="{ block: true, hidden: date.ignore }" :style="{ backgroundColor: dateBackgroundColor(date.full) }" />
        </tr>
      </tbody>
    </table>
    <div v-if="showFooter" class="tfoot">
      <div class="description">{{ description }}</div>
      <div v-show="colors?.length" class="colors">
        <span class="less-text">{{ lessText }}</span>
        <!-- 显示颜色块 -->
        <span v-for="color in colors" :key="color" class="color-item" :style="[{ backgroundColor: color, marginLeft: '4px' }]" />
        <span class="more-text">{{ moreText }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    year: { // 传入的年份，默认为 undefined
      type: Number,
      default: undefined
    },
    startDate: { // 开始日期，可以是字符串、数字或日期类型
      type: [String, Number, Date],
      default: undefined
    },
    endDate: { // 结束日期
      type: [String, Number, Date],
      default: undefined
    },
    dataSources: { // 数据源，默认为空对象
      type: Object,
      default: () => ({})
    },
    description: { // 描述信息，默认为空字符串
      type: String,
      default: ''
    },
    showFooter: { // 是否显示底部，默认为 false
      type: Boolean,
      default: false
    },
    colors: { // 颜色数组，用于不同日期的背景色
      type: Array,
      default: () => ['#ebedf0', '#87b5f1', '#1e80ff', '#005dd6']
    },
    lessText: { // 显示“Less”的文本，默认为 Less
      type: String,
      default: 'Less'
    },
    moreText: { // 显示“More”的文本，默认为 More
      type: String,
      default: 'More'
    }
  },
  data() {
    return {
      months: [], // 存储月份数据
      dates: [], // 存储日期数据
      weekNames: ['Sun', 'Mon', 'Tues', 'Wed', 'Thur', 'Fri', 'Sat'], // 星期名称
      monthNames: [ // 月份名称
        'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
      ]
    }
  },
  created() {
    // 在组件创建时调用 generateChartData，生成图表数据
    this.generateChartData(this.startDate, this.endDate)
  },
  methods: {
    // 用于将数字格式化为固定长度，前面补零
    fullZero(num, len = 2) {
      return `${num}`.padStart(len, '0')
    },

    // 格式化日期，类似 Java 的 SimpleDateFormat
    format(timestamp, format = 'yyyy-MM-dd') {
      const date = new Date(timestamp) // 将时间戳转为 Date 对象
      const year = date.getFullYear()
      const month = date.getMonth() + 1
      const day = date.getDate()
      const hour = date.getHours()
      const minutes = date.getMinutes()
      const seconds = date.getSeconds()
      return format
        .replace('yyyy', `${year}`)
        .replace('MM', `${this.fullZero(month)}`)
        .replace('dd', `${this.fullZero(day)}`)
        .replace('hh', `${this.fullZero(hour)}`)
        .replace('mm', `${this.fullZero(minutes)}`)
        .replace('ss', `${this.fullZero(seconds)}`)
    },

    // 获取日期信息（年、月、日、星期几）
    getDateInfo(date) {
      const cur = new Date(date)
      return {
        year: cur.getFullYear(),
        month: cur.getMonth() + 1,
        date: cur.getDate(),
        day: cur.getDay(),
        full: this.format(cur) // 格式化日期
      }
    },

    // 获取起始日期，计算52周之前的日期
    getStartDate(end) {
      const ONE_DAY_TIMESTAMP = 86400000
      const cols = 52
      const endDate = new Date(end)
      const day = endDate.getDay()
      const endTimeStamp = endDate.getTime() - ONE_DAY_TIMESTAMP * day
      const diff = ONE_DAY_TIMESTAMP * cols * 7
      return new Date(endTimeStamp - diff) // 返回52周前的起始日期
    },

    // 获取日期项，将日期信息按星期分配到行
    getDateItems(start, end) {
      const ONE_DAY_TIMESTAMP = 86400000
      const startDate = new Date(start)
      const day = startDate.getDay()
      const rows = [[], [], [], [], [], [], []] // 用于存储不同星期的数据
      for (let i = 0; i < day; i++) {
        const dateInfo = this.getDateInfo(startDate.getTime() - (day - i) * ONE_DAY_TIMESTAMP)
        rows[dateInfo.day].push({
          ignore: true,
          ...dateInfo
        })
      }
      const endDate = new Date(end)
      for (let current = startDate.getTime(); current <= endDate.getTime(); current += ONE_DAY_TIMESTAMP) {
        const dateInfo = this.getDateInfo(current)
        rows[dateInfo.day].push(dateInfo) // 将日期按星期添加到对应的行
      }

      return rows
    },

    // 获取月份信息
    getMonthItems(rows) {
      const result = new Map() // 存储每个月份的信息
      const firstRow = rows[0]
      const lastRow = rows[rows.length - 1]
      firstRow.forEach((item, index) => {
        let curItem = item
        // 如果存在跨年情况，合并上一年的数据
        if (index === 0 && item.month !== lastRow[0].month) {
          curItem = lastRow[0]
        }
        const key = `${curItem.year}-${curItem.month}`
        const target = result.get(key) || { colspan: 0, month: curItem.month, year: curItem.year }
        target.colspan++ // 为该月的跨度添加值
        result.set(key, target) // 存储月份信息
      })
      return [...result.values()]
    },

    // 生成图表数据，根据输入的日期或年份生成相应的日期和月份数据
    generateChartData(...args) {
      let start
      let end
      if (args.length === 1) {
        const [year] = args
        start = this.format(`${year}-01-01`)
        end = this.format(`${year}-12-31`)
      } else if (args.length === 2) {
        [start, end] = [this.format(args[0]), this.format(args[1])]
      } else {
        end = this.format(Date.now())
        start = this.format(this.getStartDate(end))
      }
      const dates = this.getDateItems(start, end) // 获取日期项
      this.months = this.getMonthItems(dates) // 获取月份信息
      this.dates = dates // 存储日期信息
    }
  }
}
</script>

<style scoped lang="less">
.contribution-chart {
  width: max-content;
  table {
    border-spacing: 3px;
  }
  th,
  td {
    box-sizing: border-box;
    background-color: transparent;
    width: 10px;
    height: 10px;
    box-sizing: border-box;
    line-height: 1;
    padding: 0;
    font-size: 12px;
  }
  th {
    text-align: left;
    color: #333;
    padding-bottom: 6px;
  }
  tr {
    font-weight: 300;
    height: 10px;
  }
  #first-block {
    width: 28px;
  }
  .block {
    border-radius: 2px;
    background-color: #efefef;
    &.hidden {
      background-color: transparent !important;
    }
  }
  .label {
    position: relative;
  }
  .tbody {
    .label span {
      position: absolute;
      top: 50%;
      transform: translate(0%, -50%);
    }
  }

  .tfoot {
    margin-top: 10px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    color: #333;
    font-size: 12px;
    color: #999;
    .colors {
      display: flex;
      align-items: center;
      .color-item {
        width: 10px;
        height: 10px;
        display: inline-block;
      }
      .color-item + .color-item {
        margin-left: 4px;
      }
      .more-text {
        margin-left: 4px;
      }
    }
  }
}
</style>
