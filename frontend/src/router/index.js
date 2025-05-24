import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

/* Layout */
import Layout from '@/layout'
// eslint-disable-next-line no-unused-vars
// eslint-disable-next-line no-unused-vars
import route from 'svgo/plugins/prefixIds'
// eslint-disable-next-line no-unused-vars
import log from 'echarts/src/scale/Log'

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
  {
    path: '/register',
    component: () => import('@/views/register/index'),
    hidden: true
  },
  {
    path: '/forgot-password',
    component: () => import('@/views/forgot-password/index'),
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
  {
    path: '/profile',
    component: Layout,
    redirect: '/profile/index',
    hidden: true,
    children: [
      {
        path: 'index',
        component: () => import('@/views/profile/index'),
        name: 'Profile',
        meta: { title: 'Profile', icon: 'user', noCache: true }
      }
    ]
  },

  // 做题信息页
  {
    path: '/allPbInfo',
    component: Layout,
    children: [
      {
        path: '',
        component: () => import('@/views/allPbInfo/index'),
        name: 'allPbInfo',
        meta: { title: '团队做题信息', icon: 'peoples', noCache: true }
      }
    ]
  },
  {
    path: '/userPbInfo/:name',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/userPbInfo/index'),
        name: 'UserPbInfo',
        props: route => ({ name: route.params.name }),
        meta: { title: ':name的个人做题信息', icon: 'user', noCache: true, needDynamic: true, roles: ['admin', 'editor', 'user'] }
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

export const asyncRoutes = [
  // 个人做题信息页：可选传参 userId
  {
    path: '/userPbInfo',
    component: Layout,
    children: [
      {
        path: ':name',
        component: () => import('@/views/userPbInfo/index'),
        name: 'UserPbInfo',
        props: route => ({ name: route.params.name }),
        meta: { title: '个人做题信息', icon: 'user', noCache: true, needDynamic: true, roles: ['admin', 'editor', 'user'] }
      }
    ]
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

router.beforeEach((to, from, next) => {
  // 如果路由的 meta.title 是函数，则调用它并设置为页面标题
  if (to.meta && to.meta.title) {
    if (to.meta.title.includes('的个人做题信息')) {
      to.meta.title = to.params.name + '的个人做题信息'
    }
  }
  next()
})
