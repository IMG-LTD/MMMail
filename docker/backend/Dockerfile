# 使用多阶段构建，首先构建阶段
FROM maven:3.9.4-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制pom文件
COPY m-parent/pom.xml ./m-parent/
COPY m-parent/m-base/pom.xml ./m-parent/m-base/
COPY m-parent/m-admin/pom.xml ./m-parent/m-admin/

# 下载依赖（利用Docker层缓存）
RUN cd m-parent && mvn dependency:go-offline -B

# 复制源代码
COPY m-parent/ ./m-parent/

# 构建应用
RUN cd m-parent && mvn clean package -DskipTests -Pprod

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

# 设置工作目录
WORKDIR /app

# 创建非root用户
RUN addgroup -g 1001 -S mmmail && \
    adduser -S mmmail -u 1001 -G mmmail

# 安装必要的包
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 从构建阶段复制jar文件
COPY --from=builder /app/m-parent/m-admin/target/*.jar /app/app.jar

# 创建配置和日志目录
RUN mkdir -p /app/config /app/logs && \
    chown -R mmmail:mmmail /app

# 切换到非root用户
USER mmmail

# 暴露端口
EXPOSE 1024

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:1024/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=docker", \
    "-Dspring.config.additional-location=file:/app/config/", \
    "-jar", \
    "/app/app.jar"]
