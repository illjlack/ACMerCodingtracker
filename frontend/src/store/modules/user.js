import { login, logout, getInfo } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/auth'
import router, { resetRouter } from '@/router'

// 初始状态
const state = {
  token: getToken(), // 从缓存获取 token
  user: {
    roles: [], // 用户角色列表
    name: '', // 用户名
    realName: '', // 真实姓名
    major: '', // 专业
    email: '', // 邮箱
    avatar: '', // 头像地址
    ojAccounts: {} // OJ 账号信息
  }
}

const getters = {
  token: state => state.token,
  name: state => state.user.name,
  realName: state => state.user.realName,
  className: state => state.user.className,
  email: state => state.user.email,
  avatar: state => state.user.avatar,
  roles: state => state.user.roles,
  luogu: state => (state.user.ojAccounts && state.user.ojAccounts.LUOGU) ? state.user.ojAccounts.LUOGU : '',
  codeforces: state => (state.user.ojAccounts && state.user.ojAccounts.CODEFORCES) ? state.user.ojAccounts.CODEFORCES : '',
  poj: state => (state.user.ojAccounts && state.user.ojAccounts.POJ) ? state.user.ojAccounts.POJ : ''
}

// 同步修改状态的方法
const mutations = {
  // 设置 token
  SET_TOKEN: (state, token) => {
    console.log('mutation SET_TOKEN: ', token)
    state.token = token
  },
  // 设置用户信息
  SET_USER_INFO: (state, userInfo) => {
    console.log('mutation SET_USER_INFO: ', userInfo)
    state.user = { ...state.user, ...userInfo }
  }
}

// 异步操作，通常用于调用接口
const actions = {
  // 用户登录
  login({ commit }, userInfo) {
    const { username, password } = userInfo
    console.log('action login 被调用，用户名：', username)
    return new Promise((resolve, reject) => {
      login({ username: username.trim(), password }).then(response => {
        // 后端返回格式：{ success, message, data: { token } }
        const resData = response
        console.log('login 接口返回：', resData)
        if (resData.success) {
          const token = resData.data.token
          commit('SET_TOKEN', token) // 提交 mutation 保存 token
          setToken(token) // 持久化 token（如 cookie/localStorage）
          console.log('登录成功，token 已保存')
          resolve()
        } else {
          console.error('登录失败，原因：', resData.message)
          reject(new Error(resData.message || '登录失败'))
        }
      }).catch(error => {
        console.error('登录请求出错：', error)
        reject(error)
      })
    })
  },

  // 获取用户信息
  getInfo({ commit, state }) {
    console.log('action getInfo 被调用，当前 token：', state.token)
    return new Promise((resolve, reject) => {
      getInfo(state.token).then(response => {
        // 后端返回格式：{ success, message, data: {...} }
        const resData = response
        console.log('getInfo 接口返回：', resData)

        if (!resData.success || !resData.data) {
          console.warn('验证失败，请重新登录。')
          reject('验证失败，请重新登录。')
          return
        }

        const data = resData.data
        const { roles, name, realName, major, email, avatar, ojAccounts } = data

        if (!roles || roles.length <= 0) {
          console.warn('获取用户信息失败: 角色信息不能为空！')
          reject('获取用户信息失败: 角色信息不能为空！')
          return
        }

        // 提交 mutation 更新用户信息
        commit('SET_USER_INFO', {
          roles,
          name,
          realName,
          major,
          email,
          avatar,
          ojAccounts
        })

        // 立即打印 action 的 state
        console.log('action 内部 state.user:', state.user)
        console.log('用户信息已更新')
        resolve(data)
      }).catch(error => {
        console.error('获取用户信息请求出错：', error)
        reject(error)
      })
    })
  },

  // 用户登出
  logout({ commit, dispatch }) {
    console.log('action logout 被调用')
    return new Promise((resolve, reject) => {
      logout().then(() => {
        commit('SET_TOKEN', '') // 清空 token
        commit('SET_USER_INFO', { // 清空用户信息
          roles: [],
          name: '',
          realName: '',
          major: '',
          email: '',
          avatar: '',
          ojAccounts: {}
        })
        removeToken() // 清除持久化 token
        resetRouter() // 重置路由

        // 清空标签页视图
        dispatch('tagsView/delAllViews', null, { root: true })

        console.log('登出成功')
        resolve()
      }).catch(error => {
        console.error('登出请求出错：', error)
        reject(error)
      })
    })
  },

  // 重置 token，清除用户信息
  resetToken({ commit }) {
    console.log('action resetToken 被调用')
    return new Promise(resolve => {
      commit('SET_TOKEN', '')
      commit('SET_USER_INFO', {
        roles: [],
        name: '',
        realName: '',
        major: '',
        email: '',
        avatar: '',
        ojAccounts: {}
      })
      removeToken()
      console.log('token 重置成功')
      resolve()
    })
  },

  // 动态切换角色（测试用）
  async changeRoles({ commit, dispatch }, role) {
    console.log('action changeRoles 被调用，目标角色：', role)
    const token = role + '-token'

    commit('SET_TOKEN', token)
    setToken(token)

    const { roles } = await dispatch('getInfo')

    resetRouter()

    const accessRoutes = await dispatch('permission/generateRoutes', roles, { root: true })
    router.addRoutes(accessRoutes)

    dispatch('tagsView/delAllViews', null, { root: true })

    console.log('角色切换完成')
  }
}

// 导出 vuex 模块
export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
