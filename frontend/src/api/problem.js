import request from '@/utils/request'

/**
 * 获取所有题目信息
 */
export function getAllPbInfo(params) {
  return request({
    url: '/api/problems/all',
    method: 'get',
    params
  })
}

/**
 * 根据ID获取题目信息
 */
export function getProblemById(id) {
  return request({
    url: `/api/problems/${id}`,
    method: 'get'
  })
}

/**
 * 更新题目标签
 */
export function updateProblemTags(problemId, data) {
  return request({
    url: `/api/problems/${problemId}/tags`,
    method: 'put',
    data
  })
}

/**
 * 批量更新题目信息
 */
export function batchUpdateProblems(data) {
  return request({
    url: '/api/problems/batch-update',
    method: 'put',
    data
  })
}

/**
 * 按平台获取题目信息
 */
export function getProblemsByPlatform(platform, params) {
  return request({
    url: `/api/problems/platform/${platform}`,
    method: 'get',
    params
  })
}

/**
 * 搜索题目
 */
export function searchProblems(params) {
  return request({
    url: '/api/problems/search',
    method: 'get',
    params
  })
}

/**
 * 获取题目统计信息
 */
export function getProblemStats() {
  return request({
    url: '/api/problems/stats',
    method: 'get'
  })
}
