
const tokens = {
  admin: {
    token: 'admin-token'
  },
  editor: {
    token: 'editor-token'
  }
}

const users = {
  'admin-token': {
    roles: ['admin'],
    name: 'admin',
    realName: '超级管理员',
    className: '信息工程1班',
    email: 'admin@example.com',
    avatar: 'https://userpic.codeforces.org/2502707/title/9d101ba1ff9298a2.jpg',
    introduction: '我是超级管理员',
    luogu: 'admin_luogu',
    codeforces: 'admin_cf',
    poj: 'admin_poj'
  },
  'editor-token': {
    roles: ['editor'],
    name: 'editor',
    realName: '普通编辑',
    className: '信息工程2班',
    email: 'editor@example.com',
    avatar: 'https://userpic.codeforces.org/2502707/title/9d101ba1ff9298a2.jpg',
    introduction: '我是普通编辑',
    luogu: 'editor_luogu',
    codeforces: 'editor_cf',
    poj: 'editor_poj'
  }
}
module.exports = [
  // user login
  {
    url: '/vue-element-admin/user/login',
    type: 'post',
    response: config => {
      const { username } = config.body
      const token = tokens[username]

      // mock error
      if (!token) {
        return {
          code: 60204,
          message: 'Account and password are incorrect.'
        }
      }

      return {
        code: 20000,
        data: token
      }
    }
  },

  // get user info
  {
    url: '/vue-element-admin/user/info.*',
    type: 'get',
    response: config => {
      const { token } = config.query
      const info = users[token]

      // mock error
      if (!info) {
        return {
          code: 50008,
          message: 'Login failed, unable to get user details.'
        }
      }

      return {
        code: 20000,
        data: info
      }
    }
  },

  // user logout
  {
    url: '/vue-element-admin/user/logout',
    type: 'post',
    response: _ => {
      return {
        code: 20000,
        data: 'success'
      }
    }
  }
]
