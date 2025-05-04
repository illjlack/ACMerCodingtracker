import { login, logout, getInfo } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/auth'
import router, { resetRouter } from '@/router'

// 初始状态
const state = {
  token: getToken(),
  user: {
    roles: [],
    name: '',
    realName: '',
    className: '',
    email: '',
    avatar: '',
    introduction: '',
    luogu: '',
    codeforces: '',
    poj: ''
  }
}

// 同步 mutations
const mutations = {
  SET_TOKEN: (state, token) => {
    state.token = token
  },
  SET_USER_INFO: (state, userInfo) => {
    state.user = { ...state.user, ...userInfo }
  }
}

// 异步 actions
const actions = {
  // 用户登录
  login({ commit }, userInfo) {
    const { username, password } = userInfo
    return new Promise((resolve, reject) => {
      login({ username: username.trim(), password }).then(response => {
        const { data } = response
        commit('SET_TOKEN', data.token)
        setToken(data.token)
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 获取用户信息
  getInfo({ commit, state }) {
    return new Promise((resolve, reject) => {
      getInfo(state.token).then(response => {
        const { data } = response

        if (!data) {
          reject('验证失败，请重新登录。')
        }

        const { roles, name, realName, className, email, avatar, introduction, luogu, codeforces, poj } = data

        if (!roles || roles.length <= 0) {
          reject('获取用户信息失败: 角色信息不能为空！')
        }

        // 更新用户信息
        commit('SET_USER_INFO', {
          roles,
          name,
          realName,
          className,
          email,
          avatar,
          introduction,
          luogu,
          codeforces,
          poj
        })

        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 用户登出
  logout({ commit, dispatch }) {
    return new Promise((resolve, reject) => {
      logout().then(() => {
        commit('SET_TOKEN', '')
        commit('SET_USER_INFO', {
          roles: [],
          name: '',
          realName: '',
          className: '',
          email: '',
          avatar: '',
          introduction: '',
          luogu: '',
          codeforces: '',
          poj: ''
        })
        removeToken()
        resetRouter()

        // 清空 tagsView
        dispatch('tagsView/delAllViews', null, { root: true })

        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 重置 token
  resetToken({ commit }) {
    return new Promise(resolve => {
      commit('SET_TOKEN', '')
      commit('SET_USER_INFO', {
        roles: [],
        name: '',
        realName: '',
        className: '',
        email: '',
        avatar: '',
        introduction: '',
        luogu: '',
        codeforces: '',
        poj: ''
      })
      removeToken()
      resolve()
    })
  },

  // 动态切换角色
  async changeRoles({ commit, dispatch }, role) {
    const token = role + '-token'

    commit('SET_TOKEN', token)
    setToken(token)

    const { roles } = await dispatch('getInfo')

    resetRouter()

    const accessRoutes = await dispatch('permission/generateRoutes', roles, { root: true })
    router.addRoutes(accessRoutes)

    dispatch('tagsView/delAllViews', null, { root: true })
  }
}

// 导出
export default {
  namespaced: true,
  state,
  mutations,
  actions
}
