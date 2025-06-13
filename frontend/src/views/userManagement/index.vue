<template>
  <div class="user-management-container">
    <div class="controls">
      <el-input
        v-model="search"
        placeholder="搜索用户名或真实姓名"
        clearable
        prefix-icon="el-icon-search"
        class="search-input"
        style="margin-right: 16px;"
      />

      <el-button type="primary" icon="el-icon-plus" @click="handleAdd">
        添加用户
      </el-button>

      <el-button type="success" icon="el-icon-upload2" @click="showImportDialog">
        批量导入
      </el-button>

      <user-export />
    </div>

    <div class="table-wrapper">
      <el-table
        v-loading="loading"
        :data="filteredList"
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
        <el-table-column prop="roles" label="角色" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.admin" type="danger">管理员</el-tag>
            <el-tag v-else-if="row.acmer" type="success">ACMer</el-tag>
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
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              size="mini"
              :type="row.active ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.active ? '禁用' : '启用' }}
            </el-button>
            <el-button
              size="mini"
              type="danger"
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
        <el-form-item label="角色" prop="roles">
          <el-select v-model="userForm.roles" multiple style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="ACMer" value="ACMER" />
          </el-select>
        </el-form-item>
        <el-form-item label="OJ账号" prop="ojAccounts">
          <div v-for="(account, index) in userForm.ojAccounts" :key="index" class="oj-account">
            <el-select v-model="account.platform" placeholder="平台" style="width: 120px; margin-right: 10px">
              <el-option label="Luogu" value="LUOGU" />
              <el-option label="Codeforces" value="CODEFORCES" />
            </el-select>
            <el-input v-model="account.accountName" placeholder="账号名" style="width: 200px" />
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
  </div>
</template>

<script>
import { getUserList, createUser, updateUserByAdmin, deleteUser, toggleUserStatus } from '@/api/user'
import UserImport from './components/UserImport'
import UserExport from './components/UserExport'

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
      dialogVisible: false,
      importDialogVisible: false,
      isEdit: false,
      userForm: {
        username: '',
        realName: '',
        email: '',
        major: '',
        roles: [],
        ojAccounts: [],
        password: '',
        active: true
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
        ]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑用户' : '添加用户'
    },
    filteredList() {
      const kw = this.search.trim().toLowerCase()
      if (!kw) return this.list
      return this.list.filter(
        item =>
          (item.username && item.username.toLowerCase().includes(kw)) ||
          (item.realName && item.realName.toLowerCase().includes(kw))
      )
    }
  },
  created() {
    this.fetchData()
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
    handleAdd() {
      this.isEdit = false
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.isEdit = true
      this.userForm = {
        ...row,
        roles: row.roles || [],
        ojAccounts: row.ojAccounts || []
      }
      this.dialogVisible = true
    },
    async handleToggleStatus(row) {
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
        roles: [],
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
        if (this.isEdit) {
          await updateUserByAdmin(data.id, data)
        } else {
          await createUser(data)
        }
        this.$message.success(this.isEdit ? '更新成功' : '创建成功')
        this.dialogVisible = false
        this.fetchData()
      } catch (error) {
        console.error('提交失败:', error)
        this.$message.error('提交失败')
      }
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
  }

  .oj-account {
    display: flex;
    align-items: center;
    margin-bottom: 10px;
  }
}
</style>
