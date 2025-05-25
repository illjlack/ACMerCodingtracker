<template>
  <div v-if="userExists" class="app-container">
    <el-row :gutter="20">
      <el-col :span="6" :xs="24">
        <user-card :user="user" />
      </el-col>
      <el-col :span="18" :xs="24">
        <el-card>
          <!-- 绑定 activeTab 并监听切换事件 -->
          <el-tabs v-model="activeTab" @tab-click="onTabClick">
            <el-tab-pane label="Account" name="account">
              <account :user="user" @update="onUserUpdate" />
            </el-tab-pane>
            <el-tab-pane label="Timeline" name="timeline">
              <!-- 只有activeTab为timeline时才渲染时间线组件 -->
              <user-timeline
                v-if="activeTab === 'timeline'"
                ref="timeline"
                :username="name"
              />
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
import UserTimeline from './components/TimeLine' // 注意路径和名字是否正确

export default {
  name: 'Profile',
  components: {
    UserCard,
    Account,
    UserTimeline
  },
  data() {
    return {
      activeTab: 'account' // 默认激活账户页
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
    userExists() {
      return !!this.name && this.roles.length > 0
    }
  },
  created() {
    this.fetchUserInfo()
  },
  methods: {
    ...mapActions('user', ['getInfo', 'modifyUser']),
    async fetchUserInfo() {
      try {
        await this.getInfo()
      } catch (err) {
        this.$message.error('获取用户信息失败，请重新登录')
      }
    },
    onTabClick(tab) {
      if (tab.name === 'timeline') {
        // 点击 timeline 标签时，调用时间线组件的 fetchPage
        this.$refs.timeline && this.$refs.timeline.fetchPage(0)
      }
    },
    async onUserUpdate(updatedUser) {
      function normalizeAccounts(accountStr) {
        if (!accountStr) return ''
        const parts = accountStr
          .split(/;|；/)
          .map(s => s.trim())
          .filter(s => s)
        return parts.join(';')
      }
      try {
        const payload = {
          id: updatedUser.id,
          name: updatedUser.name,
          realName: updatedUser.realName,
          major: updatedUser.className,
          email: updatedUser.email,
          avatar: updatedUser.avatar,
          ojAccounts: {
            LUOGU: normalizeAccounts(updatedUser.luogu),
            CODEFORCES: normalizeAccounts(updatedUser.codeforces),
            POJ: normalizeAccounts(updatedUser.poj)
          }
        }
        await this.modifyUser(payload)
        this.$message.success('用户信息已更新')
      } catch {
        this.$message.error('更新用户信息失败')
      }
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>
