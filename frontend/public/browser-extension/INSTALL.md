# 🚀 Coding Tracker Token Helper 安装指南

## 📦 安装步骤

### 1. 下载扩展文件
- 从Token管理页面下载 `coding-tracker-token-helper.zip`
- 解压到本地文件夹（如：`D:\coding-tracker-extension`）

### 2. 打开Chrome扩展管理
- 在地址栏输入 `chrome://extensions/`
- 或通过菜单：更多工具 → 扩展程序

### 3. 开启开发者模式
- 在扩展管理页面右上角开启"开发者模式"开关

### 4. 加载扩展
- 点击"加载已解压的扩展程序"
- 选择解压后的文件夹

## ⚠️ 常见问题解决

### Service Worker 注册失败 (Status code: 15)

如果遇到此错误，请尝试以下解决方案：

#### 方案一：使用简化版本
1. 将 `manifest-simple.json` 重命名为 `manifest.json`
2. 将 `background-simple.js` 重命名为 `background.js`
3. 重新加载扩展

#### 方案二：检查权限
确保 `manifest.json` 包含必要权限：
```json
{
  "permissions": [
    "activeTab",
    "cookies", 
    "storage",
    "tabs",
    "alarms"
  ]
}
```

#### 方案三：重新安装
1. 完全卸载扩展
2. 重启Chrome浏览器
3. 重新加载扩展

### "Cannot read properties of undefined (reading 'create')" 错误

这通常是权限问题导致的：

1. **检查manifest.json权限**：确保包含所需权限
2. **使用简化版本**：如果问题持续，使用不需要额外权限的简化版本
3. **更新Chrome**：确保使用最新版本的Chrome浏览器

## 🎯 使用方法

### 方式一：扩展弹窗
1. 访问OJ平台并登录
2. 点击浏览器工具栏的扩展图标
3. 在弹窗中点击"获取Token"
4. Token自动复制到剪贴板

### 方式二：页面按钮
1. 访问OJ平台并登录
2. 页面右上角显示"🔑 Token"按钮
3. 点击按钮即可获取Token
4. Token自动复制到剪贴板

### 方式三：隐藏/显示切换
- 如果页面按钮干扰操作，点击上方小按钮（🔑）隐藏
- 再次点击可重新显示
- 按钮可拖拽到合适位置

## 🌟 支持的平台

- ✅ 洛谷 (LUOGU)
- ✅ LeetCode
- ✅ LeetCode中国
- ✅ AtCoder
- ✅ TopCoder
- ✅ Virtual Judge
- ✅ SPOJ
- ✅ CodeChef

## 🔧 故障排除

### 扩展无法加载
1. 检查文件完整性
2. 确保manifest.json格式正确
3. 查看Chrome开发者工具的错误信息

### Token获取失败
1. 确保已登录对应平台
2. 检查Cookie权限
3. 尝试刷新页面后重新获取

### 按钮不显示
1. 检查是否在支持的平台
2. 确保content script正确加载
3. 查看浏览器控制台错误信息

## 📞 技术支持

如果遇到其他问题：
1. 查看浏览器控制台错误信息
2. 检查扩展管理页面的错误提示
3. 尝试重新安装扩展
4. 联系开发者获取支持

## 🔒 隐私说明

- 扩展只在本地处理数据
- 不会上传Token到任何服务器
- 仅读取必要的Cookie信息
- 所有数据存储在本地浏览器中 