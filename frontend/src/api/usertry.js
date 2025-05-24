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

export function fetchUserProblemDetails(params) {
  return request({
    url: '/dashboard/user/problems',
    method: 'get',
    params
  })
}

/**
 * 获取AC题数统计
 * @param {Object} params - 查询参数，如 { start: '2021-05-01T00:00:00', end: '2025-05-24T23:59:59' }
 * @returns {Promise}
 */
export function fetchAcCounts(params) {
  return request({
    url: '/api/usertry/stats/ac-counts',
    method: 'get',
    params
  })
}

/**
 * 获取尝试题数统计
 * @param {Object} params - 查询参数，如 { start: '2021-05-01T00:00:00', end: '2025-05-24T23:59:59' }
 * @returns {Promise}
 */
export function fetchTryCounts(params) {
  return request({
    url: '/api/usertry/stats/try-counts',
    method: 'get',
    params
  })
}

/**
 * 手动触发重新爬取（重建数据）
 * @returns {Promise}
 */
export function manualRebuild() {
  return request({
    url: '/api/usertry/stats/rebuild',
    method: 'post'
  })
}

/**
 * 获取上次爬虫数据更新时间
 * @returns {Promise}
 */
export function fetchLastUpdate() {
  return request({
    url: '/api/usertry/stats/last-update',
    method: 'get'
  })
}

