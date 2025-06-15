// Token检测器 - 智能检测和获取Token
class TokenDetector {
    constructor() {
        this.platform = null;
        this.isLoggedIn = false;
        this.observers = [];
        this.init();
    }

    init() {
        this.detectPlatform();
        this.checkLoginStatus();
        this.setupObservers();
        this.createFloatingButton();
    }

    detectPlatform() {
        this.platform = TokenUtils.getCurrentPlatform();
        if (this.platform) {
            TokenUtils.log('info', `检测到平台: ${this.platform.displayName}`);
        }
    }

    checkLoginStatus() {
        if (!this.platform) return;
        
        const wasLoggedIn = this.isLoggedIn;
        this.isLoggedIn = TokenUtils.isLoggedIn(this.platform);
        
        if (wasLoggedIn !== this.isLoggedIn) {
            TokenUtils.log('info', `登录状态变化: ${this.isLoggedIn ? '已登录' : '未登录'}`);
            this.updateFloatingButton();
        }
    }

    setupObservers() {
        // 监听DOM变化
        const observer = new MutationObserver(TokenUtils.debounce(() => {
            this.checkLoginStatus();
        }, 500));

        observer.observe(document.body, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['class', 'style']
        });

        this.observers.push(observer);

        // 监听页面可见性变化
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden) {
                this.checkLoginStatus();
            }
        });

        // 监听焦点变化
        window.addEventListener('focus', () => {
            this.checkLoginStatus();
        });
    }

    createFloatingButton() {
        if (!this.platform) return;

        const button = document.createElement('div');
        button.id = 'token-helper-floating-btn';
        button.innerHTML = `
            <div class="token-btn-content">
                <span class="token-btn-icon">🔑</span>
                <span class="token-btn-text">获取Token</span>
            </div>
        `;

        // 样式
        button.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 999999;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 12px 16px;
            border-radius: 25px;
            cursor: pointer;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            font-size: 14px;
            font-weight: 500;
            box-shadow: 0 4px 15px rgba(0,0,0,0.2);
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            user-select: none;
            backdrop-filter: blur(10px);
            border: 1px solid rgba(255,255,255,0.1);
            display: none;
        `;

        // 内容样式
        const style = document.createElement('style');
        style.textContent = `
            #token-helper-floating-btn .token-btn-content {
                display: flex;
                align-items: center;
                gap: 8px;
            }
            
            #token-helper-floating-btn:hover {
                transform: translateY(-2px);
                box-shadow: 0 8px 25px rgba(0,0,0,0.3);
                background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
            }
            
            #token-helper-floating-btn:active {
                transform: translateY(0);
                transition: transform 0.1s;
            }
            
            #token-helper-floating-btn.logged-out {
                background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
            }
            
            #token-helper-floating-btn.logged-out:hover {
                background: linear-gradient(135deg, #ee5a24 0%, #ff6b6b 100%);
            }
            
            @keyframes pulse {
                0% { box-shadow: 0 4px 15px rgba(0,0,0,0.2); }
                50% { box-shadow: 0 4px 25px rgba(102, 126, 234, 0.4); }
                100% { box-shadow: 0 4px 15px rgba(0,0,0,0.2); }
            }
            
            #token-helper-floating-btn.pulse {
                animation: pulse 2s infinite;
            }
        `;
        document.head.appendChild(style);

        // 事件监听
        button.addEventListener('click', () => this.handleButtonClick());
        
        // 拖拽功能
        this.makeDraggable(button);

        document.body.appendChild(button);
        this.floatingButton = button;
        this.updateFloatingButton();
    }

    makeDraggable(element) {
        let isDragging = false;
        let startX, startY, startLeft, startTop;

        element.addEventListener('mousedown', (e) => {
            isDragging = true;
            startX = e.clientX;
            startY = e.clientY;
            startLeft = parseInt(window.getComputedStyle(element).right);
            startTop = parseInt(window.getComputedStyle(element).top);
            element.style.cursor = 'grabbing';
            e.preventDefault();
        });

        document.addEventListener('mousemove', (e) => {
            if (!isDragging) return;
            
            const deltaX = startX - e.clientX;
            const deltaY = e.clientY - startY;
            
            element.style.right = (startLeft + deltaX) + 'px';
            element.style.top = (startTop + deltaY) + 'px';
        });

        document.addEventListener('mouseup', () => {
            if (isDragging) {
                isDragging = false;
                element.style.cursor = 'pointer';
            }
        });
    }

    updateFloatingButton() {
        if (!this.floatingButton) return;

        const textElement = this.floatingButton.querySelector('.token-btn-text');
        
        if (this.isLoggedIn) {
            this.floatingButton.style.display = 'block';
            this.floatingButton.classList.remove('logged-out');
            textElement.textContent = '获取Token';
        } else {
            this.floatingButton.style.display = 'block';
            this.floatingButton.classList.add('logged-out');
            textElement.textContent = '请先登录';
        }
    }

    async handleButtonClick() {
        if (!this.platform) {
            TokenUtils.showNotification('不支持的平台', 'error');
            return;
        }

        if (!this.isLoggedIn) {
            TokenUtils.showNotification(`请先登录${this.platform.displayName}`, 'warning');
            return;
        }

        try {
            // 添加脉冲效果
            this.floatingButton.classList.add('pulse');
            
            TokenUtils.showNotification('正在获取Token...', 'info', 1000);
            
            const token = await this.getToken();
            
            if (token) {
                const success = await TokenUtils.copyToClipboard(token);
                if (success) {
                    TokenUtils.showNotification('Token已复制到剪贴板！', 'success');
                    
                    // 保存到存储
                    await TokenUtils.setStorage(`token_${this.platform.name}`, {
                        token: token,
                        timestamp: Date.now(),
                        platform: this.platform.name
                    });
                } else {
                    TokenUtils.showNotification('复制失败，请手动复制', 'error');
                    this.showTokenDialog(token);
                }
            } else {
                TokenUtils.showNotification('获取Token失败，请检查登录状态', 'error');
            }
        } catch (error) {
            TokenUtils.log('error', 'Token获取失败', error);
            TokenUtils.showNotification('获取Token时发生错误', 'error');
        } finally {
            // 移除脉冲效果
            setTimeout(() => {
                this.floatingButton.classList.remove('pulse');
            }, 1000);
        }
    }

    async getToken() {
        try {
            // 发送消息给background script
            const response = await new Promise((resolve) => {
                chrome.runtime.sendMessage({
                    action: 'getTokens',
                    domain: window.location.hostname,
                    platform: this.platform.name
                }, resolve);
            });

            if (response && response.success && response.tokens) {
                return TokenUtils.formatToken(this.platform, response.tokens);
            }
            return null;
        } catch (error) {
            TokenUtils.log('error', '获取Token失败', error);
            return null;
        }
    }

    showTokenDialog(token) {
        const dialog = document.createElement('div');
        dialog.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0,0,0,0.5);
            z-index: 1000000;
            display: flex;
            align-items: center;
            justify-content: center;
        `;

        dialog.innerHTML = `
            <div style="
                background: white;
                padding: 24px;
                border-radius: 12px;
                max-width: 500px;
                width: 90%;
                box-shadow: 0 20px 40px rgba(0,0,0,0.3);
            ">
                <h3 style="margin: 0 0 16px 0; color: #333;">Token获取成功</h3>
                <p style="margin: 0 0 16px 0; color: #666;">请复制以下Token:</p>
                <textarea readonly style="
                    width: 100%;
                    height: 100px;
                    padding: 12px;
                    border: 1px solid #ddd;
                    border-radius: 6px;
                    font-family: monospace;
                    font-size: 12px;
                    resize: none;
                    box-sizing: border-box;
                ">${token}</textarea>
                <div style="margin-top: 16px; text-align: right;">
                    <button id="copy-token-btn" style="
                        background: #667eea;
                        color: white;
                        border: none;
                        padding: 8px 16px;
                        border-radius: 6px;
                        cursor: pointer;
                        margin-right: 8px;
                    ">复制</button>
                    <button id="close-dialog-btn" style="
                        background: #ccc;
                        color: #333;
                        border: none;
                        padding: 8px 16px;
                        border-radius: 6px;
                        cursor: pointer;
                    ">关闭</button>
                </div>
            </div>
        `;

        document.body.appendChild(dialog);

        // 事件监听
        dialog.querySelector('#copy-token-btn').addEventListener('click', async () => {
            const success = await TokenUtils.copyToClipboard(token);
            if (success) {
                TokenUtils.showNotification('已复制到剪贴板', 'success');
                document.body.removeChild(dialog);
            }
        });

        dialog.querySelector('#close-dialog-btn').addEventListener('click', () => {
            document.body.removeChild(dialog);
        });

        dialog.addEventListener('click', (e) => {
            if (e.target === dialog) {
                document.body.removeChild(dialog);
            }
        });
    }

    destroy() {
        this.observers.forEach(observer => observer.disconnect());
        if (this.floatingButton) {
            this.floatingButton.remove();
        }
    }
}

// 自动初始化
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        new TokenDetector();
    });
} else {
    new TokenDetector();
} 