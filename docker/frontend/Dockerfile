# 多阶段构建 - 构建阶段
FROM node:18-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制package文件
COPY package*.json ./

# 安装依赖
RUN npm ci --only=production

# 复制源代码
COPY . .

# 构建应用
RUN npm run build:prod

# 生产阶段
FROM nginx:alpine

# 安装必要的包
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 从构建阶段复制构建结果
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制nginx配置文件
COPY ../nginx/frontend.conf /etc/nginx/conf.d/default.conf

# 创建非root用户
RUN addgroup -g 1001 -S mmmail && \
    adduser -S mmmail -u 1001 -G mmmail

# 设置权限
RUN chown -R mmmail:mmmail /usr/share/nginx/html && \
    chown -R mmmail:mmmail /var/cache/nginx && \
    chown -R mmmail:mmmail /var/log/nginx && \
    chown -R mmmail:mmmail /etc/nginx/conf.d

# 修改nginx配置以支持非root用户
RUN sed -i 's/user  nginx;/user  mmmail;/' /etc/nginx/nginx.conf

# 切换到非root用户
USER mmmail

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# 启动nginx
CMD ["nginx", "-g", "daemon off;"]
