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
  className: state => state.user.user.major,
  email: state => state.user.user.email,
  avatar: state => state.user.user.avatar,
  introduction: state => state.user.user.introduction,
  roles: state => state.user.user.roles,
  luogu: state => {
    const val = state.user.user.ojAccounts && state.user.user.ojAccounts.LUOGU
    if (!val) return ''
    if (Array.isArray(val)) {
      return val.join(';')
    } else if (typeof val === 'string') {
      // 假设字符串可能用逗号、空格或分号分隔，统一转换为分号分隔
      return val.split(/[,; ]+/).filter(Boolean).join(';')
    }
    return ''
  },

  codeforces: state => {
    const val = state.user.user.ojAccounts && state.user.user.ojAccounts.CODEFORCES
    if (!val) return ''
    if (Array.isArray(val)) {
      return val.join(';')
    } else if (typeof val === 'string') {
      return val.split(/[,; ]+/).filter(Boolean).join(';')
    }
    return ''
  },

  poj: state => {
    const val = state.user.user.ojAccounts && state.user.user.ojAccounts.POJ
    if (!val) return ''
    if (Array.isArray(val)) {
      return val.join(';')
    } else if (typeof val === 'string') {
      return val.split(/[,; ]+/).filter(Boolean).join(';')
    }
    return ''
  },

  // 权限模块
  permission_routes: state => state.permission.routes,

  // 错误日志模块
  errorLogs: state => state.errorLog.logs
}

export default getters
