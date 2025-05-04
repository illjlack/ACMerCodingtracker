import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

/* Layout */
import Layout from '@/layout'

/**
 * constantRoutes
 * - 保留所有非 Layout 组件路由（登录、重定向、错误页等）
 * - 对于使用 Layout 的路由，只保留“个人信息”和“做题信息”两个页面
 * - 添加 OJ 外部链接分组
 */
export const constantRoutes = [
  // 重定向
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index')
      }
    ]
  },
  // 登录
  {
    path: '/login',
    component: () => import('@/views/login/index'),
    hidden: true
  },
  // 授权重定向
  {
    path: '/auth-redirect',
    component: () => import('@/views/login/auth-redirect'),
    hidden: true
  },
  // 错误页面
  {
    path: '/404',
    component: () => import('@/views/error-page/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error-page/401'),
    hidden: true
  },

  // 个人信息页
  {
    path: '/profile',
    component: Layout,
    children: [
      {
        path: '',
        component: () => import('@/views/profile/index'),
        name: 'Profile',
        meta: { title: '个人信息', icon: 'user', noCache: true }
      }
    ]
  },
  // 做题信息页
  {
    path: '/problems',
    component: Layout,
    children: [
      {
        path: '',
        component: () => import('@/views/profile/index'),
        name: 'Problems',
        meta: { title: '做题信息', icon: 'edit', noCache: true }
      }
    ]
  },
  // OJ 外部链接分组
  {
    path: '/oj',
    component: Layout,
    alwaysShow: true,
    meta: { title: '在线评测系统', icon: 'link' },
    children: [
      {
        path: 'https://www.luogu.com.cn/',
        meta: { title: 'Luogu', icon: 'link' }
      },
      {
        path: 'https://codeforces.com/',
        meta: { title: 'Codeforces', icon: 'link' }
      },
      {
        path: 'http://poj.org/',
        meta: { title: 'POJ', icon: 'link' }
      }
    ]
  },

  // 默认主页（跳转到个人信息）
  {
    path: '/',
    redirect: '/profile',
    hidden: true
  },
  // 兜底：匹配不到
  {
    path: '*',
    redirect: '/404',
    hidden: true
  }
]

const createRouter = () =>
  new Router({
    // mode: 'history', // 若需使用 history 模式，后端需支持
    scrollBehavior: () => ({ y: 0 }),
    routes: constantRoutes
  })

const router = createRouter()

/**
 * 重置路由（登出或权限改变时使用）
 */
export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher
}

export default router
