import { asyncRoutes, constantRoutes } from '@/router'
import store from '@/store'

/**
 * Use meta.role to determine if the current user has permission
 * @param roles
 * @param route
 */
function hasPermission(roles, route) {
  if (route.meta && route.meta.roles) {
    return roles.some(role => route.meta.roles.includes(role))
  } else {
    return true
  }
}

/**
 * Filter asynchronous routing tables by recursion
 * @param routes asyncRoutes
 * @param roles
 */
export function filterAsyncRoutes(routes, roles) {
  const res = []

  routes.forEach(route => {
    const tmp = { ...route }
    if (hasPermission(roles, tmp)) {
      if (tmp.children) {
        tmp.children = filterAsyncRoutes(tmp.children, roles)
      }
      res.push(tmp)
    }
  })

  return res
}

const state = {
  routes: [],
  addRoutes: []
}

const mutations = {
  SET_ROUTES: (state, routes) => {
    state.addRoutes = routes
    state.routes = constantRoutes.concat(routes)
  }
}

function handleDynamicRoutes(routes) {
  const username = store.getters.name
  console.log('开始动态处理路由，当前用户名:', username)

  routes.forEach(route => {
    console.log('处理路由:', route.path)

    // 递归处理子路由
    if (route.children && route.children.length > 0) {
      handleDynamicRoutes(route.children)
    }

    if (route.meta) {
      console.log(`route.meta存在，needDynamic的值是:`, route.meta.needDynamic)
    } else {
      console.log('route.meta不存在')
    }

    if (route.meta && route.meta.needDynamic) {
      console.log('检测到需要动态处理的路由:', route.path)

      // 检查是否包含 :name 动态参数
      if (route.path.includes(':name')) {
        const originalPath = route.path
        route.path = `${username}` // 直接替换成最终路径
        console.log(`原路径: ${originalPath}，已替换为新路径: ${route.path}`)
      }

      // 动态修改标题
      if (route.meta.title) {
        const originalTitle = route.meta.title
        route.meta.title = `${username}的${originalTitle}`
        console.log(`原标题: ${originalTitle}，新标题: ${route.meta.title}`)
      }
    }
  })
}

const actions = {
  generateRoutes({ commit }, roles) {
    return new Promise(resolve => {
      let accessedRoutes
      if (roles.includes('admin')) {
        accessedRoutes = asyncRoutes || []
      } else {
        accessedRoutes = filterAsyncRoutes(asyncRoutes, roles)
      }

      handleDynamicRoutes(accessedRoutes)

      commit('SET_ROUTES', accessedRoutes)
      resolve(accessedRoutes)
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
