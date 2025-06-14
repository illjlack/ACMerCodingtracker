import request from '@/utils/request'

/**
 * 获取所有标签
 */
export function getAllTags() {
  return request({
    url: '/api/tags',
    method: 'get'
  })
}

/**
 * 根据ID获取标签
 */
export function getTagById(id) {
  return request({
    url: `/api/tags/${id}`,
    method: 'get'
  })
}

/**
 * 创建标签
 */
export function createTag(data) {
  return request({
    url: '/api/tags',
    method: 'post',
    data
  })
}

/**
 * 更新标签
 */
export function updateTag(id, data) {
  return request({
    url: `/api/tags/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除标签
 */
export function deleteTag(id) {
  return request({
    url: `/api/tags/${id}`,
    method: 'delete'
  })
}

/**
 * 获取标签使用统计
 */
export function getTagUsageStats() {
  return request({
    url: '/api/tags/usage-stats',
    method: 'get'
  })
}

/**
 * 批量删除标签
 */
export function batchDeleteTags(tagIds) {
  return request({
    url: '/api/tags/batch-delete',
    method: 'delete',
    data: { tagIds }
  })
}
