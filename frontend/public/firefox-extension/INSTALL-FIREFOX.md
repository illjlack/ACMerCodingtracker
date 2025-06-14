# 🦊 Firefox 浏览器扩展安装指南

## 📦 Firefox 版本特点

- 🦊 专为 Firefox 浏览器优化
- 🔶 橙色主题设计，符合 Firefox 风格
- 🔧 使用 Manifest V2 格式
- 🌐 兼容 Firefox 57+ 版本

## 📥 安装步骤

### 方法一：开发者模式安装（推荐）

1. **准备文件**
   - 下载扩展文件并解压
   - 将 `manifest-firefox.json` 重命名为 `manifest.json`
   - 将 `background-firefox.js` 重命名为 `background.js`
   - 将 `content-firefox.js` 重命名为 `content.js`

2. **打开 Firefox 扩展管理**
   - 在地址栏输入 `about:debugging`
   - 点击左侧的"此 Firefox"

3. **加载临时扩展**
   - 点击"临时载入附加组件"
   - 选择扩展文件夹中的 `manifest.json` 文件

4. **确认安装**
   - 扩展会出现在已安装的扩展列表中
   - 工具栏会显示扩展图标

## 🎯 使用方法

### 页面按钮功能
- 🦊 橙色的狐狸图标按钮
- 📍 位置：页面右上角
- 🎛️ 切换按钮：可隐藏/显示主按钮
- 🖱️ 可拖拽到合适位置

### 获取 Token
1. 访问支持的 OJ 平台并登录
2. 点击页面上的 🦊 按钮
3. Token 自动复制到剪贴板
4. 粘贴到 Token 管理页面

## 🌟 支持的平台

- ✅ 洛谷 (LUOGU)
- ✅ LeetCode
- ✅ LeetCode中国
- ✅ AtCoder
- ✅ TopCoder
- ✅ Virtual Judge
- ✅ SPOJ
- ✅ CodeChef

## ⚠️ 注意事项

### Firefox 特殊要求
1. **临时安装**：扩展在重启后需要重新加载
2. **权限确认**：某些权限需要用户手动确认
3. **API 兼容**：使用 `browser` API 确保兼容性

### 已知限制
- 临时安装的扩展在重启后失效
- 需要手动重新加载扩展
- 某些高级功能可能受限

## 🔧 故障排除

### 扩展无法加载
1. 检查 manifest.json 格式是否正确
2. 确认所有必需文件都已重命名
3. 查看 about:debugging 页面的错误信息

### Token 获取失败
1. 检查网站权限设置
2. 确认已登录对应平台
3. 尝试刷新页面后重试

## 🆚 与 Chrome 版本的区别

| 功能 | Chrome 版本 | Firefox 版本 |
|------|-------------|--------------|
| 主题色 | 蓝紫色 | 橙色 |
| 图标 | 🔑 | 🦊 |
| Manifest | V3 | V2 |
| 安装方式 | 开发者模式 | 临时载入 |
| 持久性 | 永久 | 需重新加载 |

## 📞 获取帮助

如果遇到问题：
1. 查看 Firefox 开发者工具控制台
2. 检查 about:debugging 页面的错误信息
3. 确认文件重命名是否正确
4. 联系开发者获取技术支持
