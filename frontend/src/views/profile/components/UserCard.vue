<template>
  <el-card style="margin-bottom:20px;">
    <div slot="header" class="clearfix">
      <span>About me</span>
    </div>

    <!-- 用户头像与基本信息 -->
    <div class="user-profile">
      <div class="box-center">
        <pan-thumb
          :image="avatarUrl"
          height="100px"
          width="100px"
          :hoverable="false"
          @error="onAvatarError"
        >
          <div>Hello</div>
          <div class="user-roles">
            <span v-for="role in userRoles" :key="role" :class="'role-' + role.toLowerCase()">
              {{ role }}
            </span>
          </div>
        </pan-thumb>
      </div>
      <div class="box-center">
        <div class="user-realname text-center">{{ user.realName }}</div>
        <div class="user-username text-center text-muted">
          ({{ user.name }})
        </div>
      </div>
      <div class="box-center user-class text-center text-muted">
        班级：{{ user.className }}
      </div>
    </div>

    <!-- 联系方式 & 账号信息 -->
    <div class="user-bio">
      <div class="user-contact user-bio-section">
        <div class="user-bio-section-header">
          <svg-icon icon-class="email" />
          <span>Contact</span>
        </div>
        <div class="user-bio-section-body">
          <p v-if="user.email"><strong>Email:</strong> {{ user.email }}</p>
        </div>
      </div>

      <div class="user-accounts user-bio-section">
        <div class="user-bio-section-header">
          <svg-icon icon-class="link" />
          <span>Accounts</span>
        </div>
        <div class="user-bio-section-body">
          <p v-if="user.luogu">
            <a
              :href="`https://www.luogu.com.cn/user/${user.luogu}`"
              target="_blank"
            >Luogu: {{ user.luogu }}</a>
          </p>
          <p v-if="user.codeforces">
            <a
              :href="`https://codeforces.com/profile/${user.codeforces}`"
              target="_blank"
            >Codeforces: {{ user.codeforces }}</a>
          </p>
          <p v-if="user.poj">
            <a
              :href="`https://poj.org/userstatus?user_id=${user.poj}`"
              target="_blank"
            >POJ: {{ user.poj }}</a>
          </p>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script>
import PanThumb from '@/components/PanThumb'
import defaultAvatar from '@/assets/default-avatar.png'

export default {
  name: 'UserCard',
  components: { PanThumb },
  props: {
    user: {
      type: Object,
      default: () => ({
        name: '',
        realName: '',
        className: '',
        email: '',
        avatar: '',
        role: '',
        luogu: '',
        codeforces: '',
        poj: ''
      })
    }
  },
  data() {
    return {
      defaultAvatar,
      avatarError: false
    }
  },
  computed: {
    baseApiUrl() {
      return process.env.VUE_APP_BASE_API || 'http://localhost:8080'
    },
    avatarUrl() {
      const avatar = this.user.avatar
      if (!avatar || this.avatarError) return this.defaultAvatar
      if (avatar.startsWith('http')) return avatar
      return this.baseApiUrl + avatar
    },
    userRoles() {
      return this.user.role.split(' | ')
    }
  },
  methods: {
    onAvatarError() {
      this.avatarError = true
    }
  }
}
</script>

<style lang="scss" scoped>
.box-center {
  margin: 0 auto;
  display: table;
}

.text-muted {
  color: #777;
}

.user-profile {
  .user-realname {
    font-size: 18px;
    font-weight: bold;
  }
  .user-username {
    padding-top: 4px;
  }
  .user-class {
    margin-top: 8px;
  }
  .user-roles {
    margin-top: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    span {
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 500;
    }

    .role-super_admin {
      background-color: #f56c6c;
      color: white;
    }

    .role-admin {
      background-color: #e6a23c;
      color: white;
    }

    .role-user {
      background-color: #67c23a;
      color: white;
    }
  }
}

.user-bio {
  margin-top: 20px;
  color: #606266;

  .user-bio-section {
    font-size: 14px;
    padding: 15px 0;

    .user-bio-section-header {
      border-bottom: 1px solid #dfe6ec;
      padding-bottom: 10px;
      margin-bottom: 10px;
      font-weight: bold;
      display: flex;
      align-items: center;

      svg-icon {
        margin-right: 4px;
      }
    }

    .user-bio-section-body {
      padding-left: 4px;

      p {
        margin: 4px 0;
      }
    }
  }
}
</style>
