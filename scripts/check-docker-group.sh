#!/usr/bin/env bash
# scripts/check-docker-group.sh
# 验证当前 shell 是否具有 docker 访问权限。被 run-tests-docker.sh 与 release-gate.sh source。
# 失败时退出码 2，附带可执行的修复指令。

set -euo pipefail

require_docker_access() {
  if ! command -v docker >/dev/null 2>&1; then
    cat >&2 <<'EOF'
[check-docker-group] docker CLI 未安装。
请先安装 Docker Engine + Compose 插件：
  Ubuntu/Debian: https://docs.docker.com/engine/install/ubuntu/
  Arch:          sudo pacman -S docker docker-compose
EOF
    exit 2
  fi

  if ! docker version --format '{{.Server.Version}}' >/dev/null 2>&1; then
    local in_group=0
    if id -nG "$USER" 2>/dev/null | tr ' ' '\n' | grep -qx docker; then
      in_group=1
    fi

    if [[ $in_group -eq 0 ]]; then
      cat >&2 <<EOF
[check-docker-group] 当前用户 ($USER) 不在 docker 组，无法访问 /var/run/docker.sock。

一次性配置（需要 sudo）：
  sudo usermod -aG docker "$USER"

每个新终端跑测试前刷新会话权限：
  newgrp docker

完成后重新执行本脚本。
EOF
      exit 2
    fi

    cat >&2 <<'EOF'
[check-docker-group] 用户已在 docker 组但当前 shell 未刷新组权限。

在当前终端执行：
  newgrp docker

或在调用方脚本前用：
  exec sg docker -c "<your command>"

如果上述都做了仍然失败，检查 docker daemon 是否在跑：
  sudo systemctl status docker
  sudo systemctl start docker
EOF
    exit 2
  fi

  if ! docker compose version >/dev/null 2>&1; then
    cat >&2 <<'EOF'
[check-docker-group] docker compose 插件未安装。
  Ubuntu/Debian: sudo apt install docker-compose-plugin
EOF
    exit 2
  fi
}

# 允许直接执行做自检
if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  require_docker_access
  echo "[check-docker-group] OK — docker $(docker version --format '{{.Server.Version}}'), compose $(docker compose version --short 2>/dev/null || echo unknown)"
fi
