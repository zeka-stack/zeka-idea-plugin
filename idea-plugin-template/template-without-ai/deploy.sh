#!/bin/bash

# IntelliJ IDEA Plugin Template 插件发布与部署脚本
# 1) 执行 Gradle 的 publishPlugin 流程
# 2) 将 build/distributions/*.zip 上传到服务器
# 3) 部署 landing.html 到服务器
# 4) 部署用户手册文档 (docs 目录) 到服务器

set -e  # 遇到错误立即退出

# 目录与路径配置
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/" && pwd)

REMOTE_HOST="aliyun"
REMOTE_DIR="/var/www/example-landing"
REMOTE_LANDING_PATH="$REMOTE_DIR/landing.html"
REMOTE_DOCS_DIR="/var/www/example-docs"
DEST_ZIP_NAME="example.zip"

ZIP_DIR="$SCRIPT_DIR/build/distributions"
LANDING_FILE="$SCRIPT_DIR/landing.html"
DOCS_DIR="$SCRIPT_DIR/docs"

echo "================================"
echo "开始发布与部署 IntelliJ IDEA Plugin Template"
echo "================================"

# 参数解析：
# -v 指定版本号，会先调用 update_version.sh 更新版本号
# -l 仅部署 landing.html
# -z 仅上传 zip 到服务器
# -d 仅部署用户手册文档
VERSION=""
only_landing=false
only_zip=false
only_docs=false
while getopts ":v:lzd" opt; do
    case $opt in
        v)
            VERSION="$OPTARG"
            ;;
        l)
            only_landing=true
            ;;
        z)
            only_zip=true
            ;;
        d)
            only_docs=true
            ;;
        \?)
            echo "用法: $0 [-v <version>] [-l] [-z] [-d]"
            echo "  -v <version>  指定版本号 (例如: -v 1.5.0)"
            echo "  -l           仅部署 landing.html"
            echo "  -z           仅上传 zip 到服务器"
            echo "  -d           仅部署用户手册文档"
            exit 1
            ;;
        :)
            echo "错误: 选项 -$OPTARG 需要参数"
            echo "用法: $0 [-v <version>] [-l] [-z] [-d]"
            exit 1
            ;;
    esac
done

# 根据参数决定执行哪些步骤
do_publish=true
do_zip=true
do_landing=true
do_docs=true

# 如果指定了任何 only_* 参数，则只执行指定的步骤
if $only_landing || $only_zip || $only_docs; then
    do_publish=false
    do_zip=false
    do_landing=false
    do_docs=false
    
    if $only_landing; then
        do_landing=true
    fi
    if $only_zip; then
        do_zip=true
    fi
    if $only_docs; then
        do_docs=true
    fi
fi

############################################
# 1) 执行 Gradle publishPlugin
############################################
if $do_publish; then
    echo "[1/4] 执行 Gradle 发布 :publishPlugin ..."
    "$REPO_ROOT/gradlew" clean publishPlugin --no-daemon
    echo "✓ 插件发布完成"
else
    echo "[跳过] Gradle 发布 (根据参数设置)"
fi

############################################
# 2) 上传插件 ZIP 到服务器目录
############################################
if $do_zip; then
    echo "[2/4] 查找构建产物 ZIP ..."
    if [ ! -d "$ZIP_DIR" ]; then
        echo "错误: 未找到构建目录 $ZIP_DIR，请确认构建是否成功"
        exit 1
    fi

    # 选取最新的 *.zip
    ZIP_FILE=$(ls -t "$ZIP_DIR"/*.zip 2>/dev/null | head -n1 || true)
    if [ -z "$ZIP_FILE" ]; then
        echo "错误: 未找到 $ZIP_DIR/*.zip"
        exit 1
    fi

    echo "✓ 找到 ZIP 文件: $ZIP_FILE"
    echo "正在上传 ZIP 到 $REMOTE_HOST:$REMOTE_DIR/$DEST_ZIP_NAME ..."
    rsync -avz --progress \
        "$ZIP_FILE" \
        "$REMOTE_HOST:$REMOTE_DIR/$DEST_ZIP_NAME"

    echo "设置 ZIP 文件权限..."
    ssh "$REMOTE_HOST" "chmod 644 $REMOTE_DIR/$DEST_ZIP_NAME"
    echo "✓ ZIP 文件上传完成"
else
    echo "[跳过] 上传 ZIP (根据参数设置)"
fi

############################################
# 3) 部署 landing.html
############################################
if $do_landing; then
    echo "[3/4] 部署 Landing Page ..."

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
    echo "✓ Landing Page 部署完成"
else
    echo "[跳过] 部署 Landing Page (根据参数设置)"
fi

############################################
# 4) 部署用户手册文档
############################################
if $do_docs; then
    echo "[4/4] 部署用户手册文档 ..."

    # 检查源目录是否存在
    if [ ! -d "$DOCS_DIR" ]; then
        echo "错误: 找不到文档目录 $DOCS_DIR"
        exit 1
    fi

    echo "✓ 源目录检查通过: $DOCS_DIR"
    echo "正在上传文档到 $REMOTE_HOST:$REMOTE_DOCS_DIR ..."

    # 创建远程目录（如果不存在）
    ssh "$REMOTE_HOST" "mkdir -p $REMOTE_DOCS_DIR"

    # 同步文档目录
    rsync -avz --progress \
        --exclude 'node_modules' \
        --exclude '.DS_Store' \
        --exclude '*.log' \
        --delete \
        "$DOCS_DIR/" \
        "$REMOTE_HOST:$REMOTE_DOCS_DIR/"

    echo "设置文档目录权限..."
    ssh "$REMOTE_HOST" "find $REMOTE_DOCS_DIR -type f -exec chmod 644 {} \; && find $REMOTE_DOCS_DIR -type d -exec chmod 755 {} \;"
    echo "✓ 用户手册文档部署完成"
else
    echo "[跳过] 部署用户手册文档 (根据参数设置)"
fi

# 收尾输出
echo "================================"
echo "✓ 部署完成！"
if $do_publish; then
    echo "  - 插件已发布到 JetBrains Marketplace"
fi
if $do_zip; then
    echo "  - ZIP: $REMOTE_HOST:$REMOTE_DIR/$DEST_ZIP_NAME"
fi
if $do_landing; then
    echo "  - HTML: $REMOTE_HOST:$REMOTE_LANDING_PATH"
fi
if $do_docs; then
    echo "  - DOCS: $REMOTE_HOST:$REMOTE_DOCS_DIR"
fi
echo "================================"

