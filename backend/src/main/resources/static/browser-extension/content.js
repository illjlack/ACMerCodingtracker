// 内容脚本 - 在网页中运行，检测登录状态和提供Token获取功能

(function() {
    'use strict';
    
    // 平台检测配置
    const PLATFORM_CONFIGS = {
        'www.luogu.com.cn': {
            name: 'LUOGU',
            displayName: '洛谷',
            loginSelectors: ['.user-nav', '.user-dropdown', '.lg-avatar'],
            logoutSelectors: ['.login-button', '.lg-btn-login']
        },
        'leetcode.com': {
            name: 'LEETCODE',
            displayName: 'LeetCode',
            loginSelectors: ['[data-cy="user-menu"]', '.nav-user-icon-base'],
            logoutSelectors: ['[data-cy="sign-in-btn"]', '.nav-signin-btn']
        },
        'leetcode.cn': {
            name: 'LEETCODE_CN',
            displayName: 'LeetCode中国',
            loginSelectors: ['[data-cy="user-menu"]', '.nav-user-icon-base'],
            logoutSelectors: ['[data-cy="sign-in-btn"]', '.nav-signin-btn']
        },
        'atcoder.jp': {
            name: 'ATCODER',
            displayName: 'AtCoder',
            loginSelectors: ['.username', '.dropdown-toggle'],
            logoutSelectors: ['.login', 'a[href*="login"]']
        },
        'www.topcoder.com': {
            name: 'TOPCODER',
            displayName: 'TopCoder',
            loginSelectors: ['.tc-avatar', '.user-menu'],
            logoutSelectors: ['.tc-btn-login', 'a[href*="login"]']
        },
        'vjudge.net': {
            name: 'VIRTUAL_JUDGE',
            displayName: 'Virtual Judge',
            loginSelectors: ['.dropdown-toggle:contains("User")', '#username'],
            logoutSelectors: ['a[href*="login"]', '.login-link']
        },
        'www.spoj.com': {
            name: 'SPOJ',
            displayName: 'SPOJ',
            loginSelectors: ['.username', 'a[href*="users"]'],
            logoutSelectors: ['a[href*="login"]', '.login']
        },
        'www.codechef.com': {
            name: 'CODECHEF',
            displayName: 'CodeChef',
            loginSelectors: ['.user-menu', '.username'],
            logoutSelectors: ['a[href*="login"]', '.login-link']
        }
    };
    
    let currentPlatform = null;
    let isLoggedIn = false;
    let tokenHelperButton = null;
    
    // 初始化
    function init() {
        detectPlatform();
        if (currentPlatform) {
            checkLoginStatus();
            createTokenHelperButton();
            
            // 监听页面变化
            const observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(mutation) {
                    if (mutation.type === 'childList') {
                        checkLoginStatus();
                        updateTokenHelperButton();
                    }
                });
            });
            
            observer.observe(document.body, {
                childList: true,
                subtree: true
            });
        }
    }
    
    // 检测当前平台
    function detectPlatform() {
        const hostname = window.location.hostname;
        currentPlatform = PLATFORM_CONFIGS[hostname];
        
        if (!currentPlatform) {
            // 尝试匹配子域名
            for (const [domain, config] of Object.entries(PLATFORM_CONFIGS)) {
                if (hostname.includes(domain.replace('www.', ''))) {
                    currentPlatform = config;
                    break;
                }
            }
        }
    }
    
    // 检查登录状态
    function checkLoginStatus() {
        if (!currentPlatform) return;
        
        const wasLoggedIn = isLoggedIn;
        isLoggedIn = false;
        
        // 检查登录状态的选择器
        for (const selector of currentPlatform.loginSelectors) {
            if (document.querySelector(selector)) {
                isLoggedIn = true;
                break;
            }
        }
        
        // 如果没有找到登录元素，检查是否有登出元素
        if (!isLoggedIn) {
            let hasLogoutElements = false;
            for (const selector of currentPlatform.logoutSelectors) {
                if (document.querySelector(selector)) {
                    hasLogoutElements = true;
                    break;
                }
            }
            isLoggedIn = !hasLogoutElements;
        }
        
        // 如果登录状态发生变化，更新按钮
        if (wasLoggedIn !== isLoggedIn) {
            updateTokenHelperButton();
        }
    }
    
    // 创建Token获取按钮
    function createTokenHelperButton() {
        if (tokenHelperButton) return;
        
        tokenHelperButton = document.createElement('div');
        tokenHelperButton.id = 'coding-tracker-token-helper';
        tokenHelperButton.innerHTML = `
            <div style="
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 10000;
                background: #409eff;
                color: white;
                padding: 10px 15px;
                border-radius: 5px;
                cursor: pointer;
                font-family: Arial, sans-serif;
                font-size: 14px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.2);
                transition: all 0.3s ease;
                display: none;
            " onmouseover="this.style.background='#66b1ff'" onmouseout="this.style.background='#409eff'">
                <div style="display: flex; align-items: center;">
                    <span style="margin-right: 8px;">🔑</span>
                    <span>获取Token</span>
                </div>
            </div>
        `;
        
        document.body.appendChild(tokenHelperButton);
        
        // 添加点击事件
        tokenHelperButton.addEventListener('click', function() {
            getTokenForCurrentPlatform();
        });
        
        updateTokenHelperButton();
    }
    
    // 更新Token按钮状态
    function updateTokenHelperButton() {
        if (!tokenHelperButton) return;
        
        const button = tokenHelperButton.querySelector('div');
        if (isLoggedIn) {
            button.style.display = 'block';
            button.style.background = '#409eff';
            button.querySelector('span:last-child').textContent = '获取Token';
        } else {
            button.style.display = 'block';
            button.style.background = '#f56c6c';
            button.querySelector('span:last-child').textContent = '请先登录';
        }
    }
    
    // 获取当前平台的Token
    async function getTokenForCurrentPlatform() {
        if (!currentPlatform || !isLoggedIn) {
            showNotification('请先登录' + currentPlatform.displayName, 'error');
            return;
        }
        
        try {
            showNotification('正在获取Token...', 'info');
            
            // 发送消息给background script获取cookies
            const response = await new Promise((resolve) => {
                chrome.runtime.sendMessage({
                    action: 'getTokens',
                    domain: window.location.hostname,
                    platform: currentPlatform.name
                }, resolve);
            });
            
            if (response.success && response.tokens) {
                const tokenString = formatToken(response.tokens);
                
                if (tokenString) {
                    // 复制到剪贴板
                    await navigator.clipboard.writeText(tokenString);
                    showNotification('Token已复制到剪贴板', 'success');
                    
                    // 保存到扩展存储
                    chrome.runtime.sendMessage({
                        action: 'saveToken',
                        platform: currentPlatform.name,
                        token: tokenString
                    });
                } else {
                    showNotification('未找到有效的Token', 'error');
                }
            } else {
                showNotification('获取Token失败: ' + (response.error || '未知错误'), 'error');
            }
        } catch (error) {
            console.error('获取Token失败:', error);
            showNotification('获取Token失败: ' + error.message, 'error');
        }
    }
    
    // 格式化Token
    function formatToken(tokens) {
        const formats = {
            'LUOGU': '__client_id={__client_id}; _uid={_uid}',
            'LEETCODE': 'csrftoken={csrftoken}; LEETCODE_SESSION={LEETCODE_SESSION}; sessionid={sessionid}',
            'LEETCODE_CN': 'csrftoken={csrftoken}; LEETCODE_SESSION={LEETCODE_SESSION}; sessionid={sessionid}',
            'ATCODER': 'REVEL_SESSION={REVEL_SESSION}',
            'TOPCODER': 'tcsso={tcsso}; tcjwt={tcjwt}',
            'VIRTUAL_JUDGE': 'JSESSIONID={JSESSIONID}',
            'SPOJ': 'spoj_session={spoj_session}',
            'CODECHEF': 'sessionid={sessionid}; csrftoken={csrftoken}'
        };
        
        const format = formats[currentPlatform.name];
        if (!format) return null;
        
        let tokenString = format;
        Object.entries(tokens).forEach(([name, value]) => {
            tokenString = tokenString.replace(`{${name}}`, value);
        });
        
        // 检查是否所有占位符都被替换
        if (tokenString.includes('{') && tokenString.includes('}')) {
            return null; // 还有未替换的占位符，说明缺少必要的cookie
        }
        
        return tokenString;
    }
    
    // 显示通知
    function showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.style.cssText = `
            position: fixed;
            top: 80px;
            right: 20px;
            z-index: 10001;
            padding: 12px 20px;
            border-radius: 5px;
            color: white;
            font-family: Arial, sans-serif;
            font-size: 14px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.2);
            transition: all 0.3s ease;
            max-width: 300px;
            word-wrap: break-word;
        `;
        
        switch (type) {
            case 'success':
                notification.style.background = '#67c23a';
                break;
            case 'error':
                notification.style.background = '#f56c6c';
                break;
            case 'warning':
                notification.style.background = '#e6a23c';
                break;
            default:
                notification.style.background = '#409eff';
        }
        
        notification.textContent = message;
        document.body.appendChild(notification);
        
        // 3秒后自动消失
        setTimeout(() => {
            notification.style.opacity = '0';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, 3000);
    }
    
    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
    
    // 监听来自popup的消息
    chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {
        if (request.action === 'getCurrentPlatformInfo') {
            sendResponse({
                platform: currentPlatform,
                isLoggedIn: isLoggedIn,
                url: window.location.href
            });
        }
        
        if (request.action === 'getTokenFromPage') {
            getTokenForCurrentPlatform().then(() => {
                sendResponse({ success: true });
            }).catch(error => {
                sendResponse({ success: false, error: error.message });
            });
            return true; // 保持消息通道开放
        }
    });
    
})(); 