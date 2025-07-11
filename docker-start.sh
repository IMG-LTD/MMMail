#!/bin/bash

# MMMail Docker 管理脚本 (Linux/macOS)

show_banner() {
    echo "================================="
    echo "     MMMail Docker 管理脚本"
    echo "================================="
    echo
}

show_menu() {
    echo "请选择操作:"
    echo "[1] 启动所有服务"
    echo "[2] 停止所有服务"
    echo "[3] 重启所有服务"
    echo "[4] 查看服务状态"
    echo "[5] 查看服务日志"
    echo "[6] 删除所有容器和数据"
    echo "[7] 重新构建镜像"
    echo "[8] 查看资源使用情况"
    echo "[0] 退出"
    echo
}

start_services() {
    echo "正在启动所有服务..."
    docker-compose up -d
    
    if [ $? -eq 0 ]; then
        echo "✅ 服务启动成功！"
        echo
        echo "访问地址:"
        echo "前端管理界面: http://localhost:8080"
        echo "后端API接口: http://localhost:1024"
        echo "数据库监控: http://localhost:8080/druid"
        echo "API文档: http://localhost:8080/api/doc.html"
        echo
        echo "默认管理员账号: admin"
        echo "默认密码: 123456"
    else
        echo "❌ 服务启动失败！"
        echo "请检查 Docker 是否正在运行，或查看详细日志"
    fi
}

stop_services() {
    echo "正在停止所有服务..."
    docker-compose down
    echo "✅ 服务已停止"
}

restart_services() {
    echo "正在重启所有服务..."
    docker-compose restart
    echo "✅ 服务重启完成"
}

show_status() {
    echo "查看服务状态..."
    docker-compose ps
    echo
    echo "容器详细信息:"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
}

show_logs() {
    echo "查看最近的日志..."
    echo "选择要查看的服务:"
    echo "[1] 所有服务"
    echo "[2] 后端服务"
    echo "[3] 前端服务"
    echo "[4] 数据库服务"
    echo "[5] Redis服务"
    echo "[6] Nginx服务"
    
    read -p "请选择 (1-6): " log_choice
    
    case $log_choice in
        1) docker-compose logs --tail=50 -f ;;
        2) docker-compose logs --tail=50 -f backend ;;
        3) docker-compose logs --tail=50 -f frontend ;;
        4) docker-compose logs --tail=50 -f mysql ;;
        5) docker-compose logs --tail=50 -f redis ;;
        6) docker-compose logs --tail=50 -f nginx ;;
        *) echo "无效选择" ;;
    esac
}

clean_all() {
    echo "⚠️  警告：此操作将删除所有容器和数据，无法恢复！"
    read -p "确定要继续吗？(y/N): " confirm
    
    if [[ $confirm == [yY] ]]; then
        echo "正在删除所有容器和数据..."
        docker-compose down -v
        docker system prune -f
        echo "✅ 清理完成"
    else
        echo "操作已取消"
    fi
}

rebuild_images() {
    echo "正在重新构建镜像..."
    docker-compose build --no-cache
    echo "✅ 镜像重建完成"
}

show_resources() {
    echo "Docker 资源使用情况:"
    docker stats --no-stream
    echo
    echo "磁盘使用情况:"
    docker system df
}

# 检查 Docker 是否运行
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        echo "❌ Docker 未运行，请先启动 Docker 服务"
        exit 1
    fi
}

# 检查 docker-compose 是否存在
check_compose() {
    if ! command -v docker-compose &> /dev/null; then
        echo "❌ docker-compose 未安装，请先安装 docker-compose"
        exit 1
    fi
}

# 主程序
main() {
    check_docker
    check_compose
    
    show_banner
    
    while true; do
        show_menu
        read -p "请输入选择的数字: " choice
        
        case $choice in
            1) start_services ;;
            2) stop_services ;;
            3) restart_services ;;
            4) show_status ;;
            5) show_logs ;;
            6) clean_all ;;
            7) rebuild_images ;;
            8) show_resources ;;
            0) echo "再见！"; exit 0 ;;
            *) echo "无效选择，请重新输入" ;;
        esac
        
        echo
        read -p "按回车键继续..."
        echo
    done
}

# 执行主程序
main
