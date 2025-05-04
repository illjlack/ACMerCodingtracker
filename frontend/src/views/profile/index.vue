<template>
  <div class="app-container"> <!-- 根容器 -->
    <div v-if="user"> <!-- 仅当 user 对象存在时渲染内容 -->
      <el-row :gutter="20"> <!-- Element UI 栅格，列间距为 20px -->

        <el-col :span="6" :xs="24"> <!-- 大屏占 6/24，小屏占 24/24 -->
          <user-card :user="user" /> <!-- 用户卡片组件，展示头像、姓名等 -->
        </el-col>

        <el-col :span="18" :xs="24"> <!-- 大屏占 18/24，小屏占 24/24 -->
          <el-card> <!-- 卡片容器，包裹选项卡 -->
            <el-tabs v-model="activeTab"> <!-- 绑定当前激活的标签页值 -->
              <el-tab-pane label="Account" name="account"> <!-- “Account” 标签页 -->
                <account :user="user" @update="onUserUpdate" /> <!-- 账号信息编辑组件 -->
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </el-col>

      </el-row>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex' // 从 Vuex 中映射 getters
import UserCard from './components/UserCard' // 用户卡片组件
import Account from './components/Account' // 账号信息编辑组件

export default {
  name: 'Profile',
  components: { UserCard, Account }, // 注册子组件
  data() {
    return {
      user: null, // 本地 user 对象，初始化为 null
      activeTab: 'activity' // 默认激活标签页为“activity”
    }
  },
  computed: {
    ...mapGetters({
      name: 'name', // 登录名
      realName: 'realName', // 真实姓名
      className: 'className', // 班级
      email: 'email', // 邮箱
      avatar: 'avatar', // 头像 URL
      roles: 'roles', // 角色数组
      luogu: 'luogu', // Luogu 账号
      codeforces: 'codeforces', // Codeforces 账号
      poj: 'poj' // POJ 账号
    })
  },
  created() {
    this.initUser() // 组件创建时调用初始化方法
  },
  methods: {
    // 初始化本地 user 对象，从 Vuex getters 读取各字段
    initUser() {
      this.user = {
        name: this.name, // 登录名
        realName: this.realName, // 真实姓名
        className: this.className, // 班级
        email: this.email, // 邮箱
        avatar: this.avatar, // 头像 URL
        role: this.roles.join(' | '), // 角色串
        luogu: this.luogu, // Luogu 账号
        codeforces: this.codeforces, // Codeforces 账号
        poj: this.poj // POJ 账号
      }
    },
    // 处理子组件 Account 发出的 update 事件，更新本地 user
    onUserUpdate(updated) {
      this.user = { ...this.user, ...updated }
    }
  }
}
</script>
