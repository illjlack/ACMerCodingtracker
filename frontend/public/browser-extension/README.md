# Coding Tracker Token Helper 浏览器扩展

这是一个用于自动获取OJ平台Token的浏览器扩展，可以帮助用户快速获取登录Token用于Coding Tracker系统。

## 功能特性

- 🔍 **自动检测平台**：支持洛谷、LeetCode、AtCoder等多个OJ平台
- 🔑 **一键获取Token**：自动读取浏览器Cookie并格式化为Token
- 📋 **自动复制**：获取的Token自动复制到剪贴板
- 🔔 **智能提醒**：实时检测登录状态，提供操作提示
- 💾 **本地存储**：Token本地缓存，避免重复获取

## 支持的平台

- 洛谷 (LUOGU) - www.luogu.com.cn
- LeetCode - leetcode.com / leetcode.cn
- AtCoder - atcoder.jp
- TopCoder - www.topcoder.com
- Virtual Judge - vjudge.net
- SPOJ - www.spoj.com
- CodeChef - www.codechef.com

## 安装方法

### 方法一：开发者模式安装（推荐）

1. 打开Chrome浏览器，进入扩展管理页面：
   - 地址栏输入：`chrome://extensions/`
   - 或者：菜单 → 更多工具 → 扩展程序

2. 开启"开发者模式"（右上角开关）

3. 点击"加载已解压的扩展程序"

4. 选择本扩展的文件夹：`frontend/public/browser-extension/`

5. 扩展安装完成，会在浏览器工具栏显示图标

### 方法二：打包安装

1. 在扩展管理页面点击"打包扩展程序"
2. 选择扩展目录，生成.crx文件
3. 拖拽.crx文件到扩展管理页面进行安装

## 使用方法

### 方式一：扩展弹窗

1. 访问任意支持的OJ平台并登录
2. 点击浏览器工具栏中的扩展图标
3. 在弹出窗口中点击对应平台的"获取Token"按钮
4. Token会自动复制到剪贴板

### 方式二：页面内按钮

1. 访问支持的OJ平台并登录
2. 页面右上角会自动显示"🔑 获取Token"按钮
3. 点击按钮即可获取并复制Token

### 方式三：批量获取

1. 在扩展弹窗中点击"刷新检测"
2. 点击"复制所有"获取所有已登录平台的Token

## Token格式说明

不同平台的Token格式如下：

- **洛谷**: `__client_id=xxx; _uid=xxx`
- **LeetCode**: `csrftoken=xxx; LEETCODE_SESSION=xxx; sessionid=xxx`
- **AtCoder**: `REVEL_SESSION=xxx`
- **TopCoder**: `tcsso=xxx; tcjwt=xxx`
- **Virtual Judge**: `JSESSIONID=xxx`
- **SPOJ**: `spoj_session=xxx`
- **CodeChef**: `sessionid=xxx; csrftoken=xxx`

## 注意事项

1. **登录状态**：使用前请确保已登录对应的OJ平台
2. **Cookie权限**：扩展需要读取Cookie权限，这是获取Token的必要条件
3. **安全性**：Token包含敏感信息，请妥善保管，不要分享给他人
4. **有效期**：Token有时效性，失效后需要重新获取
5. **隐私保护**：扩展只在本地处理数据，不会上传到任何服务器

## 故障排除

### Token获取失败

1. 确认已正确登录对应平台
2. 刷新页面后重试
3. 检查浏览器是否阻止了Cookie访问
4. 尝试重新安装扩展

### 扩展无法加载

1. 确认Chrome版本支持Manifest V3
2. 检查扩展文件是否完整
3. 查看扩展管理页面的错误信息
4. 尝试重新加载扩展

### 按钮不显示

1. 确认当前页面是支持的OJ平台
2. 检查页面是否完全加载
3. 尝试刷新页面
4. 查看浏览器控制台是否有错误

## 开发说明

### 文件结构

```
browser-extension/
├── manifest.json      # 扩展配置文件
├── popup.html        # 弹窗页面
├── popup.js          # 弹窗逻辑
├── background.js     # 后台脚本
├── content.js        # 内容脚本
├── icon16.png        # 16x16图标
├── icon48.png        # 48x48图标
├── icon128.png       # 128x128图标
└── README.md         # 说明文档
```

### 权限说明

- `activeTab`: 访问当前活动标签页
- `cookies`: 读取Cookie信息
- `storage`: 本地存储Token
- `scripting`: 注入脚本到页面

### 自定义配置

如需添加新的OJ平台支持，请修改以下文件：

1. `popup.js` - 添加平台配置
2. `background.js` - 添加Cookie获取逻辑
3. `content.js` - 添加页面检测逻辑

## 版本历史

- v1.0 - 初始版本，支持8个主流OJ平台

## 技术支持

如有问题或建议，请联系开发团队或在项目仓库提交Issue。 