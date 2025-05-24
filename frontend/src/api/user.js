// src/api/user.js

import request from '@/utils/request'

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
 * 修改用户信息
 * @param {{ id?: number, username?: string, realName?: string, major?: string, email?: string, avatar?: string }} data
 */
export function modifyUser(data) {
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

export function logout() {
  return request({
    url: '/api/auth/logout',
    method: 'post'
  })
}
