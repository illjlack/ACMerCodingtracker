// 平台配置
const PLATFORMS = {
    'www.luogu.com.cn': {
        name: 'LUOGU',
        displayName: '洛谷',
        cookies: ['__client_id', '_uid'],
        format: '__client_id={__client_id}; _uid={_uid}'
    },
    'leetcode.com': {
        name: 'LEETCODE',
        displayName: 'LeetCode',
        cookies: ['csrftoken', 'LEETCODE_SESSION', 'sessionid'],
        format: 'csrftoken={csrftoken}; LEETCODE_SESSION={LEETCODE_SESSION}; sessionid={sessionid}'
    },
    'leetcode.cn': {
        name: 'LEETCODE_CN',
        displayName: 'LeetCode中国',
        cookies: ['csrftoken', 'LEETCODE_SESSION', 'sessionid'],
        format: 'csrftoken={csrftoken}; LEETCODE_SESSION={LEETCODE_SESSION}; sessionid={sessionid}'
    },
    'atcoder.jp': {
        name: 'ATCODER',
        displayName: 'AtCoder',
        cookies: ['REVEL_SESSION'],
        format: 'REVEL_SESSION={REVEL_SESSION}'
    },
    'www.topcoder.com': {
        name: 'TOPCODER',
        displayName: 'TopCoder',
        cookies: ['tcsso', 'tcjwt'],
        format: 'tcsso={tcsso}; tcjwt={tcjwt}'
    },
    'vjudge.net': {
        name: 'VIRTUAL_JUDGE',
        displayName: 'Virtual Judge',
        cookies: ['JSESSIONID'],
        format: 'JSESSIONID={JSESSIONID}'
    },
    'www.spoj.com': {
        name: 'SPOJ',
        displayName: 'SPOJ',
        cookies: ['spoj_session'],
        format: 'spoj_session={spoj_session}'
    },
    'www.codechef.com': {
        name: 'CODECHEF',
        displayName: 'CodeChef',
        cookies: ['sessionid', 'csrftoken'],
        format: 'sessionid={sessionid}; csrftoken={csrftoken}'
    }
};

// 存储获取到的tokens
let collectedTokens = {};

// 初始化
document.addEventListener('DOMContentLoaded', function() {
    initializePlatforms();
    setupEventListeners();
    detectCurrentPlatform();
});

function initializePlatforms() {
    const platformsContainer = document.getElementById('platforms');
    
    Object.entries(PLATFORMS).forEach(([domain, config]) => {
        const platformDiv = createPlatformElement(domain, config);
        platformsContainer.appendChild(platformDiv);
    });
}

function createPlatformElement(domain, config) {
    const div = document.createElement('div');
    div.className = 'platform';
    div.id = `platform-${config.name}`;
    
    div.innerHTML = `
        <div class="platform-header">
            <div class="platform-info">
                <div class="platform-name">${config.displayName}</div>
                <div class="platform-domain">${domain}</div>
            </div>
            <div class="platform-status" id="status-${config.name}">
                <span class="status-indicator"></span>
                <span class="status-text">检测中...</span>
            </div>
        </div>
        <div class="token-info">需要Cookie: ${config.cookies.join(', ')}</div>
        <div class="platform-actions">
            <button class="btn get-token-btn" data-domain="${domain}">
                <span>🔑</span>
                获取Token
            </button>
            <button class="btn btn-success copy-btn" data-platform="${config.name}" style="display:none;">
                <span>📋</span>
                复制
            </button>
            <button class="btn btn-secondary validate-btn" data-platform="${config.name}" style="display:none;">
                <span>✓</span>
                验证
            </button>
        </div>
        <div class="token-display" id="token-${config.name}" style="display:none;"></div>
    `;
    
    return div;
}

function setupEventListeners() {
    // 获取Token按钮事件
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('get-token-btn')) {
            const domain = e.target.getAttribute('data-domain');
            getTokenForDomain(domain);
        }
        
        if (e.target.classList.contains('copy-btn')) {
            const platform = e.target.getAttribute('data-platform');
            copyTokenToClipboard(platform);
        }
        
        if (e.target.classList.contains('validate-btn')) {
            const platform = e.target.getAttribute('data-platform');
            validateToken(platform);
        }
    });
    
    // 刷新按钮
    document.getElementById('refreshBtn').addEventListener('click', function() {
        detectCurrentPlatform();
        refreshAllTokens();
    });
    
    // 复制所有按钮
    document.getElementById('copyAllBtn').addEventListener('click', function() {
        copyAllTokens();
    });
    
    // 设置按钮
    document.getElementById('settingsBtn').addEventListener('click', function() {
        showSettings();
    });
}

async function detectCurrentPlatform() {
    try {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        const url = new URL(tab.url);
        const domain = url.hostname;
        
        // 高亮当前平台
        Object.entries(PLATFORMS).forEach(([platformDomain, config]) => {
            const platformElement = document.getElementById(`platform-${config.name}`);
            if (domain === platformDomain || domain.includes(platformDomain.replace('www.', ''))) {
                platformElement.style.border = '2px solid #409eff';
                platformElement.style.backgroundColor = '#f0f9ff';
                // 自动获取当前平台的token
                getTokenForDomain(platformDomain);
            } else {
                platformElement.style.border = '1px solid #ddd';
                platformElement.style.backgroundColor = 'white';
            }
        });
    } catch (error) {
        console.error('检测当前平台失败:', error);
    }
}

async function getTokenForDomain(domain) {
    const config = PLATFORMS[domain];
    if (!config) return;
    
    const tokenElement = document.getElementById(`token-${config.name}`);
    const copyBtn = document.querySelector(`[data-platform="${config.name}"].copy-btn`);
    const validateBtn = document.querySelector(`[data-platform="${config.name}"].validate-btn`);
    
    updateStatus(config.name, 'loading', '获取中...');
    
    try {
        // 获取cookies
        const cookies = {};
        let missingCookies = [];
        
        for (const cookieName of config.cookies) {
            try {
                const cookie = await chrome.cookies.get({
                    url: `https://${domain}`,
                    name: cookieName
                });
                
                if (cookie && cookie.value) {
                    cookies[cookieName] = cookie.value;
                } else {
                    missingCookies.push(cookieName);
                }
            } catch (error) {
                missingCookies.push(cookieName);
            }
        }
        
        if (missingCookies.length > 0) {
            updateStatus(config.name, 'error', `缺少: ${missingCookies.join(', ')}`);
            tokenElement.style.display = 'none';
            copyBtn.style.display = 'none';
            validateBtn.style.display = 'none';
            return;
        }
        
        // 生成token字符串
        let tokenString = config.format;
        Object.entries(cookies).forEach(([name, value]) => {
            tokenString = tokenString.replace(`{${name}}`, value);
        });
        
        // 存储token
        collectedTokens[config.name] = tokenString;
        
        // 显示结果
        updateStatus(config.name, 'success', '获取成功');
        tokenElement.textContent = tokenString.length > 100 ? 
            tokenString.substring(0, 100) + '...' : tokenString;
        tokenElement.style.display = 'block';
        copyBtn.style.display = 'inline-flex';
        validateBtn.style.display = 'inline-flex';
        
        // 保存到storage
        await chrome.storage.local.set({
            [`token_${config.name}`]: tokenString,
            [`token_${config.name}_timestamp`]: Date.now()
        });
        
    } catch (error) {
        console.error(`获取${config.displayName}Token失败:`, error);
        updateStatus(config.name, 'error', '获取失败');
        tokenElement.style.display = 'none';
        copyBtn.style.display = 'none';
        validateBtn.style.display = 'none';
    }
}

async function copyTokenToClipboard(platform) {
    const token = collectedTokens[platform];
    if (!token) {
        showToast('没有找到Token', 'warning');
        return;
    }
    
    try {
        await navigator.clipboard.writeText(token);
        showToast('Token已复制到剪贴板', 'success');
        
        // 更新复制按钮状态
        const copyBtn = document.querySelector(`[data-platform="${platform}"].copy-btn`);
        const originalText = copyBtn.innerHTML;
        copyBtn.innerHTML = '<span>✓</span>已复制';
        copyBtn.disabled = true;
        
        setTimeout(() => {
            copyBtn.innerHTML = originalText;
            copyBtn.disabled = false;
        }, 2000);
        
    } catch (error) {
        console.error('复制失败:', error);
        showToast('复制失败', 'error');
    }
}

async function copyAllTokens() {
    const validTokens = Object.entries(collectedTokens)
        .filter(([_, token]) => token && token.trim())
        .map(([platform, token]) => {
            const config = Object.values(PLATFORMS).find(p => p.name === platform);
            const displayName = config ? config.displayName : platform;
            return `${displayName}: ${token}`;
        })
        .join('\n\n');
    
    if (!validTokens) {
        showToast('没有可复制的Token', 'warning');
        return;
    }
    
    try {
        await navigator.clipboard.writeText(validTokens);
        showToast(`已复制${Object.keys(collectedTokens).length}个Token`, 'success');
        
        const btn = document.getElementById('copyAllBtn');
        const originalText = btn.innerHTML;
        btn.innerHTML = '<span>✓</span>已复制';
        btn.disabled = true;
        
        setTimeout(() => {
            btn.innerHTML = originalText;
            btn.disabled = false;
        }, 2000);
    } catch (error) {
        console.error('复制失败:', error);
        showToast('复制失败', 'error');
    }
}

async function refreshAllTokens() {
    const refreshBtn = document.getElementById('refreshBtn');
    const originalText = refreshBtn.innerHTML;
    refreshBtn.innerHTML = '<span>⏳</span>刷新中...';
    refreshBtn.disabled = true;
    
    collectedTokens = {};
    
    try {
        for (const [domain, config] of Object.entries(PLATFORMS)) {
            await getTokenForDomain(domain);
            // 添加小延迟避免请求过快
            await new Promise(resolve => setTimeout(resolve, 200));
        }
        showToast('刷新完成', 'success');
    } catch (error) {
        showToast('刷新失败', 'error');
    } finally {
        refreshBtn.innerHTML = originalText;
        refreshBtn.disabled = false;
    }
}

async function validateToken(platform) {
    const token = collectedTokens[platform];
    if (!token) {
        showToast('没有找到Token', 'warning');
        return;
    }
    
    const validateBtn = document.querySelector(`[data-platform="${platform}"].validate-btn`);
    const originalText = validateBtn.innerHTML;
    validateBtn.innerHTML = '<span>⏳</span>验证中...';
    validateBtn.disabled = true;
    
    try {
        // 这里可以添加实际的验证逻辑
        // 暂时使用简单的格式验证
        const config = PLATFORMS[Object.keys(PLATFORMS).find(domain => 
            PLATFORMS[domain].name === platform
        )];
        
        if (config) {
            const isValid = validateTokenFormat(config, token);
            if (isValid) {
                updateStatus(platform, 'success', '验证通过');
                showToast('Token验证通过', 'success');
            } else {
                updateStatus(platform, 'warning', '格式异常');
                showToast('Token格式可能有问题', 'warning');
            }
        }
    } catch (error) {
        updateStatus(platform, 'error', '验证失败');
        showToast('验证失败', 'error');
    } finally {
        validateBtn.innerHTML = originalText;
        validateBtn.disabled = false;
    }
}

function validateTokenFormat(config, token) {
    if (!config || !token) return false;
    
    // 检查是否包含所有必需的cookie
    for (const cookieName of config.cookies) {
        if (!token.includes(cookieName + '=')) {
            return false;
        }
    }
    return true;
}

function updateStatus(platform, type, message) {
    const statusElement = document.getElementById(`status-${platform}`);
    if (!statusElement) return;
    
    const indicator = statusElement.querySelector('.status-indicator');
    const text = statusElement.querySelector('.status-text');
    
    // 移除所有状态类
    statusElement.className = 'platform-status';
    statusElement.classList.add(`status-${type}`);
    
    text.textContent = message;
}

function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);
    
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }, 3000);
}

function showSettings() {
    // 创建设置对话框
    const overlay = document.createElement('div');
    overlay.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,0.5);
        z-index: 1000;
        display: flex;
        align-items: center;
        justify-content: center;
    `;
    
    overlay.innerHTML = `
        <div style="
            background: white;
            padding: 24px;
            border-radius: 12px;
            max-width: 400px;
            width: 90%;
            box-shadow: 0 20px 40px rgba(0,0,0,0.3);
        ">
            <h3 style="margin: 0 0 16px 0;">⚙️ 扩展设置</h3>
            <div style="margin-bottom: 16px;">
                <label style="display: block; margin-bottom: 8px;">
                    <input type="checkbox" id="autoRefresh" style="margin-right: 8px;">
                    自动刷新Token状态
                </label>
                <label style="display: block; margin-bottom: 8px;">
                    <input type="checkbox" id="showNotifications" style="margin-right: 8px;" checked>
                    显示通知消息
                </label>
                <label style="display: block; margin-bottom: 8px;">
                    <input type="checkbox" id="saveHistory" style="margin-right: 8px;" checked>
                    保存Token历史
                </label>
            </div>
            <div style="text-align: right;">
                <button id="saveSettings" class="btn" style="margin-right: 8px;">保存</button>
                <button id="closeSettings" class="btn btn-secondary">关闭</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(overlay);
    
    // 事件监听
    overlay.querySelector('#saveSettings').addEventListener('click', () => {
        // 保存设置逻辑
        showToast('设置已保存', 'success');
        document.body.removeChild(overlay);
    });
    
    overlay.querySelector('#closeSettings').addEventListener('click', () => {
        document.body.removeChild(overlay);
    });
    
    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) {
            document.body.removeChild(overlay);
        }
    });
} 