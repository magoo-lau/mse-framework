#!/bin/sh
# =============================================================================
# 统一配置写入脚本：读取本目录 config/ 下的 *.yaml 原始配置留档，写入 Nacos 配置中心。
#
# 用法：
#   sh docker/nacos/init-configs.sh              # 幂等导入：仅补缺失的 Data ID（已存在跳过，保护线上修改）
#   sh docker/nacos/init-configs.sh --force      # 强制覆盖：以本地留档为准统一写入全部 Data ID
#   sh docker/nacos/init-configs.sh mse-gateway.yaml [more-data-id ...]
#                                                # 定向覆盖：只强制写入指定 Data ID（改单个配置不打扰其余）
#
# 环境变量：
#   NACOS_ADDR        默认 http://localhost:8848（compose 内为 http://nacos:8848）
#   NACOS_GROUP       默认 DEFAULT_GROUP
#   NACOS_NAMESPACE   默认 public
#   NACOS_AUTH_IDENTITY_KEY/VALUE  默认 serverIdentity/security（与 docker-compose.yml 对齐，
#                     走 Nacos v3 admin API 鉴权放行；v2 Open API 在 Nacos 3 已废弃）
#   CONFIG_DIR        默认 <脚本目录>/config（compose 容器内挂载为 /nacos/config）
# =============================================================================
set -eu

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NACOS_ADDR="${NACOS_ADDR:-http://localhost:8848}"
GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
NAMESPACE="${NACOS_NAMESPACE:-public}"
IDENTITY_KEY="${NACOS_AUTH_IDENTITY_KEY:-serverIdentity}"
IDENTITY_VALUE="${NACOS_AUTH_IDENTITY_VALUE:-security}"
CONFIG_DIR="${CONFIG_DIR:-$SCRIPT_DIR/config}"

FORCE=0
if [ "${1:-}" = "--force" ]; then
  FORCE=1
  shift
fi

# 定向覆盖名单（Data ID = 文件名）：非空时仅写入这些文件，其余忽略
ONLY="$*"
if [ -n "$ONLY" ] && [ "$FORCE" = "1" ]; then
  echo ">>> --force 与定向 Data ID 不可同时使用（定向本身即覆盖）" >&2
  exit 1
fi

exist() {
  curl -fs -H "$IDENTITY_KEY: $IDENTITY_VALUE" \
    "$NACOS_ADDR/nacos/v3/admin/cs/config?dataId=$1&groupName=$GROUP&namespaceId=$NAMESPACE" >/dev/null 2>&1
}

publish() {
  # 内容经 stdin 传给 curl（content@-）：Windows 原生 curl 走 argv 会把 UTF-8 按本地码页转码成乱码，
  # 走文件路径（content@$2）又不识别 Git Bash 的 /d/... 路径，stdin 在两类环境都按字节透传
  cat "$2" | curl -fsS -X POST -H "$IDENTITY_KEY: $IDENTITY_VALUE" \
    "$NACOS_ADDR/nacos/v3/admin/cs/config" \
    --data-urlencode "dataId=$1" \
    --data-urlencode "groupName=$GROUP" \
    --data-urlencode "namespaceId=$NAMESPACE" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@-" >/dev/null
}

if [ ! -d "$CONFIG_DIR" ]; then
  echo "配置目录不存在：$CONFIG_DIR" >&2
  exit 1
fi

[ "$FORCE" = "1" ] && echo ">>> 强制覆盖模式：以本地留档 $CONFIG_DIR 为准统一写入" \
  || { [ -n "$ONLY" ] && echo ">>> 定向覆盖模式：仅写入 $ONLY" \
  || echo ">>> 幂等模式：仅导入缺失的 Data ID（--force 可强制覆盖）"; }

ok=0
skip=0
for file in "$CONFIG_DIR"/*.yaml; do
  [ -e "$file" ] || continue
  dataId="$(basename "$file")"
  if [ -n "$ONLY" ]; then
    case " $ONLY " in
      *" $dataId "*) ;;      # 命中定向名单：继续写入
      *) continue ;;         # 未命中：直接忽略
    esac
  elif [ "$FORCE" != "1" ] && exist "$dataId"; then
    echo "== 跳过 $dataId（已存在，不覆盖）"
    skip=$((skip + 1))
    continue
  fi
  echo "== 写入 $dataId"
  publish "$dataId" "$file"
  ok=$((ok + 1))
done

echo "Nacos 配置写入完成：$ok 个写入，$skip 个跳过（共 $((ok + skip)) 个，Group=$GROUP，Namespace=$NAMESPACE，$NACOS_ADDR）"
