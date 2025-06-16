<template>
  <div class="token-management-container">
    <div class="page-header">
      <h2>平台认证管理</h2>
      <p class="page-description">
        管理需要认证token的OJ平台，确保爬虫能够正常访问需要登录的内容
      </p>
    </div>

    <!-- 浏览器扩展提示 -->
    <el-alert
      title="🚀 推荐使用浏览器扩展自动获取Token"
      type="info"
      :closable="false"
      show-icon
      class="extension-alert"
    >
      <template slot="default">
        <div class="extension-info">
          <p>我们提供了浏览器扩展来帮助您快速获取Token，无需手动复制Cookie！</p>
          <div class="extension-features">
            <span class="feature-item">✅ 一键获取Token</span>
            <span class="feature-item">✅ 自动检测登录状态</span>
            <span class="feature-item">✅ 支持多平台</span>
            <span class="feature-item">✅ 自动复制到剪贴板</span>
          </div>
          <div class="extension-actions">
            <el-button
              type="primary"
              size="mini"
              @click="showExtensionGuide"
            >
              查看安装指南
            </el-button>
            <el-button
              type="success"
              size="mini"
              @click="downloadChromeExtension"
            >
              下载Chrome版本
            </el-button>
            <el-button
              type="warning"
              size="mini"
              @click="downloadFirefoxExtension"
            >
              🦊 下载Firefox版本
            </el-button>
          </div>
        </div>
      </template>
    </el-alert>

    <div class="actions-bar">
      <el-button type="primary" icon="el-icon-refresh" @click="refreshAllStatus">
        刷新状态
      </el-button>
      <el-button type="success" icon="el-icon-check" @click="validateAllTokens">
        验证所有Token
      </el-button>
    </div>

    <div v-if="loading" class="loading-container">
      <el-loading-text>加载平台信息中...</el-loading-text>
    </div>

    <div v-else-if="platforms.length === 0" class="empty-container">
      <el-empty description="没有需要token认证的平台" />
    </div>

    <div v-else class="platform-cards">
      <el-card
        v-for="platform in platforms"
        :key="platform.name"
        class="platform-card"
        :class="{ 'platform-card--invalid': platform.valid === false }"
      >
        <div slot="header" class="card-header">
          <span class="platform-name">{{ platform.displayName }}</span>
          <el-tag
            :type="getTagType(platform)"
            size="mini"
          >
            {{ getStatusText(platform) }}
          </el-tag>
        </div>

        <div class="card-content">
          <div class="status-info">
            <p class="token-format">
              <i class="el-icon-info" />
              Token格式: {{ platform.tokenFormat || '未知格式' }}
            </p>

            <p v-if="platform.hasToken" class="token-info">
              <i class="el-icon-key" />
              Token长度: {{ platform.tokenLength }} 字符
            </p>
            <p v-else class="no-token">
              <i class="el-icon-warning" />
              未配置认证token
            </p>

            <p v-if="platform.validationMessage" class="validation-message">
              <i :class="platform.valid ? 'el-icon-success' : 'el-icon-error'" />
              {{ platform.validationMessage }}
            </p>
          </div>

          <div class="card-actions">
            <el-button
              size="mini"
              type="primary"
              icon="el-icon-edit"
              @click="editToken(platform)"
            >
              {{ platform.hasToken ? '更新' : '添加' }}Token
            </el-button>

            <el-button
              v-if="platform.hasToken"
              size="mini"
              type="success"
              icon="el-icon-check"
              :loading="platform.validating"
              @click="validateToken(platform)"
            >
              验证
            </el-button>

            <el-button
              v-if="platform.hasToken"
              size="mini"
              type="danger"
              icon="el-icon-delete"
              @click="deleteToken(platform)"
            >
              删除
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- Token编辑对话框 -->
    <el-dialog
      :title="editDialog.title"
      :visible.sync="editDialog.visible"
      width="600px"
      @close="closeEditDialog"
    >
      <div class="edit-dialog-content">
        <div v-if="editDialog.platform" class="platform-info">
          <div class="platform-header">
            <h4>{{ editDialog.platform.displayName }}</h4>
            <el-button
              type="primary"
              size="mini"
              icon="el-icon-link"
              @click="openPlatformPage(editDialog.platform.name)"
            >
              访问平台
            </el-button>
          </div>
          <p class="platform-description">{{ getPlatformDescription(editDialog.platform.name) }}</p>
          <p class="token-format-info">
            <strong>Token格式:</strong> {{ editDialog.platform.tokenFormat || 'key=value' }}
          </p>
        </div>

        <el-form ref="tokenForm" :model="editDialog.form" label-width="120px">
          <el-form-item label="认证Token:" required>
            <el-input
              v-model="editDialog.form.token"
              type="textarea"
              :rows="4"
              placeholder="请输入从浏览器获取的认证token..."
              maxlength="2048"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <div class="token-help">
              <h5>如何获取Token:</h5>
              <div class="help-methods">
                <div class="method-card recommended">
                  <h6>🚀 推荐方式：使用浏览器扩展</h6>
                  <ol>
                    <li>安装我们提供的浏览器扩展</li>
                    <li>在对应平台登录后点击扩展图标</li>
                    <li>一键获取并复制Token</li>
                    <li>粘贴到下方输入框</li>
                  </ol>
                  <el-button type="text" size="mini" @click="showExtensionGuide">安装指南</el-button>
                </div>

                <div class="method-card manual">
                  <h6>🔧 手动方式：开发者工具</h6>
                  <ol>
                    <li>
                      在浏览器中登录 {{ editDialog.platform ? editDialog.platform.displayName : '' }}
                      <el-button
                        type="text"
                        size="mini"
                        icon="el-icon-link"
                        style="margin-left: 8px;"
                        @click="openPlatformPage(editDialog.platform ? editDialog.platform.name : '')"
                      >
                        快速跳转
                      </el-button>
                    </li>
                    <li>按F12打开开发者工具</li>
                    <li>切换到Network标签页</li>
                    <li>刷新页面并找到任意请求</li>
                    <li v-if="editDialog.platform && editDialog.platform.name === 'LEETCODE'">
                      在请求头中找到csrftoken值，格式为：csrftoken=xxx
                    </li>
                    <li v-else>复制请求头中的Cookie值</li>
                  </ol>
                </div>
              </div>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <span slot="footer" class="dialog-footer">
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="editDialog.saving"
          @click="saveToken"
        >
          保存
        </el-button>
      </span>
    </el-dialog>

    <!-- 扩展安装指南对话框 -->
    <el-dialog
      title="🚀 浏览器扩展安装指南"
      :visible.sync="extensionGuideDialog.visible"
      width="700px"
    >
      <div class="extension-guide">
        <div class="guide-section">
          <h4>📦 安装步骤</h4>
          <ol class="install-steps">
            <li>
              <strong>下载扩展文件</strong>
              <p>点击下方按钮下载扩展压缩包并解压到本地文件夹</p>
              <el-button type="primary" size="mini" @click="downloadExtension">下载扩展</el-button>
            </li>
            <li>
              <strong>打开Chrome扩展管理</strong>
              <p>在地址栏输入 <code>chrome://extensions/</code> 或通过菜单进入</p>
            </li>
            <li>
              <strong>开启开发者模式</strong>
              <p>在扩展管理页面右上角开启"开发者模式"开关</p>
            </li>
            <li>
              <strong>加载扩展</strong>
              <p>点击"加载已解压的扩展程序"，选择解压后的文件夹</p>
            </li>
            <li>
              <strong>完成安装</strong>
              <p>扩展安装成功后会在浏览器工具栏显示图标</p>
            </li>
          </ol>
        </div>

        <div class="guide-section">
          <h4>🎯 使用方法</h4>
          <div class="usage-methods">
            <div class="usage-card">
              <h5>方式一：扩展弹窗</h5>
              <ul>
                <li>访问OJ平台并登录</li>
                <li>点击浏览器工具栏的扩展图标</li>
                <li>在弹窗中点击"获取Token"</li>
                <li>Token自动复制到剪贴板</li>
              </ul>
            </div>
            <div class="usage-card">
              <h5>方式二：页面按钮</h5>
              <ul>
                <li>访问OJ平台并登录</li>
                <li>页面右上角显示"🔑 获取Token"按钮</li>
                <li>点击按钮即可获取Token</li>
                <li>Token自动复制到剪贴板</li>
              </ul>
            </div>
          </div>
        </div>

        <div class="guide-section">
          <h4>🌟 支持的平台</h4>
          <div class="supported-platforms">
            <el-tag v-for="platform in supportedPlatforms" :key="platform" type="success" size="mini">
              {{ platform }}
            </el-tag>
          </div>
        </div>

        <div class="guide-section">
          <h4>🌐 支持的浏览器</h4>
          <div class="supported-browsers">
            <el-tag v-for="browser in supportedBrowsers" :key="browser" :type="browser.includes('Firefox') ? 'warning' : 'primary'" size="mini">
              {{ browser }}
            </el-tag>
          </div>
          <div class="browser-notes">
            <p><strong>🦊 Firefox 用户超简单安装：</strong></p>
            <ol>
              <li>点击"🦊 下载Firefox版本"按钮</li>
              <li>解压文件到任意文件夹</li>
              <li>Firefox地址栏输入：<code>about:debugging</code></li>
              <li>点击"此Firefox" → "临时载入附加组件"</li>
              <li>选择解压文件夹中的 <code>manifest.json</code></li>
              <li>完成！无需重命名任何文件</li>
            </ol>
            <p class="firefox-note">💡 重启Firefox后需要重新加载，这是正常的安全机制</p>
          </div>
        </div>

        <div class="guide-section">
          <h4>⚠️ 注意事项</h4>
          <ul class="notice-list">
            <li>使用前请确保已登录对应的OJ平台</li>
            <li>扩展需要Cookie读取权限，这是获取Token的必要条件</li>
            <li>Token包含敏感信息，请妥善保管</li>
            <li>Token有时效性，失效后需要重新获取</li>
            <li>扩展只在本地处理数据，不会上传到任何服务器</li>
          </ul>
        </div>
      </div>

      <span slot="footer" class="dialog-footer">
        <el-button @click="extensionGuideDialog.visible = false">关闭</el-button>
        <el-button type="success" @click="downloadChromeExtension">下载Chrome版本</el-button>
        <el-button type="warning" @click="downloadFirefoxExtension">🦊 下载Firefox版本</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getTokenStatus, updateToken, validatePlatformToken, deleteToken } from '@/api/usertry'

export default {
  name: 'TokenManagement',
  data() {
    return {
      loading: true,
      platforms: [],
      editDialog: {
        visible: false,
        title: '',
        platform: null,
        form: {
          token: ''
        },
        saving: false
      },
      extensionGuideDialog: {
        visible: false
      },
      supportedPlatforms: [
        '洛谷 (LUOGU)',
        'LeetCode',
        'LeetCode中国',
        'AtCoder',
        'TopCoder',
        'Virtual Judge',
        'SPOJ',
        'CodeChef'
      ],
      supportedBrowsers: [
        'Chrome (推荐)',
        'Firefox 🦊',
        'Edge (基于Chromium)',
        'Opera',
        '其他Chromium内核浏览器'
      ]
    }
  },
  created() {
    this.loadTokenStatus()
  },
  methods: {
    async loadTokenStatus() {
      this.loading = true
      try {
        const res = await getTokenStatus()
        if (res && res.success && res.data) {
          this.updatePlatformList(res.data.platforms)
        }
      } catch (error) {
        this.$message.error('加载token状态失败: ' + error.message)
      } finally {
        this.loading = false
      }
    },

    updatePlatformList(platformData) {
      // 只显示需要token认证的平台
      this.platforms = Object.keys(platformData)
        .filter(platformName => {
          const data = platformData[platformName]
          return data && data.requiresToken === true
        })
        .map(platformName => {
          const data = platformData[platformName]
          return {
            name: platformName,
            displayName: this.getPlatformDisplayName(platformName),
            hasToken: data.hasToken || false,
            tokenLength: data.tokenLength || 0,
            tokenFormat: data.tokenFormat || '',
            requiresToken: data.requiresToken || false,
            valid: null,
            validationMessage: '',
            validating: false
          }
        })
    },

    getPlatformDisplayName(platformName) {
      const displayNames = {
        'LUOGU': '洛谷',
        'LEETCODE': 'LeetCode',
        'ATCODER': 'AtCoder',
        'TOPCODER': 'TopCoder',
        'CODEFORCES': 'Codeforces',
        'HDU': 'HDU OJ',
        'POJ': 'POJ'
      }
      return displayNames[platformName] || platformName
    },

    async refreshAllStatus() {
      await this.loadTokenStatus()
      this.$message.success('状态已刷新')
    },

    async validateAllTokens() {
      const tokensToValidate = this.platforms.filter(p => p.hasToken)

      if (tokensToValidate.length === 0) {
        this.$message.info('没有已配置的token需要验证')
        return
      }

      const promises = tokensToValidate.map(p => this.validateToken(p, false))

      await Promise.all(promises)
      this.$message.success('所有token验证完成')
    },

    async validateToken(platform, showMessage = true) {
      // 标记为验证中
      const platformName = platform.name
      platform.validating = true
      try {
        const res = await validatePlatformToken(platformName.toLowerCase())
        if (res && res.success && res.data) {
          // 在等待异步结果后重新定位平台对象，避免atomic-update警告
          const idx = this.platforms.findIndex(p => p.name === platformName)
          if (idx !== -1) {
            this.$set(this.platforms[idx], 'valid', res.data.valid)
            this.$set(this.platforms[idx], 'validationMessage', res.data.message)
          }

          if (showMessage) {
            if (res.data.valid) {
              this.$message.success(`${platform.displayName} token验证通过`)
            } else {
              this.$message.warning(`${platform.displayName} token验证失败: ${res.data.message}`)
            }
          }
        }
      } catch (error) {
        const idx = this.platforms.findIndex(p => p.name === platformName)
        if (idx !== -1) {
          this.$set(this.platforms[idx], 'valid', false)
          this.$set(this.platforms[idx], 'validationMessage', '验证异常: ' + error.message)
        }

        if (showMessage) {
          this.$message.error(`${platform.displayName} token验证失败: ${error.message}`)
        }
      } finally {
        const idx = this.platforms.findIndex(p => p.name === platformName)
        if (idx !== -1) {
          this.$set(this.platforms[idx], 'validating', false)
        }
      }
    },

    editToken(platform) {
      this.editDialog.platform = platform
      this.editDialog.title = `${platform.hasToken ? '更新' : '添加'} ${platform.displayName} Token`
      this.editDialog.form.token = ''
      this.editDialog.visible = true
    },

    async saveToken() {
      if (!this.editDialog.form.token.trim()) {
        this.$message.warning('请输入token')
        return
      }

      // 添加token格式验证
      const token = this.editDialog.form.token.trim()
      
      // 基本格式验证：必须包含 =
      if (!token.includes('=')) {
        this.$message.warning('Token格式错误，请使用格式：key=value')
        return
      }

      // 检查值是否为空
      const tokenValue = token.split('=', 2)[1]
      if (!tokenValue || tokenValue.trim() === '') {
        this.$message.warning('Token值不能为空')
        return
      }

      this.editDialog.saving = true
      try {
        const res = await updateToken(
          this.editDialog.platform.name.toLowerCase(),
          token
        )

        if (res && res.success) {
          this.$message.success('Token保存成功')
          this.editDialog.visible = false
          await this.loadTokenStatus()
          // 自动验证新保存的token
          const platform = this.platforms.find(p => p.name === this.editDialog.platform.name)
          if (platform) {
            await this.validateToken(platform, false)
          }
        }
      } catch (error) {
        this.$message.error('Token保存失败: ' + error.message)
      } finally {
        this.editDialog.saving = false
      }
    },

    async deleteToken(platform) {
      try {
        await this.$confirm(`确定要删除 ${platform.displayName} 的认证token吗？`, '确认删除', {
          type: 'warning'
        })

        const res = await deleteToken(platform.name.toLowerCase())
        if (res && res.success) {
          this.$message.success('Token删除成功')
          await this.loadTokenStatus()
          // eslint-disable-next-line require-atomic-updates
          platform.valid = null
          // eslint-disable-next-line require-atomic-updates
          platform.validationMessage = ''
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('Token删除失败: ' + error.message)
        }
      }
    },

    closeEditDialog() {
      this.editDialog.form.token = ''
    },

    getStatusText(platform) {
      if (!platform.hasToken) return '未配置'
      if (platform.valid === null) return '未验证'
      return platform.valid ? '有效' : '失效'
    },

    getTagType(platform) {
      if (!platform.hasToken) return 'info'
      if (platform.valid === null) return 'warning'
      return platform.valid ? 'success' : 'danger'
    },

    getPlatformDescription(platformName) {
      const descriptions = {
        'LUOGU': '洛谷需要登录后的Cookie来访问用户提交记录',
        'LEETCODE': 'LeetCode需要csrftoken来访问用户数据，格式为：csrftoken=xxx',
        'ATCODER': 'AtCoder需要登录后的Cookie来访问用户数据',
        'TOPCODER': 'TopCoder需要登录后的Cookie来访问用户数据',
        'CODEFORCES': 'Codeforces在某些情况下需要认证token',
        'HDU': 'HDU OJ在某些情况下需要认证token',
        'POJ': 'POJ在某些情况下需要认证token'
      }
      return descriptions[platformName] || '该平台需要认证token来访问用户数据'
    },

    showExtensionGuide() {
      this.extensionGuideDialog.visible = true
    },

    downloadChromeExtension() {
      // 下载Chrome版本
      const link = document.createElement('a')
      link.href = '/coding-tracker-token-helper.zip'
      link.download = 'coding-tracker-token-helper.zip'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)

      this.$message.success('Chrome扩展下载开始！解压后在Chrome扩展管理页面加载即可。')
    },

    downloadFirefoxExtension() {
      // 下载Firefox版本
      const link = document.createElement('a')
      link.href = '/firefox-extension.zip'
      link.download = 'firefox-extension.zip'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)

      this.$message.success('🦊 Firefox扩展下载开始！解压后在about:debugging页面临时载入即可，无需重命名文件！')
    },

    openPlatformPage(platformName) {
      const platformUrls = {
        'LUOGU': 'https://www.luogu.com.cn/',
        'LEETCODE': 'https://leetcode.com/',
        'ATCODER': 'https://atcoder.jp/',
        'TOPCODER': 'https://www.topcoder.com/',
        'CODEFORCES': 'https://codeforces.com/',
        'HDU': 'http://acm.hdu.edu.cn/',
        'POJ': 'http://poj.org/',
        'VIRTUAL_JUDGE': 'https://vjudge.net/',
        'SPOJ': 'https://www.spoj.com/',
        'CODECHEF': 'https://www.codechef.com/'
      }

      const url = platformUrls[platformName]
      if (url) {
        window.open(url, '_blank')
        this.$message.info(`已在新标签页打开${this.getPlatformDisplayName(platformName)}，请登录后获取Token`)
      } else {
        this.$message.warning('未找到该平台的链接')
      }
    }
  }
}
</script>

<style scoped lang="scss">
.token-management-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;

  .page-header {
    margin-bottom: 24px;
    text-align: center;

    h2 {
      color: #303133;
      margin-bottom: 8px;
    }

    .page-description {
      color: #606266;
      margin: 0;
    }
  }

  .actions-bar {
    margin-bottom: 24px;
    text-align: center;

    .el-button {
      margin: 0 8px;
    }
  }

  .loading-container, .empty-container {
    text-align: center;
    padding: 40px 0;
  }

  .platform-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 20px;
  }

  .platform-card {
    transition: box-shadow 0.3s ease;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    &--invalid {
      border-color: #f56c6c;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .platform-name {
        font-weight: 600;
        font-size: 16px;
      }
    }

    .card-content {
      .status-info {
        margin-bottom: 16px;

        p {
          margin: 8px 0;
          display: flex;
          align-items: center;

          i {
            margin-right: 8px;
          }
        }

        .token-format {
          color: #909399;
          font-size: 12px;
        }

        .token-info {
          color: #409eff;
        }

        .no-token {
          color: #909399;
        }

        .validation-message {
          font-size: 13px;

          .el-icon-success {
            color: #67c23a;
          }

          .el-icon-error {
            color: #f56c6c;
          }
        }
      }

      .card-actions {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
      }
    }
  }

  .edit-dialog-content {
    .platform-info {
      margin-bottom: 20px;
      padding: 16px;
      background: #f5f7fa;
      border-radius: 4px;

      .platform-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
      }

      h4 {
        margin: 0;
        color: #303133;
      }

      .platform-description {
        margin: 0 0 8px 0;
        color: #606266;
        font-size: 14px;
      }

      .token-format-info {
        margin: 0;
        color: #409eff;
        font-size: 13px;
      }
    }

    .token-help {
      background: #fff8dc;
      padding: 12px;
      border-radius: 4px;
      border: 1px solid #ffd700;

      h5 {
        margin: 0 0 8px 0;
        color: #e6a23c;
      }

      ol {
        margin: 0;
        padding-left: 20px;

        li {
          margin-bottom: 4px;
          font-size: 13px;
          color: #606266;
        }
      }
    }
  }

  // 扩展提示样式
  .extension-alert {
    margin-bottom: 24px;

    .extension-info {
      p {
        margin: 0 0 12px 0;
        color: #606266;
      }

      .extension-features {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
        margin-bottom: 16px;

        .feature-item {
          background: #f0f9ff;
          color: #409eff;
          padding: 4px 8px;
          border-radius: 4px;
          font-size: 12px;
        }
      }

      .extension-actions {
        display: flex;
        gap: 8px;
      }
    }
  }

  // Token帮助样式
  .token-help {
    .help-methods {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      margin-top: 12px;

      .method-card {
        padding: 12px;
        border-radius: 6px;
        border: 1px solid #dcdfe6;

        &.recommended {
          background: #f0f9ff;
          border-color: #409eff;

          h6 {
            color: #409eff;
          }
        }

        &.manual {
          background: #fafafa;

          h6 {
            color: #606266;
          }
        }

        h6 {
          margin: 0 0 8px 0;
          font-size: 13px;
        }

        ol {
          margin: 0;
          padding-left: 16px;
          font-size: 12px;

          li {
            margin-bottom: 4px;
          }
        }
      }
    }
  }

  // 扩展指南对话框样式
  .extension-guide {
    .guide-section {
      margin-bottom: 24px;

      h4 {
        margin: 0 0 12px 0;
        color: #303133;
        font-size: 16px;
      }

      .install-steps {
        li {
          margin-bottom: 16px;

          strong {
            color: #409eff;
          }

          p {
            margin: 4px 0 8px 0;
            color: #606266;
            font-size: 14px;
          }

          code {
            background: #f5f5f5;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: monospace;
          }
        }
      }

      .usage-methods {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 16px;

        .usage-card {
          padding: 16px;
          background: #f8f9fa;
          border-radius: 6px;
          border: 1px solid #e9ecef;

          h5 {
            margin: 0 0 8px 0;
            color: #495057;
          }

          ul {
            margin: 0;
            padding-left: 16px;

            li {
              margin-bottom: 4px;
              font-size: 14px;
              color: #6c757d;
            }
          }
        }
      }

      .supported-platforms {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }

      .supported-browsers {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-bottom: 16px;
      }

      .browser-notes {
        background: #fff8dc;
        padding: 12px;
        border-radius: 6px;
        border: 1px solid #ffd700;

        p {
          margin: 0 0 8px 0;
          color: #e6a23c;
          font-weight: 600;
        }

        ul, ol {
          margin: 0;
          padding-left: 16px;

          li {
            margin-bottom: 4px;
            color: #606266;
            font-size: 13px;
          }
        }

        .firefox-note {
          margin-top: 8px;
          font-size: 12px;
          color: #909399;
          font-style: italic;
        }

        code {
          background: #f5f5f5;
          padding: 2px 4px;
          border-radius: 3px;
          font-family: monospace;
          font-size: 12px;
        }
      }

      .notice-list {
        margin: 0;
        padding-left: 16px;

        li {
          margin-bottom: 8px;
          color: #606266;
          font-size: 14px;
        }
      }
    }
  }
}
</style>
