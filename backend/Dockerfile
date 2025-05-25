# 第一阶段：使用 Maven 构建项目
FROM maven:3.8.5-openjdk-17 AS builder

# 设置工作目录
WORKDIR /build

# 确保 settings.xml 存在并正确复制
COPY ./settings.xml /root/.m2/settings.xml

# 将项目源代码复制进来
COPY . .

# 构建项目，跳过测试
RUN mvn clean package -DskipTests


# ===========================
# 第二阶段：创建运行容器镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 从构建阶段复制构建好的 jar 包
COPY --from=builder /build/target/*.jar app.jar

# 暴露端口（根据你的应用设置）
EXPOSE 8080

# 启动 Spring Boot 应用
ENTRYPOINT ["java", "-jar", "app.jar"]
