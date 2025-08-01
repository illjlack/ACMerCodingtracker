// src/api/user.js

import request from '@/utils/request'

export function fetchUserInfoByName(username) {
  return request({
    url: `/api/auth/info`,
    method: 'get',
    params: { username }
  })
}

/**
 * 用户登录
 * @param {{ username: string, password: string }} data
 */
export function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

/**
 * 用户注册
 * @param {{ username: string, password: string, realName: string, major: string, email: string }} data
 */
export function register(data) {
  return request({
    url: '/api/auth/register',
    method: 'post',
    data
  })
}

/**
 * 更新用户信息接口
 * @param {Object} data 用户信息对象
 * @param {number} [data.id] 用户ID（可选）
 * @param {string} [data.name] 用户名（username）（可选）
 * @param {string} [data.realName] 真实姓名（可选）
 * @param {string} [data.major] 专业（可选）
 * @param {string} [data.email] 邮箱（可选）
 * @param {string} [data.avatar] 头像URL或Base64字符串（可选）
 * @param {Object} [data.ojAccounts] OJ账号信息，键为平台名，值为账号字符串（可选）
 */
export function updateUser(data) {
  return request({
    url: '/api/auth/modify',
    method: 'put',
    data
  })
}

/**
 * 修改密码
 * @param {{ username: string, oldPassword: string, newPassword: string }} data
 */
export function changePassword(data) {
  return request({
    url: '/api/auth/modifyPassword',
    method: 'put',
    data
  })
}

export function getInfo(token) {
  return request({
    url: '/api/auth/userInfo',
    method: 'get'
  })
}

/**
 * 上传头像
 * @param {FormData} formData 包含文件的 FormData 对象，字段名为 'avatar'
 */
export function uploadAvatar(formData) {
  return request({
    url: '/api/auth/upload-avatar',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data' // 必须指定
    }
  })
}

export function sendEmailCode(data) {
  // data: { email: string }
  return request({
    url: '/api/email/sendCode',
    method: 'post',
    params: data // 发送查询参数 ?email=xxx
  })
}

// 通过邮箱验证码修改密码
export function resetPasswordByEmail(data) {
  // data: { username: string, email: string, code: string, newPassword: string }
  return request({
    url: '/api/email/modifyPassword',
    method: 'put',
    params: data
  })
}

export function logout() {
  return request({
    url: '/api/auth/logout',
    method: 'post'
  })
}

/**
 * 获取用户列表
 */
export function getUserList() {
  return request({
    url: '/api/admin/users',
    method: 'get'
  })
}

/**
 * 创建新用户
 * @param {{ username: string, password: string, realName: string, email: string, role: string }} data
 */
export function createUser(data) {
  return request({
    url: '/api/admin/users',
    method: 'post',
    data
  })
}

/**
 * 更新用户信息
 * @param {number} id 用户ID
 * @param {{ realName: string, email: string, role: string }} data
 */
export function updateUserByAdmin(id, data) {
  return request({
    url: `/api/admin/users/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除用户
 * @param {number} id 用户ID
 */
export function deleteUser(id) {
  return request({
    url: `/api/admin/users/${id}`,
    method: 'delete'
  })
}

/**
 * 切换用户状态（启用/禁用）
 * @param {number} id 用户ID
 */
export function toggleUserStatus(id) {
  return request({
    url: `/api/admin/users/${id}/toggle-status`,
    method: 'put'
  })
}

// ================== 用户标签相关 API ==================

/**
 * 获取所有用户标签
 */
export function getAllUserTags() {
  return request({
    url: '/api/admin/user-tags',
    method: 'get'
  })
}

/**
 * 根据ID获取用户标签
 * @param {number} id 标签ID
 */
export function getUserTagById(id) {
  return request({
    url: `/api/admin/user-tags/${id}`,
    method: 'get'
  })
}

/**
 * 创建新的用户标签
 * @param {{ name: string, color?: string, description?: string }} data
 */
export function createUserTag(data) {
  return request({
    url: '/api/admin/user-tags',
    method: 'post',
    data
  })
}

/**
 * 更新用户标签
 * @param {number} id 标签ID
 * @param {{ name?: string, color?: string, description?: string }} data
 */
export function updateUserTag(id, data) {
  return request({
    url: `/api/admin/user-tags/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除用户标签
 * @param {number} id 标签ID
 */
export function deleteUserTag(id) {
  return request({
    url: `/api/admin/user-tags/${id}`,
    method: 'delete'
  })
}

/**
 * 为用户添加标签
 * @param {number} userId 用户ID
 * @param {number} tagId 标签ID
 */
export function addTagToUser(userId, tagId) {
  return request({
    url: `/api/admin/user-tags/users/${userId}/tags/${tagId}`,
    method: 'post'
  })
}

/**
 * 从用户移除标签
 * @param {number} userId 用户ID
 * @param {number} tagId 标签ID
 */
export function removeTagFromUser(userId, tagId) {
  return request({
    url: `/api/admin/user-tags/users/${userId}/tags/${tagId}`,
    method: 'delete'
  })
}

/**
 * 设置用户的所有标签
 * @param {number} userId 用户ID
 * @param {number[]} tagIds 标签ID数组
 */
export function setUserTags(userId, tagIds) {
  return request({
    url: `/api/admin/user-tags/users/${userId}/tags`,
    method: 'put',
    data: tagIds
  })
}

/**
 * 搜索用户标签
 * @param {string} name 标签名称
 */
export function searchUserTags(name) {
  return request({
    url: '/api/admin/user-tags/search',
    method: 'get',
    params: { name }
  })
}

/**
 * 获取标签使用统计
 * @param {number} id 标签ID
 */
export function getUserTagUsage(id) {
  return request({
    url: `/api/admin/user-tags/${id}/usage`,
    method: 'get'
  })
}

/**
 * 获取支持的OJ平台列表
 */
export function getOJPlatforms() {
  return request({
    url: '/api/admin/users/oj-platforms',
    method: 'get'
  })
}

export function getAllUsers() {
  return request({
    url: '/api/users/list',
    method: 'get'
  })
}
