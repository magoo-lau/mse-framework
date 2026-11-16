#!/usr/bin/env bash
# =============================================================================
# 启动器镜像构建脚本 —— 每个启动器目录自带 Dockerfile，本脚本负责批量 / 按名调用
# 覆盖：微服务 boot-gateway/system/platform/auth/job
#
# 用法：
#   ./build-images.sh                                     # 构建全部（mse/xxx:1.0.0）
#   ./build-images.sh boot-system                         # 只构建指定启动器
#   ./build-images.sh boot-system boot-auth               # 任意子集
#   IMAGE_REPO=registry.example.com/mse ./build-images.sh # 指定仓库前缀
#   VERSION=v1.0.1 ./build-images.sh                      # 指定版本 tag
#   PUSH=1 IMAGE_REPO=registry.example.com/mse ./build-images.sh   # 构建并推送
#
# 可选启动器名：boot-gateway boot-system boot-platform boot-auth boot-job
# 业务工程（mse-modules 各 biz / 示例 mse-monolith）已拆分至各自仓库，镜像构建见对应仓库 docker/README.md。
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")/../.."

IMAGE_REPO="${IMAGE_REPO:-mse}"
VERSION="${VERSION:-1.0.0}"
PUSH="${PUSH:-0}"

# 启动器名 | 模块目录（相对仓库根）| Dockerfile | 端口
MODULES=(
  "boot-gateway|mse-framework/mse-boot/boot-gateway|mse-framework/mse-boot/boot-gateway/Dockerfile|48080"
  "boot-system|mse-framework/mse-boot/boot-system|mse-framework/mse-boot/boot-system/Dockerfile|48081"
  "boot-platform|mse-framework/mse-boot/boot-platform|mse-framework/mse-boot/boot-platform/Dockerfile|48082"
  "boot-auth|mse-framework/mse-boot/boot-auth|mse-framework/mse-boot/boot-auth/Dockerfile|48083"
  "boot-job|mse-framework/mse-boot/boot-job|mse-framework/mse-boot/boot-job/Dockerfile|48084"
)

ALL_NAMES="$(printf '%s\n' "${MODULES[@]}" | cut -d'|' -f1)"
wanted=("$@")
if [ ${#wanted[@]} -eq 0 ]; then
  wanted=(${ALL_NAMES})
fi

for name in "${wanted[@]}"; do
  entry="$(printf '%s\n' "${MODULES[@]}" | grep "^${name}|")" || {
    echo "未知启动器：${name}（可选：$(echo ${ALL_NAMES})）" >&2
    exit 1
  }
  IFS='|' read -r name path dockerfile port <<< "${entry}"
  tag="${IMAGE_REPO}/${name}:${VERSION}"
  echo "==> 构建镜像 ${tag}（模块 ${path}）"
  docker build -f "${dockerfile}" -t "${tag}" .
  if [ "${PUSH}" = "1" ]; then
    docker push "${tag}"
  fi
done

echo "完成。镜像列表："
docker images --filter=reference="${IMAGE_REPO}/*:${VERSION}" --format 'table {{.Repository}}\t{{.Tag}}\t{{.Size}}'
