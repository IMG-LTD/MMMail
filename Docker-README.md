# MMMail Docker 部署指南

这是 MMMail 一站式协同办公平台的 Docker 容器化部署方案。

## 🏗️ 架构说明

本 Docker 配置包含以下服务：

- **MySQL 8.0**: 主数据库，端口 3306
- **Redis 7.2**: 缓存服务，端口 6379  
- **Backend**: Spring Boot 后端服务，端口 1024
- **Frontend**: Vue.js 前端服务，端口 80
- **Nginx**: 反向代理服务，端口 8080

## 📋 准备工作

确保您的系统已安装：

- Docker Desktop (Windows/Mac) 或 Docker Engine (Linux)
- Docker Compose

### 验证 Docker 安装

```bash
docker --version
docker-compose --version
```

## 🚀 快速启动

### 方法一：使用启动脚本（推荐）

**Windows 用户**：
在项目根目录下，双击运行 `docker-start.bat`，然后按照菜单提示操作。

**Linux/macOS 用户**：
```bash
# 给脚本执行权限
chmod +x docker-start.sh

# 运行脚本
./docker-start.sh
```

### 方法二：使用命令行

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 方法三：生产环境部署

```bash
# 复制环境变量模板
cp .env.template .env

# 编辑环境变量
vim .env

# 使用生产环境配置启动
docker-compose -f docker-compose.prod.yml up -d
```

## 🌐 访问地址

启动成功后，您可以通过以下地址访问：

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端管理界面 | http://localhost:8080 | 主要的管理界面 |
| 后端API接口 | http://localhost:1024 | RESTful API 接口 |
| 数据库监控 | http://localhost:8080/druid | Druid 监控面板 |
| API文档 | http://localhost:8080/api/doc.html | Knife4j 接口文档 |

## 👤 默认账号

| 服务 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 系统管理员 | admin | 123456 | 超级管理员 |
| Druid监控 | admin | admin123 | 数据库监控 |
| MySQL | mmmail | mmmail123 | 数据库用户 |

## 🗂️ 项目结构

```
docker/
├── backend/                 # 后端配置
│   ├── Dockerfile          # 后端镜像构建文件
│   └── application-docker.yaml  # Docker环境配置
├── frontend/               # 前端配置
│   └── Dockerfile          # 前端镜像构建文件
├── mysql/                  # MySQL配置
│   ├── conf/
│   │   └── my.cnf          # MySQL配置文件
│   └── init/
│       ├── mmmail_v3.sql   # 数据库初始化脚本
│       └── init-database.sh  # 数据库初始化脚本
├── nginx/                  # Nginx配置
│   ├── conf.d/
│   │   └── default.conf    # 反向代理配置
│   ├── nginx.conf          # 主配置文件
│   └── frontend.conf       # 前端配置
└── redis/
    └── redis.conf          # Redis配置文件
```

## ⚙️ 配置说明

### 端口映射

- `3306`: MySQL 数据库
- `6379`: Redis 缓存
- `1024`: 后端 API 服务
- `80`: 前端静态服务（容器内部）
- `8080`: Nginx 反向代理（外部访问）

### 数据持久化

以下数据会被持久化存储：

- MySQL 数据：`mysql_data` volume
- Redis 数据：`redis_data` volume
- 应用日志：`./logs` 目录

### 网络配置

所有服务都在 `mmmail-network` 自定义网络中，服务间可以通过服务名进行通信。

## 🔧 自定义配置

### 修改端口

如需修改端口，请编辑 `docker-compose.yml` 文件中的 `ports` 配置。

### 修改数据库密码

1. 修改 `docker-compose.yml` 中的环境变量
2. 修改 `docker/backend/application-docker.yaml` 中的数据库配置

### 修改邮件配置

编辑 `docker/backend/application-docker.yaml` 文件中的邮件配置：

```yaml
spring:
  mail:
    host: your-smtp-server
    port: 465
    username: your-email@domain.com
    password: your-password
```

## 🔍 故障排除

### 常见问题

1. **端口冲突**
   ```bash
   # 检查端口占用
   netstat -ano | findstr :8080
   ```

2. **数据库连接失败**
   ```bash
   # 查看数据库日志
   docker-compose logs mysql
   ```

3. **内存不足**
   ```bash
   # 检查 Docker 资源使用
   docker stats
   ```

### 重新构建

如果需要重新构建镜像：

```bash
# 重新构建所有服务
docker-compose build --no-cache

# 重新构建特定服务
docker-compose build --no-cache backend
```

### 清理和重置

```bash
# 停止并删除所有容器
docker-compose down

# 删除所有容器和数据卷（谨慎使用）
docker-compose down -v

# 清理未使用的镜像
docker system prune -f
```

## 📝 日志查看

```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs backend

# 实时跟踪日志
docker-compose logs -f backend
```

## 🔐 安全建议

1. **修改默认密码**：部署前请修改所有默认密码
2. **网络安全**：生产环境中请配置防火墙规则
3. **数据备份**：定期备份数据库和重要文件
4. **SSL配置**：生产环境建议配置 HTTPS

## 📧 技术支持

如遇问题，请参考：

- [项目文档](README.md)
- [GitHub Issues](https://github.com/your-repo/issues)
- 技术QQ群：123456789

## 📄 许可证

本项目基于 MIT 许可证开源。
