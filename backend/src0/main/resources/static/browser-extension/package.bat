@echo off
echo ========================================
echo Coding Tracker Token Helper 扩展打包工具
echo ========================================
echo.

echo 正在检查文件...
if not exist "manifest.json" (
    echo 错误: 找不到 manifest.json 文件
    pause
    exit /b 1
)

if not exist "popup.html" (
    echo 错误: 找不到 popup.html 文件
    pause
    exit /b 1
)

echo 文件检查完成！
echo.

echo 打包说明:
echo 1. 确保已安装Chrome浏览器
echo 2. 打开Chrome扩展管理页面 (chrome://extensions/)
echo 3. 开启"开发者模式"
echo 4. 点击"加载已解压的扩展程序"
echo 5. 选择当前文件夹
echo.

echo 或者使用以下方法打包为.crx文件:
echo 1. 在扩展管理页面点击"打包扩展程序"
echo 2. 选择当前文件夹作为扩展根目录
echo 3. 生成.crx文件用于分发
echo.

echo 当前目录包含以下文件:
dir /b *.json *.html *.js *.png *.svg 2>nul
echo.

echo 安装完成后，扩展将在以下网站自动工作:
echo - 洛谷 (www.luogu.com.cn)
echo - LeetCode (leetcode.com / leetcode.cn)
echo - AtCoder (atcoder.jp)
echo - TopCoder (www.topcoder.com)
echo - Virtual Judge (vjudge.net)
echo - SPOJ (www.spoj.com)
echo - CodeChef (www.codechef.com)
echo.

echo 使用方法:
echo 1. 访问支持的OJ平台并登录
echo 2. 点击浏览器工具栏的扩展图标
echo 3. 或者查看页面右上角的"获取Token"按钮
echo 4. 一键获取并复制Token到剪贴板
echo.

pause 