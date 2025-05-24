<template>
  <div class="social-signup-container">
    <!-- 微信登录按钮 -->
    <div class="sign-btn" @click="wechatHandleClick('wechat')">
      <span class="wx-svg-container"><svg-icon icon-class="wechat" class="icon" /></span>
      WeChat
    </div>

    <!-- QQ登录按钮 -->
    <div class="sign-btn" @click="tencentHandleClick('tencent')">
      <span class="qq-svg-container"><svg-icon icon-class="qq" class="icon" /></span>
      QQ
    </div>
  </div>
</template>

<script>
export default {
  // 组件名称
  name: 'SocialSignin',

  methods: {
    // 微信登录点击处理
    wechatHandleClick(thirdpart) {
      alert('ok') // 测试用的提示框
      // 以下代码是伪代码，用于在微信登录时的跳转逻辑
      // this.$store.commit('SET_AUTH_TYPE', thirdpart)
      // const appid = 'xxxxx' // 微信的appid
      // const redirect_uri = encodeURIComponent('xxx/redirect?redirect=' + window.location.origin + '/auth-redirect')
      // const url = 'https://open.weixin.qq.com/connect/qrconnect?appid=' + appid + '&redirect_uri=' + redirect_uri + '&response_type=code&scope=snsapi_login#wechat_redirect'
      // openWindow(url, thirdpart, 540, 540)  // 使用 openWindow 函数打开微信授权页面
    },

    // 腾讯QQ登录点击处理
    tencentHandleClick(thirdpart) {
      alert('ok') // 测试用的提示框
      // 以下代码是伪代码，用于在腾讯QQ登录时的跳转逻辑
      // this.$store.commit('SET_AUTH_TYPE', thirdpart)
      // const client_id = 'xxxxx'  // QQ的client_id
      // const redirect_uri = encodeURIComponent('xxx/redirect?redirect=' + window.location.origin + '/auth-redirect')
      // const url = 'https://graph.qq.com/oauth2.0/authorize?response_type=code&client_id=' + client_id + '&redirect_uri=' + redirect_uri
      // openWindow(url, thirdpart, 540, 540)  // 使用 openWindow 函数打开QQ授权页面
    }
  }
}
</script>

<style lang="scss" scoped>
.social-signup-container {
  margin: 20px 0;  /* 设置容器的上下间距 */

  /* 登录按钮的样式 */
  .sign-btn {
    display: inline-block;  /* 设置为内联块元素，使其横向排列 */
    cursor: pointer;  /* 鼠标悬停时显示为指针 */
  }

  /* 图标的样式 */
  .icon {
    color: #fff;  /* 设置图标的颜色为白色 */
    font-size: 24px;  /* 设置图标大小 */
    margin-top: 8px;  /* 设置图标顶部的间距 */
  }

  /* 微信图标容器的样式 */
  .wx-svg-container,
  .qq-svg-container {
    display: inline-block;  /* 设置为内联块元素 */
    width: 40px;  /* 设置容器宽度 */
    height: 40px;  /* 设置容器高度 */
    line-height: 40px;  /* 设置容器内文字的行高，使图标垂直居中 */
    text-align: center;  /* 设置文字居中对齐 */
    padding-top: 1px;  /* 设置顶部内边距 */
    border-radius: 4px;  /* 设置圆角 */
    margin-bottom: 20px;  /* 设置容器底部间距 */
    margin-right: 5px;  /* 设置容器右侧间距 */
  }

  /* 微信按钮的背景色 */
  .wx-svg-container {
    background-color: #24da70;  /* 设置微信按钮的背景色 */
  }

  /* QQ按钮的背景色，且设置左侧间距 */
  .qq-svg-container {
    background-color: #6BA2D6;  /* 设置QQ按钮的背景色 */
    margin-left: 50px;  /* 设置QQ按钮左侧的间距 */
  }
}
</style>
