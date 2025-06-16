# 阶段1：构建阶段
FROM node:20 AS build-stage

# 设置工作目录
WORKDIR /app

# 复制 package.json 和 package-lock.json 到容器中
COPY package*.json ./

# 安装项目依赖
RUN npm config set registry https://registry.npmmirror.com
RUN npm install --verbose

# 复制项目源代码
COPY . .

# 执行构建命令生成生产环境的静态文件
RUN npm run build

# 阶段2：生产部署阶段
FROM nginx:stable-alpine AS production-stage

# 删除默认的 nginx 页面
RUN rm -rf /usr/share/nginx/html/*

# 将构建好的静态文件从构建阶段复制到 nginx 服务器的静态目录
COPY --from=build-stage /app/dist /usr/share/nginx/html

# 复制自定义的 nginx 配置文件（可选）
# COPY nginx.conf /etc/nginx/nginx.conf

# 暴露 80 端口
EXPOSE 80

# 启动 nginx 服务
CMD ["nginx", "-g", "daemon off;"]
