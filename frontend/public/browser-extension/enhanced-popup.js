// 增强版弹窗脚本
class EnhancedPopup {
    constructor() {
        this.platforms = {};
        this.currentTab = null;
        this.collectedTokens = {};
        this.init();
    }

    async init() {
        await this.getCurrentTab();
        this.setupEventListeners();
        this.loadPlatforms();
        this.detectCurrentPlatform();
        this.loadStoredTokens();
    }

    async getCurrentTab() {
        try {
            const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
            this.currentTab = tab;
        } catch (error) {
            console.error('获取当前标签页失败:', error);
        }
    }

    setupEventListeners() {
        // 刷新按钮
        document.getElementById('refreshBtn')?.addEventListener('click', () => {
            this.refreshAllTokens();
        });

        // 复制所有按钮
        document.getElementById('copyAllBtn')?.addEventListener('click', () => {
            this.copyAllTokens();
        });

        // 设置按钮
        document.getElementById('settingsBtn')?.addEventListener('click', () => {
            this.showSettings();
        });

        // 帮助按钮
        document.getElementById('helpBtn')?.addEventListener('click', () => {
            this.showHelp();
        });

        // 全局键盘快捷键
        document.addEventListener('keydown', (e) => {
            if (e.ctrlKey || e.metaKey) {
                switch (e.key) {
                    case 'r':
                        e.preventDefault();
                        this.refreshAllTokens();
                        break;
                    case 'a':
                        e.preventDefault();
                        this.copyAllTokens();
                        break;
                }
            }
        });
    }

    loadPlatforms() {
        this.platforms = TokenUtils.PLATFORMS;
        this.renderPlatforms();
    }

    renderPlatforms() {
        const container = document.getElementById('platforms');
        if (!container) return;

        container.innerHTML = '';

        Object.entries(this.platforms).forEach(([domain, config]) => {
            const platformElement = this.createPlatformElement(domain, config);
            container.appendChild(platformElement);
        });
    }

    createPlatformElement(domain, config) {
        const div = document.createElement('div');
        div.className = 'platform-card';
        div.id = `platform-${config.name}`;
        
        div.innerHTML = `
            <div class="platform-header">
                <div class="platform-info">
                    <h3 class="platform-name">${config.displayName}</h3>
                    <span class="platform-domain">${domain}</span>
                </div>
                <div class="platform-status" id="status-${config.name}">
                    <span class="status-indicator"></span>
                    <span class="status-text">检测中...</span>
                </div>
            </div>
            
            <div class="platform-content">
                <div class="token-info">
                    <p class="required-cookies">
                        <i class="icon-cookie"></i>
                        需要: ${config.cookies.join(', ')}
                    </p>
                    <div class="token-preview" id="preview-${config.name}" style="display: none;">
                        <p class="preview-label">Token预览:</p>
                        <div class="preview-content" id="token-content-${config.name}"></div>
                    </div>
                </div>
                
                <div class="platform-actions">
                    <button class="btn btn-primary get-token-btn" data-domain="${domain}" data-platform="${config.name}">
                        <i class="icon-key"></i>
                        获取Token
                    </button>
                    <button class="btn btn-secondary copy-btn" data-platform="${config.name}" style="display: none;">
                        <i class="icon-copy"></i>
                        复制
                    </button>
                    <button class="btn btn-info validate-btn" data-platform="${config.name}" style="display: none;">
                        <i class="icon-check"></i>
                        验证
                    </button>
                </div>
            </div>
            
            <div class="platform-footer">
                <div class="last-updated" id="updated-${config.name}" style="display: none;">
                    <i class="icon-time"></i>
                    <span class="update-time"></span>
                </div>
            </div>
        `;
        
        // 添加事件监听
        this.setupPlatformEvents(div, domain, config);
        
        return div;
    }

    setupPlatformEvents(element, domain, config) {
        // 获取Token按钮
        const getBtn = element.querySelector('.get-token-btn');
        getBtn.addEventListener('click', () => this.getTokenForPlatform(domain, config));

        // 复制按钮
        const copyBtn = element.querySelector('.copy-btn');
        copyBtn.addEventListener('click', () => this.copyToken(config.name));

        // 验证按钮
        const validateBtn = element.querySelector('.validate-btn');
        validateBtn.addEventListener('click', () => this.validateToken(config.name));
    }

    async detectCurrentPlatform() {
        if (!this.currentTab) return;

        try {
            const url = new URL(this.currentTab.url);
            const domain = url.hostname;
            
            // 高亮当前平台
            Object.entries(this.platforms).forEach(([platformDomain, config]) => {
                const element = document.getElementById(`platform-${config.name}`);
                if (!element) return;

                if (domain === platformDomain || domain.includes(platformDomain.replace('www.', ''))) {
                    element.classList.add('current-platform');
                    // 自动获取当前平台的token
                    this.getTokenForPlatform(platformDomain, config);
                } else {
                    element.classList.remove('current-platform');
                }
            });
        } catch (error) {
            console.error('检测当前平台失败:', error);
        }
    }

    async getTokenForPlatform(domain, config) {
        const statusElement = document.getElementById(`status-${config.name}`);
        const previewElement = document.getElementById(`preview-${config.name}`);
        const tokenContentElement = document.getElementById(`token-content-${config.name}`);
        const copyBtn = document.querySelector(`[data-platform="${config.name}"].copy-btn`);
        const validateBtn = document.querySelector(`[data-platform="${config.name}"].validate-btn`);
        const getBtn = document.querySelector(`[data-platform="${config.name}"].get-token-btn`);

        // 更新状态
        this.updateStatus(config.name, 'loading', '获取中...');
        getBtn.disabled = true;

        try {
            // 获取cookies
            const cookies = {};
            const missingCookies = [];
            
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
                this.updateStatus(config.name, 'error', `缺少: ${missingCookies.join(', ')}`);
                previewElement.style.display = 'none';
                copyBtn.style.display = 'none';
                validateBtn.style.display = 'none';
                return;
            }
            
            // 生成token
            const tokenString = TokenUtils.formatToken(config, cookies);
            
            if (tokenString) {
                // 存储token
                this.collectedTokens[config.name] = {
                    token: tokenString,
                    timestamp: Date.now(),
                    platform: config.displayName
                };
                
                // 更新UI
                this.updateStatus(config.name, 'success', '获取成功');
                tokenContentElement.textContent = this.truncateToken(tokenString);
                previewElement.style.display = 'block';
                copyBtn.style.display = 'inline-flex';
                validateBtn.style.display = 'inline-flex';
                
                // 更新时间
                this.updateLastUpdated(config.name);
                
                // 保存到storage
                await TokenUtils.setStorage(`token_${config.name}`, {
                    token: tokenString,
                    timestamp: Date.now(),
                    domain: domain
                });
                
            } else {
                this.updateStatus(config.name, 'error', '格式化失败');
            }
            
        } catch (error) {
            console.error(`获取${config.displayName}Token失败:`, error);
            this.updateStatus(config.name, 'error', '获取失败');
        } finally {
            getBtn.disabled = false;
        }
    }

    updateStatus(platformName, type, message) {
        const statusElement = document.getElementById(`status-${platformName}`);
        if (!statusElement) return;

        const indicator = statusElement.querySelector('.status-indicator');
        const text = statusElement.querySelector('.status-text');
        
        // 移除所有状态类
        statusElement.className = 'platform-status';
        statusElement.classList.add(`status-${type}`);
        
        text.textContent = message;
    }

    truncateToken(token, maxLength = 50) {
        if (token.length <= maxLength) return token;
        return token.substring(0, maxLength) + '...';
    }

    updateLastUpdated(platformName) {
        const element = document.getElementById(`updated-${platformName}`);
        if (!element) return;

        const timeSpan = element.querySelector('.update-time');
        timeSpan.textContent = `更新于 ${new Date().toLocaleTimeString()}`;
        element.style.display = 'flex';
    }

    async copyToken(platformName) {
        const tokenData = this.collectedTokens[platformName];
        if (!tokenData) return;

        try {
            await navigator.clipboard.writeText(tokenData.token);
            this.showToast('Token已复制到剪贴板', 'success');
            
            // 更新复制按钮状态
            const copyBtn = document.querySelector(`[data-platform="${platformName}"].copy-btn`);
            const originalText = copyBtn.innerHTML;
            copyBtn.innerHTML = '<i class="icon-check"></i>已复制';
            copyBtn.disabled = true;
            
            setTimeout(() => {
                copyBtn.innerHTML = originalText;
                copyBtn.disabled = false;
            }, 2000);
            
        } catch (error) {
            this.showToast('复制失败', 'error');
        }
    }

    async validateToken(platformName) {
        const tokenData = this.collectedTokens[platformName];
        if (!tokenData) return;

        const validateBtn = document.querySelector(`[data-platform="${platformName}"].validate-btn`);
        const originalText = validateBtn.innerHTML;
        validateBtn.innerHTML = '<i class="icon-loading"></i>验证中...';
        validateBtn.disabled = true;

        try {
            // 这里可以调用后端API验证token
            // 暂时使用简单的格式验证
            const platform = Object.values(this.platforms).find(p => p.name === platformName);
            const isValid = TokenUtils.validateTokenFormat(platform, tokenData.token);
            
            if (isValid) {
                this.updateStatus(platformName, 'success', '验证通过');
                this.showToast('Token验证通过', 'success');
            } else {
                this.updateStatus(platformName, 'warning', '格式异常');
                this.showToast('Token格式可能有问题', 'warning');
            }
            
        } catch (error) {
            this.updateStatus(platformName, 'error', '验证失败');
            this.showToast('验证失败', 'error');
        } finally {
            validateBtn.innerHTML = originalText;
            validateBtn.disabled = false;
        }
    }

    async refreshAllTokens() {
        const refreshBtn = document.getElementById('refreshBtn');
        const originalText = refreshBtn.textContent;
        refreshBtn.textContent = '刷新中...';
        refreshBtn.disabled = true;

        try {
            // 重新检测当前平台
            await this.detectCurrentPlatform();
            
            // 刷新所有平台状态
            for (const [domain, config] of Object.entries(this.platforms)) {
                await this.getTokenForPlatform(domain, config);
                // 添加延迟避免请求过快
                await new Promise(resolve => setTimeout(resolve, 200));
            }
            
            this.showToast('刷新完成', 'success');
        } catch (error) {
            this.showToast('刷新失败', 'error');
        } finally {
            refreshBtn.textContent = originalText;
            refreshBtn.disabled = false;
        }
    }

    async copyAllTokens() {
        const validTokens = Object.entries(this.collectedTokens)
            .filter(([_, data]) => data && data.token)
            .map(([platform, data]) => `${data.platform}: ${data.token}`)
            .join('\n\n');

        if (!validTokens) {
            this.showToast('没有可复制的Token', 'warning');
            return;
        }

        try {
            await navigator.clipboard.writeText(validTokens);
            this.showToast(`已复制${Object.keys(this.collectedTokens).length}个Token`, 'success');
            
            const copyAllBtn = document.getElementById('copyAllBtn');
            const originalText = copyAllBtn.textContent;
            copyAllBtn.textContent = '已复制';
            copyAllBtn.disabled = true;
            
            setTimeout(() => {
                copyAllBtn.textContent = originalText;
                copyAllBtn.disabled = false;
            }, 2000);
            
        } catch (error) {
            this.showToast('复制失败', 'error');
        }
    }

    async loadStoredTokens() {
        for (const config of Object.values(this.platforms)) {
            try {
                const stored = await TokenUtils.getStorage(`token_${config.name}`);
                if (stored && stored.token) {
                    this.collectedTokens[config.name] = stored;
                    
                    // 更新UI
                    const previewElement = document.getElementById(`preview-${config.name}`);
                    const tokenContentElement = document.getElementById(`token-content-${config.name}`);
                    const copyBtn = document.querySelector(`[data-platform="${config.name}"].copy-btn`);
                    const validateBtn = document.querySelector(`[data-platform="${config.name}"].validate-btn`);
                    
                    if (previewElement && tokenContentElement) {
                        tokenContentElement.textContent = this.truncateToken(stored.token);
                        previewElement.style.display = 'block';
                        copyBtn.style.display = 'inline-flex';
                        validateBtn.style.display = 'inline-flex';
                        
                        this.updateStatus(config.name, 'success', '已缓存');
                        
                        // 显示缓存时间
                        const element = document.getElementById(`updated-${config.name}`);
                        if (element) {
                            const timeSpan = element.querySelector('.update-time');
                            timeSpan.textContent = `缓存于 ${new Date(stored.timestamp).toLocaleString()}`;
                            element.style.display = 'flex';
                        }
                    }
                }
            } catch (error) {
                console.error(`加载${config.name}缓存失败:`, error);
            }
        }
    }

    showToast(message, type = 'info') {
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

    showSettings() {
        // 实现设置对话框
        console.log('显示设置');
    }

    showHelp() {
        // 打开帮助页面
        try {
            if (chrome.tabs && chrome.tabs.create) {
                chrome.tabs.create({ url: chrome.runtime.getURL('README.md') });
            } else {
                // 如果没有tabs权限，在新窗口打开
                window.open(chrome.runtime.getURL('README.md'), '_blank');
            }
        } catch (error) {
            console.error('打开帮助页面失败:', error);
            this.showToast('无法打开帮助页面', 'error');
        }
    }
}

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    new EnhancedPopup();
}); 