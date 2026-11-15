# Elasticsearch 9.x + smartcn 中文分析（搜索引擎服务 mse-search 的默认引擎）
# 官方发行版自带 bundled 插件 analysis-smartcn，离线安装无需外网；
# 如需 IK：把版本匹配的 analysis-ik zip 放入本目录后改为
#   COPY analysis-ik.zip /tmp/analysis-ik.zip
#   RUN bin/elasticsearch-plugin install --batch file:///tmp/analysis-ik.zip
# 并在 Nacos mse-search.yaml 把 mse.search.analyzer 改为 ik_max_word（已有索引需重建）。
# 服务端取 9.2 线最新 9.2.8（9.2.9 tag 不存在——那是 BOM 仲裁的客户端 elasticsearch-java 版本号；
# 同 minor 内客户端 9.2.9 ↔ 服务端 9.2.8 兼容）
ARG ES_VERSION=9.2.8
FROM docker.elastic.co/elasticsearch/elasticsearch:${ES_VERSION}
RUN bin/elasticsearch-plugin install --batch --silent analysis-smartcn
