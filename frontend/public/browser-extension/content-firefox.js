// 火狐浏览器兼容的内容脚本 - 在网页中运行，检测登录状态和提供Token获取功能

(function() {
    'use strict';
    
    // 兼容性处理 - 统一API
    const browserAPI = typeof browser !== 'undefined' ? browser : chrome;
    
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
    let isDragging = false;
    let isProcessing = false;
    let isHidden = false;
    let dragStartTime = 0;
    
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
        
        // 创建样式
        const style = document.createElement('style');
        style.textContent = `
            #coding-tracker-token-helper {
                position: fixed;
                top: 120px;
                right: 10px;
                z-index: 2147483647;
                background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%);
                color: white;
                padding: 8px 12px;
                border-radius: 20px;
                cursor: pointer;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                font-size: 12px;
                font-weight: 500;
                box-shadow: 0 2px 10px rgba(255, 107, 53, 0.3);
                transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                user-select: none;
                backdrop-filter: blur(10px);
                border: 1px solid rgba(255,255,255,0.2);
                display: none;
                pointer-events: auto;
                opacity: 0.7;
                min-width: 80px;
                text-align: center;
                transform: scale(0.9);
            }
            
            #coding-tracker-token-helper:hover {
                transform: scale(1) translateY(-1px);
                box-shadow: 0 4px 15px rgba(255, 107, 53, 0.4);
                opacity: 1;
            }
            
            #coding-tracker-token-helper:active {
                transform: scale(0.95);
                transition: transform 0.1s;
            }
            
            #coding-tracker-token-helper.logged-out {
                background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
                box-shadow: 0 2px 10px rgba(255, 107, 107, 0.3);
            }
            
            #coding-tracker-token-helper.logged-out:hover {
                box-shadow: 0 4px 15px rgba(255, 107, 107, 0.4);
            }
            
            #coding-tracker-token-helper.processing {
                background: linear-gradient(135deg, #ffa726 0%, #fb8c00 100%);
                animation: pulse 1.5s infinite;
                pointer-events: none;
            }
            
            #coding-tracker-token-helper.success {
                background: linear-gradient(135deg, #66bb6a 0%, #43a047 100%);
                animation: bounce 0.6s ease-out;
            }
            
            #coding-tracker-token-helper.hidden {
                opacity: 0.2;
                transform: scale(0.8);
                pointer-events: none;
            }
            
            @keyframes pulse {
                0%, 100% { 
                    transform: scale(0.9);
                    opacity: 0.7;
                }
                50% { 
                    transform: scale(0.95);
                    opacity: 0.9;
                }
            }
            
            @keyframes bounce {
                0%, 20%, 60%, 100% {
                    transform: scale(1) translateY(0);
                }
                40% {
                    transform: scale(1) translateY(-8px);
                }
                80% {
                    transform: scale(1) translateY(-4px);
                }
            }
            
            .token-btn-content {
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 4px;
            }
            
            .token-btn-icon {
                font-size: 14px;
            }
            
            .token-btn-text {
                font-weight: 600;
                font-size: 11px;
            }
            
            /* 隐藏/显示切换按钮 */
            #coding-tracker-toggle {
                position: fixed;
                top: 90px;
                right: 10px;
                z-index: 2147483646;
                background: rgba(255,107,53,0.8);
                color: white;
                padding: 4px 6px;
                border-radius: 10px;
                cursor: pointer;
                font-size: 10px;
                opacity: 0.3;
                transition: opacity 0.3s;
                user-select: none;
                pointer-events: auto;
            }
            
            #coding-tracker-toggle:hover {
                opacity: 0.8;
            }
            
            #coding-tracker-toggle::after {
                content: ' Firefox';
                font-size: 8px;
                opacity: 0.7;
            }
        `;
        document.head.appendChild(style);
        
        // 创建切换按钮
        const toggleButton = document.createElement('div');
        toggleButton.id = 'coding-tracker-toggle';
        toggleButton.textContent = '🦊';
        toggleButton.title = '切换Token助手显示/隐藏 (Firefox版本)';
        document.body.appendChild(toggleButton);
        
        // 创建主按钮
        tokenHelperButton = document.createElement('div');
        tokenHelperButton.id = 'coding-tracker-token-helper';
        tokenHelperButton.innerHTML = `
            <div class="token-btn-content">
                <span class="token-btn-icon">🦊</span>
                <span class="token-btn-text">Token</span>
            </div>
        `;
        
        document.body.appendChild(tokenHelperButton);
        
        // 添加事件监听
        setupButtonEvents();
        setupToggleEvents(toggleButton);
        updateTokenHelperButton();
    }
    
    // 设置切换按钮事件
    function setupToggleEvents(toggleButton) {
        toggleButton.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            e.stopImmediatePropagation();
            
            isHidden = !isHidden;
            if (isHidden) {
                tokenHelperButton.classList.add('hidden');
                toggleButton.textContent = '👁️';
                toggleButton.title = '显示Token助手 (Firefox版本)';
            } else {
                tokenHelperButton.classList.remove('hidden');
                toggleButton.textContent = '🦊';
                toggleButton.title = '隐藏Token助手 (Firefox版本)';
            }
        }, true);
        
        // 完全隔离切换按钮的事件
        ['mousedown', 'mouseup', 'mousemove', 'touchstart', 'touchend', 'touchmove'].forEach(eventType => {
            toggleButton.addEventListener(eventType, function(e) {
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation();
            }, true);
        });
    }
    
    // 设置按钮事件
    function setupButtonEvents() {
        let startX, startY, startRight, startTop;
        
        // 完全隔离所有鼠标事件
        ['mousedown', 'mouseup', 'mousemove', 'click', 'dblclick', 'contextmenu'].forEach(eventType => {
            tokenHelperButton.addEventListener(eventType, function(e) {
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation();
            }, true);
        });
        
        // 点击事件 - 使用单独的处理器
        tokenHelperButton.addEventListener('click', function(e) {
            if (!isDragging && !isProcessing && !isHidden) {
                const clickTime = Date.now();
                // 确保不是拖拽结束后的误触
                if (clickTime - dragStartTime > 200) {
                    getTokenForCurrentPlatform();
                }
            }
        }, true);
        
        // 拖拽事件
        tokenHelperButton.addEventListener('mousedown', function(e) {
            if (isHidden) return;
            
            isDragging = false;
            dragStartTime = Date.now();
            startX = e.clientX;
            startY = e.clientY;
            startRight = parseInt(window.getComputedStyle(tokenHelperButton).right);
            startTop = parseInt(window.getComputedStyle(tokenHelperButton).top);
            
            const onMouseMove = function(e) {
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation();
                
                const deltaX = startX - e.clientX;
                const deltaY = e.clientY - startY;
                
                // 如果移动距离超过3px，认为是拖拽
                if (Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3) {
                    isDragging = true;
                    tokenHelperButton.style.cursor = 'grabbing';
                    
                    const newRight = Math.max(5, Math.min(window.innerWidth - 100, startRight + deltaX));
                    const newTop = Math.max(5, Math.min(window.innerHeight - 40, startTop + deltaY));
                    
                    tokenHelperButton.style.right = newRight + 'px';
                    tokenHelperButton.style.top = newTop + 'px';
                }
            };
            
            const onMouseUp = function(e) {
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation();
                
                document.removeEventListener('mousemove', onMouseMove, true);
                document.removeEventListener('mouseup', onMouseUp, true);
                
                tokenHelperButton.style.cursor = 'pointer';
                
                // 延迟重置拖拽状态
                setTimeout(() => {
                    isDragging = false;
                }, 150);
            };
            
            document.addEventListener('mousemove', onMouseMove, true);
            document.addEventListener('mouseup', onMouseUp, true);
        }, true);
    }
    
    // 更新Token按钮状态
    function updateTokenHelperButton() {
        if (!tokenHelperButton) return;
        
        const textElement = tokenHelperButton.querySelector('.token-btn-text');
        
        tokenHelperButton.classList.remove('logged-out', 'processing', 'success');
        
        if (isLoggedIn) {
            tokenHelperButton.style.display = 'block';
            textElement.textContent = 'Token';
        } else {
            tokenHelperButton.style.display = 'block';
            tokenHelperButton.classList.add('logged-out');
            textElement.textContent = '登录';
        }
    }
    
    // 获取当前平台的Token
    async function getTokenForCurrentPlatform() {
        if (!currentPlatform) {
            showNotification('不支持的平台', 'error');
            return;
        }
        
        if (!isLoggedIn) {
            showNotification(`请先登录${currentPlatform.displayName}`, 'warning');
            return;
        }
        
        if (isProcessing) return;
        
        isProcessing = true;
        const textElement = tokenHelperButton.querySelector('.token-btn-text');
        const originalText = textElement.textContent;
        
        try {
            // 更新按钮状态
            tokenHelperButton.classList.add('processing');
            textElement.textContent = '获取中';
            
            showNotification('正在获取Token...', 'info', 2000);
            
            // 发送消息给background script获取cookies
            const response = await new Promise((resolve) => {
                browserAPI.runtime.sendMessage({
                    action: 'getTokens',
                    domain: window.location.hostname,
                    platform: currentPlatform.name
                }, resolve);
            });
            
            if (response && response.success && response.tokens) {
                const tokenString = formatToken(response.tokens);
                
                if (tokenString) {
                    // 复制到剪贴板
                    await navigator.clipboard.writeText(tokenString);
                    
                    // 成功状态
                    tokenHelperButton.classList.remove('processing');
                    tokenHelperButton.classList.add('success');
                    textElement.textContent = '已复制';
                    
                    showNotification('Token已复制到剪贴板！', 'success');
                    
                    // 保存到扩展存储
                    browserAPI.runtime.sendMessage({
                        action: 'saveToken',
                        platform: currentPlatform.name,
                        token: tokenString
                    });
                    
                    // 2秒后恢复原状
                    setTimeout(() => {
                        tokenHelperButton.classList.remove('success');
                        textElement.textContent = originalText;
                    }, 2000);
                } else {
                    throw new Error('Token格式化失败');
                }
            } else {
                throw new Error(response?.error || '获取Token失败');
            }
        } catch (error) {
            console.error('获取Token失败:', error);
            
            // 错误状态
            tokenHelperButton.classList.remove('processing');
            textElement.textContent = '失败';
            
            showNotification('获取Token失败: ' + error.message, 'error');
            
            // 2秒后恢复原状
            setTimeout(() => {
                textElement.textContent = originalText;
            }, 2000);
        } finally {
            isProcessing = false;
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
            return null;
        }
        
        return tokenString;
    }
    
    // 显示通知
    function showNotification(message, type = 'info', duration = 3000) {
        const notification = document.createElement('div');
        notification.style.cssText = `
            position: fixed;
            top: 50px;
            right: 20px;
            z-index: 2147483645;
            padding: 10px 16px;
            border-radius: 6px;
            color: white;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            font-size: 13px;
            font-weight: 500;
            box-shadow: 0 3px 15px rgba(0,0,0,0.2);
            transition: all 0.3s ease;
            max-width: 280px;
            word-wrap: break-word;
            backdrop-filter: blur(10px);
            border: 1px solid rgba(255,255,255,0.2);
            transform: translateX(100%);
            opacity: 0;
            pointer-events: none;
        `;
        
        const colors = {
            success: 'linear-gradient(135deg, #66bb6a 0%, #43a047 100%)',
            error: 'linear-gradient(135deg, #ef5350 0%, #d32f2f 100%)',
            warning: 'linear-gradient(135deg, #ffa726 0%, #fb8c00 100%)',
            info: 'linear-gradient(135deg, #ff6b35 0%, #f7931e 100%)'
        };
        
        notification.style.background = colors[type] || colors.info;
        notification.textContent = message + ' 🦊';
        
        document.body.appendChild(notification);
        
        // 动画进入
        setTimeout(() => {
            notification.style.transform = 'translateX(0)';
            notification.style.opacity = '1';
        }, 10);
        
        // 自动消失
        setTimeout(() => {
            notification.style.transform = 'translateX(100%)';
            notification.style.opacity = '0';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, duration);
    }
    
    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
    
    // 监听来自popup的消息
    browserAPI.runtime.onMessage.addListener(function(request, sender, sendResponse) {
        if (request.action === 'getCurrentPlatformInfo') {
            sendResponse({
                platform: currentPlatform,
                isLoggedIn: isLoggedIn,
                url: window.location.href,
                browser: 'firefox'
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