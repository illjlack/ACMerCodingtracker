<template>
  <el-card class="timeline-card">
    <!-- 加载中提示 -->
    <div v-if="loading" class="loading-tip">
      正在加载中，请稍候...
    </div>

    <el-timeline v-else>
      <el-timeline-item
        v-for="item in timeline"
        :key="item.attemptTime + '-' + item.pid"
        :timestamp="formatDate(item.attemptTime)"
        placement="top"
        :icon="item.result === 'AC' ? 'el-icon-circle-check' : 'el-icon-error'"
        :color="item.result === 'AC' ? '#67C23A' : '#F56C6C'"
      >
        <el-card shadow="hover" class="item-card">
          <div class="item-header">
            <span class="platform">{{ item.ojName }}</span>
            <span class="pid">{{ item.pid }}</span>
            <span class="result" :class="item.result">{{ item.result }}</span>
          </div>
          <div class="item-body">
            <a :href="item.url" target="_blank" class="title">
              {{ item.name || item.pid }}
            </a>
            <div class="tags">
              <el-tag
                v-for="tag in item.tags"
                :key="tag"
                size="mini"
                class="tag"
              >
                {{ tag }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>

    <div v-if="total > pageSize && !loading" class="pagination-wrapper">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="currentPage + 1"
        :page-size="pageSize"
        :total="total"
        @current-change="onPageChange"
      />
    </div>
  </el-card>
</template>

<script>
import { listUserTries } from '@/api/usertry'

export default {
  name: 'UserTimeline',
  props: {
    username: { type: String, required: true }
  },
  data() {
    return {
      timeline: [],
      total: 0,
      pageSize: 10,
      currentPage: 0,
      loading: false
    }
  },
  created() {
    this.fetchPage(0)
  },
  methods: {
    async fetchPage(page) {
      this.loading = true
      try {
        const res = await listUserTries(this.username, page, this.pageSize)
        if (res.success) {
          this.timeline = res.data.items
          this.total = res.data.total
          this.currentPage = page
        } else {
          this.$message.error(res.message || '做题时间线获取失败')
        }
      } catch {
        this.$message.error('请求出错，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    onPageChange(page) {
      this.fetchPage(page - 1)
    },
    formatDate(dt) {
      return dt ? dt.replace('T', ' ') : ''
    }
  }
}
</script>

<style scoped>
.timeline-card {
  padding: 20px;
  background: #fafafa;
}
.loading-tip {
  text-align: center;
  color: #409EFF;
  margin: 20px 0;
  font-weight: 500;
}
.el-timeline {
  padding: 0 10px;
}
.item-card {
  padding: 10px;
  margin-bottom: 10px;
}
.item-header {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}
.platform {
  font-weight: bold;
  margin-right: 8px;
  color: #606266;
}
.pid {
  margin-right: 8px;
  color: #909399;
}
.result {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}
.result.AC {
  background: #f0f9eb;
  color: #67C23A;
}
.result.WA {
  background: #fef0f0;
  color: #F56C6C;
}
/* 其他结果类型可按需添加 */

.item-body .title {
  font-size: 14px;
  color: #409EFF;
  text-decoration: none;
}
.item-body .title:hover {
  text-decoration: underline;
}
.tags {
  margin-top: 8px;
}
.tag {
  margin-right: 4px;
}
.pagination-wrapper {
  text-align: center;
  margin-top: 20px;
}
</style>
