// 后台脚本 - 处理扩展的生命周期和消息传递

// 安装时的初始化
chrome.runtime.onInstalled.addListener(function(details) {
    console.log('Coding Tracker Token Helper 已安装');
    
    if (details.reason === 'install') {
        // 首次安装时的逻辑
        chrome.storage.local.set({
            'extension_installed': true,
            'install_time': Date.now()
        });
    }
});

// 监听来自content script的消息
chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {
    if (request.action === 'getTokens') {
        handleGetTokens(request, sendResponse);
        return true; // 保持消息通道开放
    }
    
    if (request.action === 'saveToken') {
        handleSaveToken(request, sendResponse);
        return true;
    }
    
    if (request.action === 'checkLoginStatus') {
        handleCheckLoginStatus(request, sendResponse);
        return true;
    }
});

// 处理获取Token请求
async function handleGetTokens(request, sendResponse) {
    try {
        const domain = request.domain;
        const platform = request.platform;
        
        // 根据平台获取相应的cookies
        const tokens = await getTokensForPlatform(domain, platform);
        
        sendResponse({
            success: true,
            tokens: tokens,
            platform: platform
        });
    } catch (error) {
        console.error('获取Token失败:', error);
        sendResponse({
            success: false,
            error: error.message
        });
    }
}

// 处理保存Token请求
async function handleSaveToken(request, sendResponse) {
    try {
        const { platform, token } = request;
        
        await chrome.storage.local.set({
            [`token_${platform}`]: token,
            [`token_${platform}_timestamp`]: Date.now()
        });
        
        sendResponse({
            success: true,
            message: 'Token保存成功'
        });
    } catch (error) {
        console.error('保存Token失败:', error);
        sendResponse({
            success: false,
            error: error.message
        });
    }
}

// 检查登录状态
async function handleCheckLoginStatus(request, sendResponse) {
    try {
        const domain = request.domain;
        const isLoggedIn = await checkIfLoggedIn(domain);
        
        sendResponse({
            success: true,
            isLoggedIn: isLoggedIn
        });
    } catch (error) {
        console.error('检查登录状态失败:', error);
        sendResponse({
            success: false,
            error: error.message
        });
    }
}

// 根据平台获取Token
async function getTokensForPlatform(domain, platform) {
    const platformConfigs = {
        'LUOGU': {
            domain: 'www.luogu.com.cn',
            cookies: ['__client_id', '_uid']
        },
        'LEETCODE': {
            domain: 'leetcode.com',
            cookies: ['csrftoken', 'LEETCODE_SESSION', 'sessionid']
        },
        'LEETCODE_CN': {
            domain: 'leetcode.cn',
            cookies: ['csrftoken', 'LEETCODE_SESSION', 'sessionid']
        },
        'ATCODER': {
            domain: 'atcoder.jp',
            cookies: ['REVEL_SESSION']
        },
        'TOPCODER': {
            domain: 'www.topcoder.com',
            cookies: ['tcsso', 'tcjwt']
        },
        'VIRTUAL_JUDGE': {
            domain: 'vjudge.net',
            cookies: ['JSESSIONID']
        },
        'SPOJ': {
            domain: 'www.spoj.com',
            cookies: ['spoj_session']
        },
        'CODECHEF': {
            domain: 'www.codechef.com',
            cookies: ['sessionid', 'csrftoken']
        }
    };
    
    const config = platformConfigs[platform];
    if (!config) {
        throw new Error(`不支持的平台: ${platform}`);
    }
    
    const tokens = {};
    const url = `https://${config.domain}`;
    
    for (const cookieName of config.cookies) {
        try {
            const cookie = await chrome.cookies.get({
                url: url,
                name: cookieName
            });
            
            if (cookie && cookie.value) {
                tokens[cookieName] = cookie.value;
            }
        } catch (error) {
            console.warn(`获取Cookie ${cookieName} 失败:`, error);
        }
    }
    
    return tokens;
}

// 检查是否已登录
async function checkIfLoggedIn(domain) {
    try {
        // 简单的登录检查 - 检查是否有关键的认证cookie
        const authCookies = {
            'www.luogu.com.cn': ['_uid'],
            'leetcode.com': ['LEETCODE_SESSION'],
            'leetcode.cn': ['LEETCODE_SESSION'],
            'atcoder.jp': ['REVEL_SESSION'],
            'www.topcoder.com': ['tcsso'],
            'vjudge.net': ['JSESSIONID'],
            'www.spoj.com': ['spoj_session'],
            'www.codechef.com': ['sessionid']
        };
        
        const requiredCookies = authCookies[domain];
        if (!requiredCookies) return false;
        
        for (const cookieName of requiredCookies) {
            const cookie = await chrome.cookies.get({
                url: `https://${domain}`,
                name: cookieName
            });
            
            if (cookie && cookie.value) {
                return true;
            }
        }
        
        return false;
    } catch (error) {
        console.error('检查登录状态失败:', error);
        return false;
    }
}

// 定期清理过期的Token
chrome.alarms.create('cleanupTokens', { periodInMinutes: 60 });

chrome.alarms.onAlarm.addListener(function(alarm) {
    if (alarm.name === 'cleanupTokens') {
        cleanupExpiredTokens();
    }
});

async function cleanupExpiredTokens() {
    try {
        const storage = await chrome.storage.local.get();
        const now = Date.now();
        const expireTime = 24 * 60 * 60 * 1000; // 24小时
        
        const keysToRemove = [];
        
        Object.keys(storage).forEach(key => {
            if (key.startsWith('token_') && key.endsWith('_timestamp')) {
                const timestamp = storage[key];
                if (now - timestamp > expireTime) {
                    const tokenKey = key.replace('_timestamp', '');
                    keysToRemove.push(key, tokenKey);
                }
            }
        });
        
        if (keysToRemove.length > 0) {
            await chrome.storage.local.remove(keysToRemove);
            console.log('清理过期Token:', keysToRemove);
        }
    } catch (error) {
        console.error('清理过期Token失败:', error);
    }
} 