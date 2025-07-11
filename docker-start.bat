@echo off
echo =================================
echo     MMMail Docker 管理脚本
echo =================================
echo.

:menu
echo 请选择操作:
echo [1] 启动所有服务
echo [2] 停止所有服务
echo [3] 重启所有服务
echo [4] 查看服务状态
echo [5] 查看服务日志
echo [6] 删除所有容器和数据
echo [0] 退出
echo.
set /p choice=请输入选择的数字: 

if "%choice%"=="1" goto start
if "%choice%"=="2" goto stop
if "%choice%"=="3" goto restart
if "%choice%"=="4" goto status
if "%choice%"=="5" goto logs
if "%choice%"=="6" goto clean
if "%choice%"=="0" goto exit
echo 无效选择，请重新输入
goto menu

:start
echo 正在启动所有服务...
docker-compose up -d
if %errorlevel%==0 (
    echo 服务启动成功！
    echo.
    echo 访问地址:
    echo 前端管理界面: http://localhost:8080
    echo 后端API接口: http://localhost:1024
    echo 数据库监控: http://localhost:8080/druid
    echo.
    echo 默认管理员账号: admin
    echo 默认密码: 123456
) else (
    echo 服务启动失败！
)
pause
goto menu

:stop
echo 正在停止所有服务...
docker-compose down
echo 服务已停止
pause
goto menu

:restart
echo 正在重启所有服务...
docker-compose restart
echo 服务重启完成
pause
goto menu

:status
echo 查看服务状态...
docker-compose ps
pause
goto menu

:logs
echo 查看最近的日志...
docker-compose logs --tail=50
pause
goto menu

:clean
echo 警告：此操作将删除所有容器和数据，无法恢复！
set /p confirm=确定要继续吗？(y/N): 
if /i "%confirm%"=="y" (
    echo 正在删除所有容器和数据...
    docker-compose down -v
    docker system prune -f
    echo 清理完成
) else (
    echo 操作已取消
)
pause
goto menu

:exit
echo 再见！
exit /b 0
