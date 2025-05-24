import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress' // 进度条库
import 'nprogress/nprogress.css' // 进度条样式
import { getToken } from '@/utils/auth' // 从 cookie 中获取 token
import getPageTitle from '@/utils/get-page-title' // 页面标题处理函数

NProgress.configure({ showSpinner: false }) // 配置进度条，不显示旋转加载图标

// 定义白名单，不需要登录即可访问的路由路径
const whiteList = ['/login', '/auth-redirect']

// 路由全局前置守卫
router.beforeEach(async(to, from, next) => {
  // 开启进度条
  NProgress.start()

  // 设置页面标题
  document.title = getPageTitle(to.meta.title)

  // 判断是否有登录 token
  const hasToken = getToken()

  if (hasToken) {
    // 如果已经登录，访问登录页则重定向到首页
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done() // 结束进度条，避免进度条卡住
    } else {
      // 判断是否已经获取用户角色信息
      console.log('路由守卫 name:', store.getters.name)
      console.log('路由守卫 roles:', store.getters.roles)
      const hasRoles = store.getters.roles && store.getters.roles.length > 0
      if (hasRoles) {
        // 已经有角色信息，放行路由
        next()
      } else {
        try {
          // 没有角色信息，调用 getInfo 获取用户信息（包含角色）
          // 注意 roles 必须是一个数组，例如 ['admin'] 或 ['developer', 'editor']
          console.log('开始调用 getInfo 拉取用户信息')
          const { roles } = await store.dispatch('user/getInfo')
          console.log('getInfo 返回角色：', roles)

          // 根据角色生成可访问的路由表
          const accessRoutes = await store.dispatch('permission/generateRoutes', roles)

          // 动态添加这些路由
          router.addRoutes(accessRoutes)

          // 使用 replace:true 以确保路由不会留下历史记录，防止重复导航
          next({ ...to, replace: true })
        } catch (error) {
          // 如果获取用户信息失败（如 token 过期），执行以下操作：
          // 1. 重置 token（清除登录状态）
          await store.dispatch('user/resetToken')
          // 2. 弹出错误提示
          Message.error(error || 'Has Error')
          // 3. 重定向到登录页，并携带当前访问页面路径，登录成功后可跳回
          next(`/login?redirect=${to.path}`)
          // 4. 结束进度条
          NProgress.done()
        }
      }
    }
  } else {
    // 如果没有 token（未登录）
    if (whiteList.indexOf(to.path) !== -1) {
      // 在白名单中则直接放行（比如登录页）
      next()
    } else {
      // 否则跳转到登录页，携带重定向路径
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
})

// 全局后置钩子，关闭进度条
router.afterEach(() => {
  NProgress.done()
})
