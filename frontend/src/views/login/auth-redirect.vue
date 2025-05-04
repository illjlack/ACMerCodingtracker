<script>
export default {
  // 组件名称
  name: 'AuthRedirect',

  // 组件的生命周期钩子，创建时执行
  created() {
    // 获取 URL 查询参数中的哈希部分（去掉 '?'）
    const hash = window.location.search.slice(1)

    // 检查浏览器是否支持 localStorage
    if (window.localStorage) {
      // 将 OAuth 代码存入浏览器的 localStorage
      window.localStorage.setItem('x-admin-oauth-code', hash)

      // 关闭当前窗口（通常是 OAuth 授权后的跳转页面）
      window.close()
    }
  },

  // 渲染函数，用于避免警告信息
  render: function(h) {
    // 返回空的虚拟 DOM，避免 Vue 提示 "没有返回内容"
    return h()
  }
}
</script>
