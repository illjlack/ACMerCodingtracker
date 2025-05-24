// mock/index.js
// Mock 前端 API 数据模拟入口文件

const Mock = require('mockjs')
const { param2Obj } = require('./utils')

// 引入各模块的 mock 数据
const user = require('./user')
const role = require('./role')
const article = require('./article')
const search = require('./remote-search')
const dashboard = require('./dashboard')

// 将所有 mock 数据汇总到一个数组中
const mocks = [
  ...user,
  ...role,
  ...article,
  ...search,
  ...dashboard
]

/**
 * 挂载前端 XHR 拦截，重写 send 方法，
 * 将 Mock.js 的模拟逻辑接入到浏览器原生 XMLHttpRequest 中。
 */
function mockXHR() {
  // 记录原始 send 方法
  Mock.XHR.prototype.proxy_send = Mock.XHR.prototype.send
  // 重写 send
  Mock.XHR.prototype.send = function() {
    if (this.custom.xhr) {
      // 同步 withCredentials 设置
      this.custom.xhr.withCredentials = this.withCredentials || false
      // 同步 responseType
      if (this.responseType) {
        this.custom.xhr.responseType = this.responseType
      }
    }
    // 调用原始 send
    this.proxy_send(...arguments)
  }

  /**
   * 将 Mock 配置的 response 适配为 Express 风格的请求处理
   * @param {Function|Object} respond - mock 数据生成函数或静态数据
   * @returns {Function} - 处理 options 并返回 Mock.mock 结果的函数
   */
  function XHR2ExpressReqWrap(respond) {
    return function(options) {
      let result = null
      if (respond instanceof Function) {
        const { body, type, url } = options
        // 模拟 Express 请求对象格式
        result = respond({
          method: type,
          body: JSON.parse(body),
          query: param2Obj(url)
        })
      } else {
        // 静态数据返回
        result = respond
      }
      // 最终输出 Mock 模拟数据
      return Mock.mock(result)
    }
  }

  // 将每个 mock 配置注册到 Mock.js
  for (const i of mocks) {
    Mock.mock(
      new RegExp(i.url), // 匹配请求 URL（支持正则）
      i.type || 'get', // HTTP 方法，默认为 GET
      XHR2ExpressReqWrap(i.response)
    )
  }
}

// 导出 mocks 列表和 mockXHR 初始化函数
module.exports = {
  mocks,
  mockXHR
}
