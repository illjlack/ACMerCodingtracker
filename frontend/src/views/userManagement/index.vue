<template>
  <div class="user-management-container">
    <div class="controls">
      <el-input
        v-model="search"
        placeholder="搜索用户名或真实姓名"
        clearable
        prefix-icon="el-icon-search"
        class="search-input"
        style="width: 250px; margin-right: 16px;"
      />

      <el-select
        v-model="selectedTags"
        multiple
        collapse-tags
        placeholder="按标签筛选"
        style="width: 280px; margin-right: 16px;"
        @change="filterByTags"
      >
        <el-option
          v-for="tag in allTags"
          :key="tag.id"
          :label="tag.name"
          :value="tag.id"
        >
          <span :style="{ color: tag.color }">{{ tag.name }}</span>
        </el-option>
      </el-select>

      <el-button v-if="isAdmin" type="primary" icon="el-icon-plus" @click="handleAdd">
        添加用户
      </el-button>

      <el-button v-if="isAdmin" type="info" icon="el-icon-collection-tag" @click="handleTagManagement">
        标签管理
      </el-button>

      <el-button v-if="isAdmin" type="success" icon="el-icon-upload2" @click="showImportDialog">
        批量导入
      </el-button>

      <user-export v-if="isAdmin" />
    </div>

    <div class="table-wrapper">
      <el-table
        v-loading="loading"
        :data="displayList"
        border
        stripe
        style="width: 100%;"
        fit
      >
        <el-table-column type="index" label="序号" min-width="60" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="真实姓名" min-width="120" />
        <el-table-column prop="major" label="专业" min-width="150" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="ojAccounts" label="OJ账号" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="account in row.ojAccounts" :key="account.id" style="margin-right: 5px">
              {{ account.platform }}: {{ account.accountName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="120">
          <template #default="{ row }">
            <el-tag
              v-for="tag in row.tags"
              :key="tag.id"
              :color="tag.color"
              :style="{ color: getTextColor(tag.color) }"
              style="margin-right: 5px; margin-bottom: 2px;"
              size="mini"
            >
              {{ tag.name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="roles" label="角色" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.superAdmin" type="danger">超级管理员</el-tag>
            <el-tag v-else-if="row.admin" type="warning">管理员</el-tag>
            <el-tag v-else type="info">普通用户</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="active" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">
              {{ row.active ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              size="mini"
              type="primary"
              :disabled="!canEditUser(row)"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              size="mini"
              :type="row.active ? 'warning' : 'success'"
              :disabled="!canEditUser(row)"
              @click="handleToggleStatus(row)"
            >
              {{ row.active ? '禁用' : '启用' }}
            </el-button>
            <el-button
              size="mini"
              type="danger"
              :disabled="!canDeleteUser(row)"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 用户表单对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="userForm"
        :model="userForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="userForm.major" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option
              v-if="canSetRole('SUPER_ADMIN')"
              label="超级管理员"
              value="SUPER_ADMIN"
            />
            <el-option
              v-if="canSetRole('ADMIN')"
              label="管理员"
              value="ADMIN"
            />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-select v-model="userForm.tags" multiple style="width: 100%" placeholder="选择标签">
            <el-option
              v-for="tag in allTags"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
            >
              <span :style="{ color: tag.color }">{{ tag.name }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="OJ账号" prop="ojAccounts">
          <div v-for="(account, index) in userForm.ojAccounts" :key="index" class="oj-account">
            <el-select
              v-model="account.platform"
              placeholder="请选择平台"
              style="width: 150px; margin-right: 10px"
              @change="validateOjAccount(index)"
            >
              <el-option
                v-for="platform in ojPlatforms"
                :key="platform.code"
                :label="platform.name"
                :value="platform.code"
              />
            </el-select>
            <el-input
              v-model="account.accountName"
              placeholder="请输入账号名"
              style="width: 200px; margin-right: 10px"
              :class="{ 'input-error': !isValidOjAccount(account) }"
              @blur="validateOjAccount(index)"
            />
            <el-button type="danger" icon="el-icon-delete" circle @click="removeOjAccount(index)" />
          </div>
          <el-button type="primary" icon="el-icon-plus" @click="addOjAccount">添加OJ账号</el-button>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog
      title="批量导入用户"
      :visible.sync="importDialogVisible"
      width="500px"
    >
      <user-import @import-success="handleImportSuccess" />
    </el-dialog>

    <!-- 标签管理对话框 -->
    <el-dialog
      title="标签管理"
      :visible.sync="tagDialogVisible"
      width="800px"
    >
      <div class="tag-management">
        <div class="tag-controls">
          <el-button v-if="isAdmin" type="primary" icon="el-icon-plus" @click="handleAddTag">
            新建标签
          </el-button>
        </div>

        <el-table :data="allTags" border style="width: 100%; margin-top: 20px;">
          <el-table-column prop="name" label="标签名称" min-width="120" />
          <el-table-column prop="color" label="颜色" width="80">
            <template #default="{ row }">
              <div :style="{ backgroundColor: row.color, width: '20px', height: '20px', borderRadius: '3px' }" />
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="150" />
          <el-table-column label="使用统计" width="100">
            <template #default="{ row }">
              {{ getTagUsageCount(row.id) || 0 }} 人
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button v-if="isAdmin" size="mini" type="primary" @click="handleEditTag(row)">编辑</el-button>
              <el-button v-if="isAdmin" size="mini" type="danger" @click="handleDeleteTag(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 标签表单对话框 -->
    <el-dialog
      :title="tagFormTitle"
      :visible.sync="tagFormDialogVisible"
      width="400px"
    >
      <el-form
        ref="tagForm"
        :model="tagForm"
        :rules="tagRules"
        label-width="80px"
      >
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="tagForm.name" />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-color-picker v-model="tagForm.color" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="tagForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="tagFormDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitTagForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getUserList,
  createUser,
  updateUserByAdmin,
  deleteUser,
  toggleUserStatus,
  getAllUserTags,
  createUserTag,
  updateUserTag,
  deleteUserTag,
  getOJPlatforms

} from '@/api/user'
import UserImport from './components/UserImport'
import UserExport from './components/UserExport'
import { mapGetters } from 'vuex'

export default {
  name: 'UserManagement',
  components: {
    UserImport,
    UserExport
  },
  data() {
    return {
      loading: false,
      search: '',
      list: [],
      filteredList: [],
      selectedTags: [],
      allTags: [],
      tagUsageMap: {},
      ojPlatforms: [],
      dialogVisible: false,
      importDialogVisible: false,
      tagDialogVisible: false,
      tagFormDialogVisible: false,
      isEdit: false,
      isTagEdit: false,
      userForm: {
        username: '',
        realName: '',
        email: '',
        major: '',
        role: 'USER', // 默认为普通用户
        tags: [],
        ojAccounts: [],
        password: '',
        active: true
      },
      tagForm: {
        name: '',
        color: '#409EFF',
        description: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        realName: [
          { required: true, message: '请输入真实姓名', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱地址', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
        ],
        major: [
          { required: true, message: '请输入专业', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择用户角色', trigger: 'change' }
        ]
      },
      tagRules: {
        name: [
          { required: true, message: '请输入标签名称', trigger: 'blur' },
          { min: 1, max: 20, message: '长度在 1 到 20 个字符', trigger: 'blur' }
        ],
        color: [
          { required: true, message: '请选择标签颜色', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    ...mapGetters(['name', 'roles']),
    dialogTitle() {
      return this.isEdit ? '编辑用户' : '添加用户'
    },
    tagFormTitle() {
      return this.isTagEdit ? '编辑标签' : '新建标签'
    },
    displayList() {
      let filtered = [...this.list]

      // 按搜索关键词筛选
      const kw = this.search.trim().toLowerCase()
      if (kw) {
        filtered = filtered.filter(
          item =>
            (item.username && item.username.toLowerCase().includes(kw)) ||
            (item.realName && item.realName.toLowerCase().includes(kw))
        )
      }

      // 按标签筛选
      if (this.selectedTags.length > 0) {
        filtered = filtered.filter(user => {
          return user.tags && user.tags.some(tag => this.selectedTags.includes(tag.id))
        })
      }

      return filtered
    },
    currentUser() {
      const username = this.name
      const user = this.list.find(u => u.username === username)
      if (!user) {
        return {
          id: null,
          isSuperAdmin: this.roles.includes('SUPER_ADMIN'),
          isAdmin: this.roles.includes('ADMIN')
        }
      }
      return {
        ...user,
        isSuperAdmin: this.roles.includes('SUPER_ADMIN'),
        isAdmin: this.roles.includes('ADMIN')
      }
    },
    /**
     * 当前登录用户是否是超级管理员
     */
    isSuperAdmin() {
      return this.roles.includes('SUPER_ADMIN')
    },

    /**
     * 当前登录用户是否是管理员（包括超级管理员）
     */
    isAdmin() {
      return this.roles.includes('ADMIN') || this.roles.includes('SUPER_ADMIN')
    }
  },
  created() {
    this.fetchData()
    this.fetchTags()
    this.fetchOJPlatforms()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const res = await getUserList()
        if (res.success) {
          this.list = res.data || []
        } else {
          this.$message.error(res.message || '获取用户列表失败')
          this.list = []
        }
      } catch (error) {
        console.error('获取用户列表失败:', error)
        this.$message.error('获取用户列表失败')
        this.list = []
      } finally {
        this.loading = false
      }
    },
    async fetchTags() {
      try {
        const res = await getAllUserTags()
        if (res.success) {
          this.allTags = res.data || []
          // 获取标签使用统计
          for (const tag of this.allTags) {
            this.getTagUsageCount(tag.id)
          }
        } else {
          this.$message.error(res.message || '获取标签列表失败')
          this.allTags = []
        }
      } catch (error) {
        console.error('获取标签列表失败:', error)
        this.$message.error('获取标签列表失败')
        this.allTags = []
      }
    },
    async fetchOJPlatforms() {
      try {
        const res = await getOJPlatforms()
        if (res.success) {
          this.ojPlatforms = res.data || []
        } else {
          this.$message.error(res.message || '获取OJ平台列表失败')
          this.ojPlatforms = []
        }
      } catch (error) {
        console.error('获取OJ平台列表失败:', error)
        this.$message.error('获取OJ平台列表失败')
        this.ojPlatforms = []
      }
    },
    handleAdd() {
      this.isEdit = false
      this.dialogVisible = true
    },
    handleEdit(row) {
      if (!this.canEditUser(row)) {
        this.$message.warning('权限不足：无法编辑该用户')
        return
      }

      this.isEdit = true
      // 获取用户的主要角色（假设用户只有一个主要角色）
      let userRole = 'USER' // 默认值
      if (row.roles && row.roles.length > 0) {
        // 优先级：SUPER_ADMIN > ADMIN > USER
        if (row.roles.includes('SUPER_ADMIN')) {
          userRole = 'SUPER_ADMIN'
        } else if (row.roles.includes('ADMIN')) {
          userRole = 'ADMIN'
        } else {
          userRole = 'USER'
        }
      }

      this.userForm = {
        ...row,
        role: userRole,
        tags: row.tags ? row.tags.map(tag => tag.id) : [],
        ojAccounts: row.ojAccounts || []
      }
      this.dialogVisible = true
    },
    async handleToggleStatus(row) {
      if (!this.canEditUser(row)) {
        this.$message.warning('权限不足：无法修改该用户状态')
        return
      }

      try {
        await this.$confirm(
          `确认要${row.active ? '禁用' : '启用'}该用户吗？`,
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        await toggleUserStatus(row.id)
        this.$message.success('操作成功')
        this.fetchData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('操作失败:', error)
          this.$message.error('操作失败')
        }
      }
    },
    async handleDelete(row) {
      if (!this.canDeleteUser(row)) {
        this.$message.warning('权限不足：无法删除该用户')
        return
      }

      try {
        await this.$confirm('确认要删除该用户吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteUser(row.id)
        this.$message.success('删除成功')
        this.fetchData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
          this.$message.error('删除失败')
        }
      }
    },
    resetForm() {
      this.userForm = {
        username: '',
        realName: '',
        email: '',
        major: '',
        role: 'USER',
        tags: [],
        ojAccounts: [],
        password: '',
        active: true
      }
      if (this.$refs.userForm) {
        this.$refs.userForm.resetFields()
      }
    },
    addOjAccount() {
      this.userForm.ojAccounts.push({
        platform: '',
        accountName: ''
      })
    },
    removeOjAccount(index) {
      this.userForm.ojAccounts.splice(index, 1)
    },
    validateOjAccount(index) {
      const account = this.userForm.ojAccounts[index]
      if (!account.platform) {
        this.$message.warning('请选择 OJ 平台')
        return false
      }
      if (!account.accountName || !account.accountName.trim()) {
        this.$message.warning('OJ 账号名不能为空')
        return false
      }

      // 检查重复
      const duplicateIndex = this.userForm.ojAccounts.findIndex((acc, i) =>
        i !== index && acc.platform === account.platform &&
        acc.accountName && acc.accountName.trim() === account.accountName.trim()
      )

      if (duplicateIndex !== -1) {
        this.$message.warning(`重复的 OJ 账号：${account.platform} - ${account.accountName.trim()}`)
        return false
      }

      return true
    },
    isValidOjAccount(account) {
      return account.platform && account.accountName && account.accountName.trim()
    },
    showImportDialog() {
      this.importDialogVisible = true
    },
    handleImportSuccess() {
      this.importDialogVisible = false
      this.fetchData()
    },
    async submitForm() {
      try {
        await this.$refs.userForm.validate()
        const data = { ...this.userForm }

        // 将单个角色转换为角色数组
        data.roles = [data.role]
        delete data.role

        // 处理 OJ 账号数据，确保数据格式正确
        if (data.ojAccounts && data.ojAccounts.length > 0) {
          // 验证并过滤 OJ 账号
          const validAccounts = []
          const seenAccounts = new Set() // 用于检测重复

          for (const account of data.ojAccounts) {
            // 验证平台和账号名
            if (!account.platform) {
              this.$message.warning('请选择 OJ 平台')
              return
            }

            if (!account.accountName || !account.accountName.trim()) {
              this.$message.warning('OJ 账号名不能为空')
              return
            }

            const accountName = account.accountName.trim()
            const accountKey = `${account.platform}:${accountName}`

            // 检查重复
            if (seenAccounts.has(accountKey)) {
              this.$message.warning(`重复的 OJ 账号：${account.platform} - ${accountName}`)
              return
            }

            seenAccounts.add(accountKey)
            validAccounts.push({
              platform: account.platform,
              accountName: accountName
            })
          }

          data.ojAccounts = validAccounts
        } else {
          data.ojAccounts = []
        }

        // 标签数据保留在data中，让后端统一处理
        // data.tags 已经是标签ID数组，符合DTO要求

        if (this.isEdit) {
          // 编辑用户 - 使用新的DTO格式
          await updateUserByAdmin(data.id, data)
        } else {
          // 创建用户 - 使用新的DTO格式
          await createUser(data)
        }

        this.$message.success(this.isEdit ? '更新成功' : '创建成功')
        this.dialogVisible = false
        this.fetchData()
      } catch (error) {
        console.error('提交失败:', error)
        this.$message.error('提交失败')
      }
    },

    // ================== 标签管理相关方法 ==================
    handleTagManagement() {
      this.tagDialogVisible = true
    },
    handleAddTag() {
      this.isTagEdit = false
      this.resetTagForm()
      this.tagFormDialogVisible = true
    },
    handleEditTag(tag) {
      this.isTagEdit = true
      this.tagForm = {
        ...tag
      }
      this.tagFormDialogVisible = true
    },
    async handleDeleteTag(tag) {
      try {
        await this.$confirm(`确认要删除标签"${tag.name}"吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteUserTag(tag.id)
        this.$message.success('删除成功')
        this.fetchTags()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
          this.$message.error('删除失败')
        }
      }
    },
    resetTagForm() {
      this.tagForm = {
        name: '',
        color: '#409EFF',
        description: ''
      }
      if (this.$refs.tagForm) {
        this.$refs.tagForm.resetFields()
      }
    },
    async submitTagForm() {
      try {
        await this.$refs.tagForm.validate()
        if (this.isTagEdit) {
          await updateUserTag(this.tagForm.id, this.tagForm)
        } else {
          await createUserTag(this.tagForm)
        }
        this.$message.success(this.isTagEdit ? '更新成功' : '创建成功')
        this.tagFormDialogVisible = false
        this.fetchTags()
      } catch (error) {
        console.error('提交失败:', error)
        this.$message.error('提交失败')
      }
    },
    filterByTags() {
      // 这个方法会被计算属性自动调用，这里不需要实现
    },
    getTagUsageCount(tagId) {
      if (this.tagUsageMap[tagId] !== undefined) {
        return this.tagUsageMap[tagId]
      }
      // 这里可以异步获取使用统计，但为了简化，我们计算本地数据
      const count = this.list.filter(user =>
        user.tags && user.tags.some(tag => tag.id === tagId)
      ).length
      this.$set(this.tagUsageMap, tagId, count)
      return count
    },
    /**
     * 根据背景色计算合适的文本颜色（黑色或白色）
     */
    getTextColor(bgColor) {
      if (!bgColor) return '#000000' // 如果背景色无效，返回默认黑色

      // 确保颜色格式正确
      let color = bgColor.startsWith('#') ? bgColor.substring(1, 7) : bgColor
      if (color.length === 3) {
        color = color.split('').map(char => char + char).join('')
      }
      if (color.length !== 6) {
        return '#000000' // 格式不正确，返回默认黑色
      }

      const r = parseInt(color.substring(0, 2), 16)
      const g = parseInt(color.substring(2, 4), 16)
      const b = parseInt(color.substring(4, 6), 16)
      const brightness = (r * 299 + g * 587 + b * 114) / 1000
      return brightness > 125 ? '#000000' : '#FFFFFF'
    },
    /**
     * 判断当前用户是否可以编辑目标用户
     */
    canEditUser(user) {
      const currentUser = this.currentUser
      if (!currentUser) return false

      // 自己不能编辑自己
      if (currentUser.id === user.id) {
        return false
      }

      // 如果当前用户是超级管理员，可以编辑任何人
      if (this.isSuperAdmin) {
        return true
      }

      // 如果当前用户是管理员，可以编辑普通用户，但不能编辑其他管理员或超级管理员
      if (this.isAdmin) {
        return !user.admin && !user.superAdmin
      }

      // 普通用户不能编辑任何人
      return false
    },
    /**
     * 判断当前用户是否可以删除目标用户
     */
    canDeleteUser(user) {
      // 任何人都不能删除自己
      if (this.currentUser && this.currentUser.id === user.id) {
        return false
      }

      // 超级管理员可以删除任何人（除了自己）
      if (this.isSuperAdmin) {
        return true
      }

      // 管理员可以删除普通用户（但不能删除其他管理员或超级管理员）
      if (this.isAdmin) {
        return !user.admin && !user.superAdmin
      }

      // 普通用户不能删除任何人
      return false
    },
    canSetRole(role) {
      // 超级管理员可以设置所有角色
      if (this.isSuperAdmin) {
        return true
      }

      // 普通管理员不能设置管理员或超级管理员角色
      if (this.isAdmin && !this.isSuperAdmin) {
        return role === 'USER'
      }

      // 非管理员不能设置任何角色
      return false
    }
  }
}
</script>

<style lang="scss" scoped>
.user-management-container {
  padding: 20px;

  .controls {
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap; // 允许换行

    .search-input {
      min-width: 200px; // 设置最小宽度
    }

    // 响应式设计
    @media (max-width: 1200px) {
      .search-input {
        width: 200px !important;
      }

      .el-select {
        width: 220px !important;
      }
    }
  }

  .oj-account {
    display: flex;
    align-items: center;
    margin-bottom: 10px;

    // OJ账号验证错误样式
    .input-error {
      border-color: #f56c6c !important;

      &:focus {
        border-color: #f56c6c !important;
        box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.2) !important;
      }
    }
  }

  .table-wrapper {
    // 添加表格容器样式
    .el-table {
      // 优化表格样式
      .el-tag {
        margin: 2px;
      }

      // 禁用按钮样式
      .el-button.is-disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  }
}
</style>
