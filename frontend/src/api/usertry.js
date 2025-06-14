import request from '@/utils/request'
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

/**
 * 获取用户尝试记录分页列表
 * @param {string} username 用户名
 * @param {number} page    0-based 页码
 * @param {number} size    每页条数
 * @returns Promise<AxiosResponse>
 */
export function listUserTries(username, page = 0, size = 10) {
  return request(`/api/usertry/list/${username}`, {
    params: { page, size }
  })
}

export function fetchUserTryProblems(params) {
  return request({
    url: '/api/usertry/list/' + params.username,
    method: 'get',
    params: {
      page: params.page || 0,
      size: params.size || 20
    }
  })
}

export function forceRebuild() {
  return request({
    url: '/api/usertry/stats/force-rebuild',
    method: 'post'
  })
}

export function getUpdateStatus() {
  return request({
    url: '/api/usertry/stats/status',
    method: 'get'
  })
}

// 新增的token验证和管理API
export function validateAllTokens() {
  return request({
    url: '/api/admin/tokens/validate-all',
    method: 'post'
  })
}

export function forceUpdateDB() {
  return request({
    url: '/api/usertry/updatedb/force',
    method: 'post'
  })
}

export function getTokenStatus() {
  return request({
    url: '/api/admin/tokens/status',
    method: 'get'
  })
}

export function updateToken(platform, token) {
  return request({
    url: `/api/admin/tokens/${platform}`,
    method: 'put',
    data: { token }
  })
}

export function validatePlatformToken(platform) {
  return request({
    url: `/api/admin/tokens/${platform}/validate`,
    method: 'post'
  })
}

export function deleteToken(platform) {
  return request({
    url: `/api/admin/tokens/${platform}`,
    method: 'delete'
  })
}

export function getPlatformInfo(platform) {
  return request({
    url: `/api/admin/tokens/${platform}/info`,
    method: 'get'
  })
}

export function validateTokenFormat(platform, token) {
  return request({
    url: `/api/admin/tokens/${platform}/validate-format`,
    method: 'post',
    data: { token }
  })
}
