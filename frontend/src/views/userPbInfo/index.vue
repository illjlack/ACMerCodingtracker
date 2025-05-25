<template>
  <div v-if="userExists" class="app-container">
    <el-row :gutter="20">
      <!-- 左侧显示用户卡片 -->
      <el-col :span="6" :xs="24">
        <user-card :user="user" :name="name" />
      </el-col>

      <!-- 右侧显示标签页 -->
      <el-col :span="18" :xs="24">
        <el-card>
          <!-- 绑定 activeTab 并监听切换事件 -->
          <el-tabs v-model="activeTab" @tab-click="onTabClick">
            <!-- 时间线面板 -->
            <el-tab-pane label="Timeline" name="timeline">
              <!-- 使用 key 强制重新渲染组件 -->
              <user-timeline
                v-if="activeTab === 'timeline'"
                :key="name"
                ref="timeline"
                :name="name"
              />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>

  <!-- 加载用户信息提示 -->
  <div v-else class="loading-container">
    <el-spinner type="circle" /> 加载用户信息中...
  </div>
</template>

<script>
import UserCard from './components/UserCard_byName.vue'
import TimeLineByName from './components/TimeLine_byName.vue'

export default {
  name: 'ProfilePage',
  components: {
    UserCard,
    UserTimeline: TimeLineByName
  },
  props: {
    name: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      activeTab: 'timeline', // 默认选中时间线标签
      user: { name: this.name }, // 初始化 user 对象，并直接使用 props 的 name
      userExists: true // 用户是否存在
    }
  },
  watch: {
    // 监听 name 的变化，并重新加载时间线数据
    name(newName) {
      console.log('name changed:', newName) // 打印出 name 的值
      this.user = { name: newName } // 更新用户信息
      this.$nextTick(() => {
        this.$refs.timeline?.fetchPage(0) // 重新加载数据
      })
    },
    // 监听 activeTab 改变，切换到时间线时再加载数据
    activeTab(newTab) {
      if (newTab === 'timeline') {
        this.$nextTick(() => {
          this.$refs.timeline?.fetchPage(0)
        })
      }
    }
  },
  mounted() {
    // 初次加载时，如果 activeTab 为 timeline，则立即加载数据
    console.log('Initial name:', this.name) // 打印初始传递的 name
    if (this.activeTab === 'timeline') {
      this.$nextTick(() => {
        this.$refs.timeline?.fetchPage(0)
      })
    }
  },
  methods: {
    onTabClick(tab) {
      // 时间线标签点击时加载数据
      if (tab.name === 'timeline') {
        this.$refs.timeline?.fetchPage(0) // 初始化加载第一页数据
      }
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 32px;
  background-color: #f0f2f5;
}

h1 {
  margin-bottom: 24px;
  color: #333;
}

.tab-card {
  padding: 16px;
}

.profile-pane {
  padding: 16px;
}

.timeline-pane {
  padding: 16px;
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  font-size: 16px;
  color: #888;
}
</style>
