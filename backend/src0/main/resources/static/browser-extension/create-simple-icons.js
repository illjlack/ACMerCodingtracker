// 简单图标生成脚本
// 运行此脚本生成基本的PNG图标文件

function createSimpleIcon(size, filename) {
    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');
    
    // 绘制背景
    const gradient = ctx.createLinearGradient(0, 0, size, size);
    gradient.addColorStop(0, '#667eea');
    gradient.addColorStop(1, '#764ba2');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, size, size);
    
    // 绘制圆角
    ctx.globalCompositeOperation = 'destination-in';
    ctx.beginPath();
    ctx.roundRect(0, 0, size, size, size * 0.2);
    ctx.fill();
    ctx.globalCompositeOperation = 'source-over';
    
    // 绘制钥匙图标
    const centerX = size / 2;
    const centerY = size / 2;
    const keySize = size * 0.6;
    
    ctx.strokeStyle = 'white';
    ctx.fillStyle = 'white';
    ctx.lineWidth = size * 0.08;
    
    // 钥匙头部（圆形）
    ctx.beginPath();
    ctx.arc(centerX - keySize * 0.2, centerY, keySize * 0.15, 0, 2 * Math.PI);
    ctx.stroke();
    
    // 钥匙柄
    ctx.beginPath();
    ctx.moveTo(centerX - keySize * 0.05, centerY);
    ctx.lineTo(centerX + keySize * 0.3, centerY);
    ctx.stroke();
    
    // 钥匙齿
    ctx.beginPath();
    ctx.moveTo(centerX + keySize * 0.2, centerY);
    ctx.lineTo(centerX + keySize * 0.2, centerY + keySize * 0.1);
    ctx.stroke();
    
    ctx.beginPath();
    ctx.moveTo(centerX + keySize * 0.3, centerY);
    ctx.lineTo(centerX + keySize * 0.3, centerY + keySize * 0.08);
    ctx.stroke();
    
    // 转换为blob并下载
    canvas.toBlob(function(blob) {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }, 'image/png');
}

// 使用说明：
// 1. 在浏览器控制台中运行此脚本
// 2. 或者创建一个HTML页面包含此脚本
// 3. 调用以下函数生成图标：

console.log('图标生成脚本已加载');
console.log('使用方法：');
console.log('createSimpleIcon(16, "icon16.png")');
console.log('createSimpleIcon(48, "icon48.png")');
console.log('createSimpleIcon(128, "icon128.png")');

// 自动生成所有尺寸的图标
function generateAllIcons() {
    createSimpleIcon(16, 'icon16.png');
    setTimeout(() => createSimpleIcon(48, 'icon48.png'), 500);
    setTimeout(() => createSimpleIcon(128, 'icon128.png'), 1000);
    console.log('正在生成所有图标...');
}

// 如果在HTML页面中运行，可以调用：
// generateAllIcons(); 