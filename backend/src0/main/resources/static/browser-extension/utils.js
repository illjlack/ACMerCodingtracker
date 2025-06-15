// 工具函数库
class TokenUtils {
    // 平台配置
    static PLATFORMS = {
        'www.luogu.com.cn': {
            name: 'LUOGU',
            displayName: '洛谷',
            cookies: ['__client_id', '_uid'],
            format: '__client_id={__client_id}; _uid={_uid}',
            loginCheck: () => document.querySelector('.user-nav, .user-dropdown, .lg-avatar')
        },
        'leetcode.com': {
            name: 'LEETCODE',
            displayName: 'LeetCode',
            cookies: ['csrftoken', 'LEETCODE_SESSION', 'sessionid'],
            format: 'csrftoken={csrftoken}; LEETCODE_SESSION={LEETCODE_SESSION}; sessionid={sessionid}',
            loginCheck: () => document.querySelector('[data-cy="user-menu"], .nav-user-icon-base')
        },
        'leetcode.cn': {
            name: 'LEETCODE_CN',
            displayName: 'LeetCode中国',
            cookies: ['csrftoken', 'LEETCODE_SESSION', 'sessionid'],
            format: 'csrftoken={csrftoken}; LEETCODE_SESSION={LEETCODE_SESSION}; sessionid={sessionid}',
            loginCheck: () => document.querySelector('[data-cy="user-menu"], .nav-user-icon-base')
        },
        'atcoder.jp': {
            name: 'ATCODER',
            displayName: 'AtCoder',
            cookies: ['REVEL_SESSION'],
            format: 'REVEL_SESSION={REVEL_SESSION}',
            loginCheck: () => document.querySelector('.username, .dropdown-toggle')
        },
        'www.topcoder.com': {
            name: 'TOPCODER',
            displayName: 'TopCoder',
            cookies: ['tcsso', 'tcjwt'],
            format: 'tcsso={tcsso}; tcjwt={tcjwt}',
            loginCheck: () => document.querySelector('.tc-avatar, .user-menu')
        },
        'vjudge.net': {
            name: 'VIRTUAL_JUDGE',
            displayName: 'Virtual Judge',
            cookies: ['JSESSIONID'],
            format: 'JSESSIONID={JSESSIONID}',
            loginCheck: () => document.querySelector('#username')
        },
        'www.spoj.com': {
            name: 'SPOJ',
            displayName: 'SPOJ',
            cookies: ['spoj_session'],
            format: 'spoj_session={spoj_session}',
            loginCheck: () => document.querySelector('.username')
        },
        'www.codechef.com': {
            name: 'CODECHEF',
            displayName: 'CodeChef',
            cookies: ['sessionid', 'csrftoken'],
            format: 'sessionid={sessionid}; csrftoken={csrftoken}',
            loginCheck: () => document.querySelector('.user-menu, .username')
        }
    };

    // 获取当前平台配置
    static getCurrentPlatform() {
        const hostname = window.location.hostname;
        return this.PLATFORMS[hostname] || null;
    }

    // 检查是否已登录
    static isLoggedIn(platform) {
        if (!platform || !platform.loginCheck) return false;
        return !!platform.loginCheck();
    }

    // 格式化Token
    static formatToken(platform, cookies) {
        if (!platform || !platform.format) return null;
        
        let tokenString = platform.format;
        Object.entries(cookies).forEach(([name, value]) => {
            tokenString = tokenString.replace(`{${name}}`, value);
        });
        
        // 检查是否所有占位符都被替换
        if (tokenString.includes('{') && tokenString.includes('}')) {
            return null;
        }
        
        return tokenString;
    }

    // 验证Token格式
    static validateTokenFormat(platform, token) {
        if (!platform || !token) return false;
        
        const requiredCookies = platform.cookies;
        for (const cookieName of requiredCookies) {
            if (!token.includes(cookieName + '=')) {
                return false;
            }
        }
        return true;
    }

    // 显示通知
    static showNotification(message, type = 'info', duration = 3000) {
        const notification = document.createElement('div');
        notification.className = 'token-helper-notification';
        notification.style.cssText = `
            position: fixed;
            top: 80px;
            right: 20px;
            z-index: 10001;
            padding: 12px 20px;
            border-radius: 6px;
            color: white;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            font-size: 14px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            transition: all 0.3s ease;
            max-width: 300px;
            word-wrap: break-word;
            backdrop-filter: blur(10px);
        `;
        
        const colors = {
            success: '#10b981',
            error: '#ef4444',
            warning: '#f59e0b',
            info: '#3b82f6'
        };
        
        notification.style.background = colors[type] || colors.info;
        notification.textContent = message;
        
        document.body.appendChild(notification);
        
        // 动画进入
        setTimeout(() => {
            notification.style.transform = 'translateX(0)';
            notification.style.opacity = '1';
        }, 10);
        
        // 自动消失
        setTimeout(() => {
            notification.style.opacity = '0';
            notification.style.transform = 'translateX(100%)';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, duration);
    }

    // 复制到剪贴板
    static async copyToClipboard(text) {
        try {
            await navigator.clipboard.writeText(text);
            return true;
        } catch (error) {
            // 降级方案
            const textArea = document.createElement('textarea');
            textArea.value = text;
            textArea.style.position = 'fixed';
            textArea.style.opacity = '0';
            document.body.appendChild(textArea);
            textArea.select();
            
            try {
                document.execCommand('copy');
                document.body.removeChild(textArea);
                return true;
            } catch (fallbackError) {
                document.body.removeChild(textArea);
                return false;
            }
        }
    }

    // 防抖函数
    static debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    // 节流函数
    static throttle(func, limit) {
        let inThrottle;
        return function() {
            const args = arguments;
            const context = this;
            if (!inThrottle) {
                func.apply(context, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    }

    // 存储管理
    static async setStorage(key, value) {
        try {
            await chrome.storage.local.set({ [key]: value });
            return true;
        } catch (error) {
            console.error('Storage set error:', error);
            return false;
        }
    }

    static async getStorage(key) {
        try {
            const result = await chrome.storage.local.get([key]);
            return result[key];
        } catch (error) {
            console.error('Storage get error:', error);
            return null;
        }
    }

    // 日志记录
    static log(level, message, data = null) {
        const timestamp = new Date().toISOString();
        const logMessage = `[${timestamp}] [${level.toUpperCase()}] ${message}`;
        
        if (data) {
            console[level](logMessage, data);
        } else {
            console[level](logMessage);
        }
        
        // 可以扩展为发送到后台或存储日志
    }
} 