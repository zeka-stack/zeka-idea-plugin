#!/bin/bash

# ai-javadoc 插件发布与部署脚本
# 1) 执行 Gradle 的 publishPlugin 流程
# 2) 将 build/distributions/ai-javadoc-{version}.zip 重命名为 aij.zip 并上传到 /var/www/aij-landing/
# 3) 部署 landing.html 到 /var/www/aij-landing/landing.html

set -e  # 遇到错误立即退出

# 目录与路径配置
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/" && pwd)

REMOTE_HOST="aliyun"
REMOTE_DIR="/var/www/aij-landing"
REMOTE_LANDING_PATH="$REMOTE_DIR/landing.html"
DEST_ZIP_NAME="aij.zip"

ZIP_DIR="$SCRIPT_DIR/build/distributions"
LANDING_FILE="$SCRIPT_DIR/landing.html"

echo "================================"
echo "开始发布与部署 ai-javadoc"
echo "================================"

# 参数解析：
# -l 仅部署 landing.html
# -z 仅上传 zip 到阿里云
only_landing=false
only_zip=false
while getopts ":lz" opt; do
    case $opt in
        l)
            only_landing=true
            ;;
        z)
            only_zip=true
            ;;
        \?)
            echo "用法: $0 [-l] [-z]"
            echo "  -l 仅部署 landing.html"
            echo "  -z 仅上传 zip 到阿里云"
            exit 1
            ;;
    esac
done

# 根据参数决定执行哪些步骤
do_publish=true
do_zip=true
do_landing=true

if $only_landing && ! $only_zip; then
    do_publish=false
    do_zip=false
    do_landing=true
elif $only_zip && ! $only_landing; then
    do_publish=false
    do_zip=true
    do_landing=false
elif $only_zip && $only_landing; then
    do_publish=false
    do_zip=true
    do_landing=true
fi

############################################
# 1) 执行 Gradle publishPlugin
############################################
if $do_publish; then
    echo "[1/3] 执行 Gradle 发布 :publishPlugin ..."
    "$REPO_ROOT/gradlew" :publishPlugin --no-daemon
else
    echo "[跳过] Gradle 发布 (根据参数设置)"
fi

############################################
# 2) 上传插件 ZIP 为 aij.zip 到服务器目录
############################################
if $do_zip; then
    echo "[2/3] 查找构建产物 ZIP ..."
    if [ ! -d "$ZIP_DIR" ]; then
        echo "错误: 未找到构建目录 $ZIP_DIR，请确认构建是否成功"
        exit 1
    fi

    # 选取最新的 ai-javadoc-*.zip
    ZIP_FILE=$(ls -t "$ZIP_DIR"/ai-javadoc-*.zip 2>/dev/null | head -n1 || true)
    if [ -z "$ZIP_FILE" ]; then
        echo "错误: 未找到 $ZIP_DIR/ai-javadoc-*.zip"
        exit 1
    fi

    echo "✓ 找到 ZIP 文件: $ZIP_FILE"
    echo "正在上传 ZIP 到 $REMOTE_HOST:$REMOTE_DIR/$DEST_ZIP_NAME ..."
    rsync -avz --progress \
        "$ZIP_FILE" \
        "$REMOTE_HOST:$REMOTE_DIR/$DEST_ZIP_NAME"

    echo "设置 ZIP 文件权限..."
    ssh "$REMOTE_HOST" "chmod 644 $REMOTE_DIR/$DEST_ZIP_NAME"
else
    echo "[跳过] 上传 ZIP (根据参数设置)"
fi

############################################
# 3) 部署 landing.html
############################################
if $do_landing; then
    echo "[3/3] 部署 Landing Page ..."

    # 检查源文件是否存在
    if [ ! -f "$LANDING_FILE" ]; then
        echo "错误: 找不到文件 $LANDING_FILE"
        exit 1
    fi

    echo "✓ 源文件检查通过: $LANDING_FILE"
    echo "正在上传文件到 $REMOTE_HOST:$REMOTE_LANDING_PATH ..."

    rsync -avz --progress \
        "$LANDING_FILE" \
        "$REMOTE_HOST:$REMOTE_LANDING_PATH"

    echo "设置 landing.html 文件权限..."
    ssh "$REMOTE_HOST" "chmod 644 $REMOTE_LANDING_PATH"
else
    echo "[跳过] 部署 Landing Page (根据参数设置)"
fi

# 收尾输出
echo "================================"
echo "流程结束"
echo "ZIP 目标: $REMOTE_HOST:$REMOTE_DIR/$DEST_ZIP_NAME (如执行)"
echo "HTML 目标: $REMOTE_HOST:$REMOTE_LANDING_PATH (如执行)"
echo "================================"
