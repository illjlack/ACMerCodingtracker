
// mock/usertry.js
const Mock = require('mockjs')

// 生成假数据列表：各平台题数随机，再计算总题数与评分（与总题数正相关），每三人一队
const List = []
for (let i = 1; i <= 32; i++) {
  // 各平台做题数
  const hdu = Mock.mock('@integer(0,200)')
  const poj = Mock.mock('@integer(0,200)')
  const luogu = Mock.mock('@integer(0,200)')
  const cf = Mock.mock('@integer(0,200)')
  // 总题数为各平台做题数之和
  const total = hdu + poj + luogu + cf
  // 评分与总题数正相关，设置在 [total, total*2] 范围内
  const rating = Mock.mock(`@integer(${total}, ${total * 2})`)
  // 队伍名称：每三人分为一队
  const teamIndex = Math.ceil(i / 3)
  List.push({
    id: i,
    name: Mock.mock('@cname'),
    team: `队伍${teamIndex}`,
    clazz: `班级${Mock.mock('@integer(1,5)')}`,
    total,
    hdu,
    poj,
    luogu,
    cf,
    rating
  })
}

const problems = []
for (let i = 0; i < 100; i++) {
  problems.push({
    problemId: Mock.mock('@integer(1000, 9999)'),
    oj: Mock.mock('@pick(["HDU", "POJ", "Luogu", "Codeforces"])'),
    submitTime: Mock.mock('@datetime'),
    result: Mock.mock('@pick(["AC", "WA"])')
  })
}

module.exports = [
  {
    url: '/dashboard/ranking',
    type: 'get',
    // 获取排行榜，支持 search 参数过滤
    response: config => {
      const { search = '' } = config.query
      const filtered = List.filter(item =>
        item.name.includes(search) ||
        item.clazz.includes(search) ||
        item.team.includes(search)
      )
      return {
        code: 20000,
        data: filtered
      }
    }
  },
  {
    url: '/dashboard/ranking/refresh',
    type: 'post',
    // 刷新数据
    response: () => ({
      code: 20000,
      message: '刷新成功'
    })
  },
  {
    url: '/dashboard/daily-stats',
    type: 'get',
    // 获取指定用户的每日做题统计
    response: config => {
      // eslint-disable-next-line no-unused-vars
      const { username } = config.query // 这里改成 username

      // 生成过去300天的假数据
      const stats = []
      for (let i = 300; i >= 0; i--) {
        const d = new Date()
        d.setDate(d.getDate() - i)
        const dateStr = d.toISOString().slice(0, 10)

        stats.push({
          date: dateStr,
          acCount: Mock.mock('@integer(0, 5)'), // AC数量
          waCount: Mock.mock('@integer(0, 5)') // WA数量
        })
      }

      return {
        code: 20000,
        data: stats
      }
    }
  },
  {
    url: '/dashboard/user/problems',
    type: 'get',
    response: config => {
      const { username } = config.query
      const userProblems = problems.filter(item => item.oj === username) // 模拟只返回用户特定数据

      return {
        code: 20000,
        data: userProblems
      }
    }
  }
]
