<template>
  <div v-if="userExists" class="app-container"> <!-- 有用户信息才显示 -->
    <el-row :gutter="20">
      <el-col :span="6" :xs="24">
        <user-card :user="user" />
      </el-col>
      <el-col :span="18" :xs="24">
        <el-card>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="Account" name="account">
              <account :user="user" @update="onUserUpdate" />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
  <div v-else>
    <p>加载用户信息中...</p>
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'
import UserCard from './components/UserCard'
import Account from './components/Account'

export default {
  name: 'Profile',
  components: { UserCard, Account },
  data() {
    return {
      activeTab: 'account'
    }
  },
  computed: {
    ...mapGetters({
      name: 'name',
      realName: 'realName',
      className: 'className',
      email: 'email',
      avatar: 'avatar',
      roles: 'roles',
      luogu: 'luogu',
      codeforces: 'codeforces',
      poj: 'poj'
    }),
    // 计算完整用户对象，绑定到子组件
    user() {
      return {
        name: this.name,
        realName: this.realName,
        className: this.className,
        email: this.email,
        avatar: this.avatar || require('@/assets/default-avatar.png'),
        role: this.roles.join(' | '),
        luogu: this.luogu,
        codeforces: this.codeforces,
        poj: this.poj
      }
    },
    // 判断是否有有效用户数据
    userExists() {
      return this.name && this.roles.length > 0
    }
  },
  created() {
    this.fetchUserInfo()
  },
  methods: {
    ...mapActions('user', ['getInfo', 'modifyUser']),
    // 统一调用 Vuex action 拉取用户信息，避免重复请求
    async fetchUserInfo() {
      try {
        await this.getInfo()
      } catch (err) {
        this.$message.error('获取用户信息失败，请重新登录')
        // 可考虑跳转登录页
      }
    },
    // 处理子组件 Account 更新事件，调用 Vuex action 同步更新数据
    async onUserUpdate(updatedUser) {
      try {
        // 这里的 modifyUser 需要你自己在 Vuex 实现，负责调用接口并更新用户信息
        await this.modifyUser(updatedUser)
        this.$message.success('用户信息已更新')
      } catch (err) {
        this.$message.error('更新用户信息失败')
      }
    }
  }
}
</script>
