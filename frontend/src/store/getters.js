const getters = {
  // app模块
  sidebar: state => state.app.sidebar,
  size: state => state.app.size,
  device: state => state.app.device,

  // tagsView模块
  visitedViews: state => state.tagsView.visitedViews,
  cachedViews: state => state.tagsView.cachedViews,

  token: state => state.user.token,
  name: state => state.user.user.name,
  realName: state => state.user.user.realName,
  className: state => state.user.user.className,
  email: state => state.user.user.email,
  avatar: state => state.user.user.avatar,
  introduction: state => state.user.user.introduction,
  roles: state => state.user.user.roles,
  luogu: state => state.user.user.luogu,
  codeforces: state => state.user.user.codeforces,
  poj: state => state.user.user.poj,

  // 权限模块
  permission_routes: state => state.permission.routes,

  // 错误日志模块
  errorLogs: state => state.errorLog.logs
}

export default getters
