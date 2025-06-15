# 🔧 扩展安装指南

## 问题解决方案

如果遇到"解压显示格式错误"，请按以下步骤操作：

### 步骤1：生成图标文件

1. 打开 `create-temp-icons.html` 文件
2. 点击"生成图标"按钮
3. 下载生成的3个PNG文件：
   - icon16.png
   - icon48.png  
   - icon128.png
4. 将这些文件放到扩展目录中

### 步骤2：验证文件完整性

确保扩展目录包含以下文件：
```
browser-extension/
├── manifest.json          ✅ 必需
├── popup.html             ✅ 必需
├── popup.js               ✅ 必需
├── background.js          ✅ 必需
├── content.js             ✅ 必需
├── utils.js               ✅ 必需
├── icon16.png             ⚠️  可选（但推荐）
├── icon48.png             ⚠️  可选（但推荐）
├── icon128.png            ⚠️  可选（但推荐）
└── README.md              ℹ️  说明文件
```

### 步骤3：安装扩展

1. 打开Chrome浏览器
2. 地址栏输入：`chrome://extensions/`
3. 开启右上角的"开发者模式"
4. 点击"加载已解压的扩展程序"
5. 选择 `browser-extension` 文件夹
6. 确认安装

### 步骤4：测试扩展

1. 访问支持的OJ平台（如洛谷）
2. 确保已登录
3. 点击浏览器工具栏的扩展图标
4. 测试Token获取功能

## 常见问题

### Q: 提示"manifest.json格式错误"
A: 检查manifest.json文件是否完整，没有语法错误

### Q: 扩展加载后无法使用
A: 检查是否缺少必需的文件，特别是popup.html和background.js

### Q: 无法获取Token
A: 确保：
- 已登录对应的OJ平台
- 浏览器允许扩展访问Cookie
- 网络连接正常

### Q: 图标不显示
A: 图标文件是可选的，不影响功能。如需图标：
1. 使用 `create-temp-icons.html` 生成
2. 或者复制任意PNG图片并重命名

## 技术支持

如果仍有问题，请检查：
1. Chrome版本是否支持Manifest V3
2. 扩展权限是否正确授予
3. 浏览器控制台是否有错误信息

## 备用方案

如果扩展无法正常工作，可以：
1. 使用简化版manifest：`manifest-simple.json`
2. 手动复制Cookie（按F12 → Application → Cookies）
3. 联系技术支持获取帮助 