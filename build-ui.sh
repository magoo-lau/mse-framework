#!/bin/sh
# =============================================================================
# 前端全量构建（本仓三工程）：逐工程 npm ci + type-check（--if-present）+ 生产构建。
#
# 用法：sh build-ui.sh [工程目录 ...]     # 缺省构建清单内全部工程
# 环境变量：
#   NPM_FLAGS   透传给 npm 的附加参数（如 --prefer-offline）
#
# mse-ui-shared 必须排在所有消费者之前（共享源码包，file: 源码直出，自身无构建产物依赖）；
# type-check 门禁试点：mse-ui / mse-ui-mobile / mse-ui-shared 三工程均登记 type-check。
# Windows 本地注意：dev server 运行中会锁 native 二进制，npm ci 前先停 dev server。
# =============================================================================
set -u

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

NPM="npm ${NPM_FLAGS:-}"

ALL_FRONTENDS="
mse-ui-shared
mse-ui
mse-ui-mobile
"

TARGETS=${*:-$ALL_FRONTENDS}

failed=""
total=0
for dir in $TARGETS; do
  [ -f "$dir/package.json" ] || { echo "!! 跳过 $dir（无 package.json）"; continue; }
  total=$((total + 1))
  echo "=== [$dir] npm ci ==="
  if (cd "$dir" && $NPM ci --no-audit --no-fund) \
      && echo "=== [$dir] type-check（如登记）===" \
      && (cd "$dir" && $NPM run type-check --if-present) \
      && echo "=== [$dir] build（如登记）===" \
      && (cd "$dir" && $NPM run build --if-present); then
    echo "--- [$dir] OK"
  else
    echo "--- [$dir] FAILED"
    failed="$failed $dir"
  fi
done

echo ""
if [ -n "$failed" ]; then
  echo "前端构建失败（$failed ），共 $total 个工程" >&2
  exit 1
fi
echo "前端全部构建通过（$total 个工程）"
