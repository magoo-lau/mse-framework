#!/bin/sh
# =============================================================================
# 框架区全量构建：mvn install（reactor：mse-dependencies → mse-common → mse-starter
# → mse-api → mse-service → mse-boot），产物安装到本地/私服仓库。
#
# 用法：
#   sh build.sh                       # 缺省跳过测试
#   MSE_RUN_ALL_TESTS=1 sh build.sh   # 放开单元测试
#   MAVEN_ARGS="-s /path/settings.xml" sh build.sh   # 透传给 mvn
#
# 构建成功后，业务工程（mse-modules / mse-mes / mse-oa / mse-mp 各仓）与示例工程
# （mse-sample 仓）即可经 mse-starter-parent + mse-dependencies BOM 引用本仓产物。
# 跨仓构建顺序：本仓 → 各业务仓 → mse-sample 仓（mse-monolith 聚合全部 biz 发布件，最后构建）。
# =============================================================================
set -eu
cd "$(dirname "$0")"

MVN="mvn -B -ntp ${MAVEN_ARGS:-}"
if [ "${MSE_RUN_ALL_TESTS:-0}" = "1" ]; then
  $MVN install
else
  $MVN install -DskipTests
fi
echo "框架区构建完成"
