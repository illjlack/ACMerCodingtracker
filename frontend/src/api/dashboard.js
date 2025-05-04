import request from '@/utils/request'

/**
 * 获取排行榜数据
 * @param {Object} params - 查询参数，如分页、搜索关键词等
 * @returns Promise
 */
export function fetchDashboardRanking(params) {
  return request({
    url: '/dashboard/ranking',
    method: 'get',
    params
  })
}

/**
 * 刷新排行榜数据
 * @returns Promise
 */
export function refreshDashboardData() {
  return request({
    url: '/dashboard/ranking/refresh',
    method: 'post'
  })
}

/**
 * 获取用户每日做题统计
 * @param {Object} params - 包含 userId，如 { userId: 123 }
 * @returns {Promise}
 */
export function fetchDailyPbStats(params) {
  return request({
    url: '/dashboard/daily-stats',
    method: 'get',
    params
  })
}
