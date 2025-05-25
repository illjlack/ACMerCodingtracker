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
