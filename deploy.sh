#!/bin/bash

# 统一插件发布与部署脚本
# 用法: ./deploy.sh <plugin-name> [options]
# 例如: ./deploy.sh engine -v 1.5.0
#
# 支持的插件名称：
#   engine    - intelli-ai-engine
#   javadoc   - intelli-ai-javadoc
#   changelog - intelli-ai-changelog
#   nacos     - intelli-ai-nacos
#   tracer    - intelli-ai-tracer
#   swagger   - intelli-ai-swagger

set -e  # 遇到错误立即退出

# 脚本所在目录（项目根目录）
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

# 如果第一个参数是 -n，则执行全局 Nginx 配置部署，不依赖具体插件
if [ "${1:-}" = "-n" ]; then
    REMOTE_HOST="aliyun"
    IDEAPLUGIN_NGINX_CONF="$SCRIPT_DIR/ideaplugin.dong4j.site.conf"
    DOWNLOAD_NGINX_CONF="$SCRIPT_DIR/download.dong4j.site.conf"
    REMOTE_NGINX_DIR="/etc/nginx/conf.d"

    echo "================================"
    echo "开始部署全局 Nginx 配置 (无需指定插件)"
    echo "本地配置文件: $IDEAPLUGIN_NGINX_CONF"
    echo "本地配置文件: $DOWNLOAD_NGINX_CONF"
    echo "远程目录: $REMOTE_NGINX_DIR"
    echo "目标服务器: $REMOTE_HOST"
    echo "================================"

    if [ ! -f "$IDEAPLUGIN_NGINX_CONF" ]; then
        echo "错误: 找不到本地 Nginx 配置文件: $IDEAPLUGIN_NGINX_CONF"
        exit 1
    fi

    if [ ! -f "$DOWNLOAD_NGINX_CONF" ]; then
            echo "错误: 找不到本地 Nginx 配置文件: $DOWNLOAD_NGINX_CONF"
            exit 1
        fi

    echo "上传 Nginx 配置到 $REMOTE_HOST:$REMOTE_NGINX_DIR/ ..."
    rsync -avz --progress \
        "$IDEAPLUGIN_NGINX_CONF" \
        "$DOWNLOAD_NGINX_CONF" \
        "$REMOTE_HOST:$REMOTE_NGINX_DIR/"

    echo "Testing and reloading Nginx on server '$REMOTE_HOST'..."
    ssh "$REMOTE_HOST" "nginx -t && systemctl restart nginx"

    if [ $? -ne 0 ]; then
        echo "Error: Failed to reload Nginx on server '$REMOTE_HOST'."
        exit 1
    fi

    echo "Nginx configuration successfully updated and reloaded on '$REMOTE_HOST'."
    echo "================================"
    exit 0
fi

# 如果第一个参数是 -w，则执行 latest.html 部署，不依赖具体插件
if [ "${1:-}" = "-w" ]; then
    REMOTE_HOST="aliyun"
    REMOTE_ROOT_DIR="/var/www/zeka-idea-plugin"
    REMOTE_WHATSNEW_DIR="$REMOTE_ROOT_DIR"

    echo "================================"
    echo "开始部署 latest.html (What's New 聚合页面)"
    echo "目标服务器: $REMOTE_HOST"
    echo "远程目录: $REMOTE_WHATSNEW_DIR"
    echo "================================"

    GENERATE_WHATSNEW_SCRIPT="$SCRIPT_DIR/generate-whatsnew.sh"
    if [ ! -f "$GENERATE_WHATSNEW_SCRIPT" ]; then
        echo "错误: 找不到 generate-whatsnew.sh 脚本: $GENERATE_WHATSNEW_SCRIPT"
        exit 1
    fi

    echo "执行 generate-whatsnew.sh 生成 latest.html ..."
    bash "$GENERATE_WHATSNEW_SCRIPT"
    if [ $? -ne 0 ]; then
        echo "错误: 生成 latest.html 失败"
        exit 1
    fi

    LATEST_HTML_FILE="$SCRIPT_DIR/latest.html"

    if [ ! -f "$LATEST_HTML_FILE" ]; then
        echo "错误: 生成 latest.html 后未找到文件: $LATEST_HTML_FILE"
        exit 1
    fi

    BUYMYACOFFEE_HTML_FILE="$SCRIPT_DIR/buy-me-a-coffee.html"

    if [ ! -f "$BUYMYACOFFEE_HTML_FILE" ]; then
        echo "错误: buy-me-a-coffee.html 不存在: $BUYMYACOFFEE_HTML_FILE"
        exit 1
    fi

    echo "正在上传 latest.html 到 $REMOTE_HOST:$REMOTE_WHATSNEW_DIR/ ..."
    ssh "$REMOTE_HOST" "mkdir -p $REMOTE_WHATSNEW_DIR"
    rsync -avz --progress \
        "$LATEST_HTML_FILE" \
        "$BUYMYACOFFEE_HTML_FILE" \
        "$REMOTE_HOST:$REMOTE_WHATSNEW_DIR/"

    echo "设置 latest.html 文件权限..."
    ssh "$REMOTE_HOST" "chmod 644 $REMOTE_WHATSNEW_DIR/latest.html"
    echo "✓ latest.html 部署完成"
    echo "================================"
    exit 0
fi

# 检查参数
if [ $# -lt 1 ]; then
    echo "用法: $0 <plugin-name> [options] 或 $0 [-n|-w]"
    echo ""
    echo "支持的插件名称:"
    echo "  engine    - intelli-ai-engine"
    echo "  javadoc   - intelli-ai-javadoc"
    echo "  changelog - intelli-ai-changelog"
    echo "  nacos     - intelli-ai-nacos"
    echo "  tracer    - intelli-ai-tracer"
    echo "  archiver  - archiver-man"
    echo ""
    echo "选项:"
    echo "  -v <version>  指定版本号，会先调用 update_version.sh 更新版本号 (例如: -v 1.5.0)"
    echo "  -z           仅上传 zip 到阿里云"
    echo "  -d           仅部署 site 整个目录 (包含 landing.html, docs.html, docs/ 等)"
    echo "  -p           仅打包并发布到插件市场 (publishPlugin)"
    echo "  -n           部署 Nginx 配置并在远程服务器上重载 (无需指定插件名称)"
    echo "  -w           部署 latest.html (What's New 聚合页面) (无需指定插件名称，独立使用)"
    exit 1
fi

# 获取插件名称
PLUGIN_NAME="$1"
shift  # 移除第一个参数，剩余参数传递给后续处理

# 根据插件名称设置目录名和路径
case "$PLUGIN_NAME" in
    engine)
        PLUGIN_DIR_NAME="intelli-ai-engine"
        PLUGIN_PATH="engine"
        PLUGIN_ID="29152"
        ;;
    javadoc)
        PLUGIN_DIR_NAME="intelli-ai-javadoc"
        PLUGIN_PATH="javadoc"
        PLUGIN_ID="28835"
        ;;
    changelog)
        PLUGIN_DIR_NAME="intelli-ai-changelog"
        PLUGIN_PATH="changelog"
        PLUGIN_ID="29154"
        ;;
    nacos)
        PLUGIN_DIR_NAME="intelli-ai-nacos"
        PLUGIN_PATH="nacos"
        PLUGIN_ID="29156"
        ;;
    tracer)
        PLUGIN_DIR_NAME="intelli-ai-tracer"
        PLUGIN_PATH="tracer"
        PLUGIN_ID="29155"
        ;;
    swagger)
        PLUGIN_DIR_NAME="intelli-ai-swagger"
        PLUGIN_PATH="swagger"
        PLUGIN_ID=""
        ;;
    archiver)
        PLUGIN_DIR_NAME="archiver-man"
        PLUGIN_PATH="archiver"
        PLUGIN_ID=""
        ;;
    helper)
        PLUGIN_DIR_NAME="zks-dev-helper"
        PLUGIN_PATH="helper"
        PLUGIN_ID=""
        ;;
    *)
        echo "错误: 未知的插件名称 '$PLUGIN_NAME'"
        echo "支持的插件名称: engine javadoc changelog nacos tracer archiver"
        exit 1
        ;;
esac
PLUGIN_DIR="$SCRIPT_DIR/$PLUGIN_DIR_NAME"

# 检查插件目录是否存在
if [ ! -d "$PLUGIN_DIR" ]; then
    echo "错误: 插件目录不存在: $PLUGIN_DIR"
    exit 1
fi

# 检查是否有 site 目录，如果有则使用 site 目录，否则使用插件根目录
if [ -d "$PLUGIN_DIR/site" ]; then
    SITE_DIR="$PLUGIN_DIR/site"
    USE_SITE_DIR=true
else
    SITE_DIR="$PLUGIN_DIR"
    USE_SITE_DIR=false
fi

# 设置路径配置
REMOTE_HOST="aliyun"
REMOTE_ROOT_DIR="/var/www/zeka-idea-plugin"
REMOTE_BASE_DIR="$REMOTE_ROOT_DIR/$PLUGIN_DIR_NAME"
if [ "$USE_SITE_DIR" = true ]; then
    REMOTE_DIR="$REMOTE_BASE_DIR/site"
else
    REMOTE_DIR="$REMOTE_BASE_DIR"
fi
DEST_ZIP_NAME="$PLUGIN_DIR_NAME.zip"

ZIP_DIR="$PLUGIN_DIR/build/distributions"
LANDING_FILE="$SITE_DIR/landing-v2.html"
DOCS_DIR="$SITE_DIR/docs"

echo "================================"
echo "开始发布与部署 $PLUGIN_DIR_NAME"
echo "================================"
echo "插件目录: $PLUGIN_DIR"
echo "站点目录: $SITE_DIR"
echo "远程路径: $REMOTE_BASE_DIR"
echo "================================"

# 参数解析
VERSION=""
only_site=false
only_zip=false
only_publish=false
deploy_nginx=false
explicit_plugin_action=false  # 是否显式要求执行插件相关操作（publish/zip/site）
while getopts ":v:zdnp" opt; do
    case $opt in
        v)
            VERSION="$OPTARG"
            explicit_plugin_action=true
            ;;
        z)
            only_zip=true
            explicit_plugin_action=true
            ;;
        d)
            only_site=true
            explicit_plugin_action=true
            ;;
        p)
            only_publish=true
            explicit_plugin_action=true
            ;;
        n)
            deploy_nginx=true
            ;;
        \?)
            echo "用法: $0 $PLUGIN_NAME [-v <version>] [-z] [-d] [-p] [-n]"
            echo "  -v <version>  指定版本号，会先调用 update_version.sh 更新版本号 (例如: -v 1.5.0)"
            echo "  -z           仅上传 zip 到阿里云"
            echo "  -d           仅部署 site 整个目录 (包含 landing.html, docs.html, docs/ 等)"
            echo "  -p           仅打包并发布到插件市场 (publishPlugin)"
            echo "  -n           部署 Nginx 配置并在远程服务器上重载"
            exit 1
            ;;
        :)
            echo "错误: 选项 -$OPTARG 需要参数"
            echo "用法: $0 $PLUGIN_NAME [-v <version>] [-z] [-d] [-p] [-n]"
            exit 1
            ;;
    esac
done

# 如果指定了版本号，先更新版本号
if [ -n "$VERSION" ]; then
    echo "[0/4] 更新版本号为 $VERSION ..."
    UPDATE_VERSION_SCRIPT="$PLUGIN_DIR/update_version.sh"
    if [ ! -f "$UPDATE_VERSION_SCRIPT" ]; then
        echo "警告: 找不到 $UPDATE_VERSION_SCRIPT 文件，跳过版本号更新"
    else
        # 切换到插件目录执行更新脚本
        (cd "$PLUGIN_DIR" && bash "$UPDATE_VERSION_SCRIPT" "$VERSION")
        echo "✓ 版本号已更新为 $VERSION"
        echo ""
    fi
fi

# 根据参数决定执行哪些步骤
# 默认行为：如果只指定插件名（没有其他参数），按顺序执行 -p, -z, -d 三个操作
do_publish=false
do_zip=false
do_site=false

# 如果指定了 only_* 参数，则只执行指定的步骤
if $only_site || $only_zip || $only_publish; then
    if $only_publish; then
        do_publish=true
    fi
    if $only_zip; then
        do_zip=true
    fi
    if $only_site; then
        do_site=true
    fi
elif $deploy_nginx && ! $explicit_plugin_action; then
    # 如果只想部署 Nginx（例如: ./deploy.sh engine -n），且没有显式插件操作，则跳过插件相关步骤
    do_publish=false
    do_zip=false
    do_site=false
else
    # 默认情况：只指定插件名时，按顺序执行 -p, -z, -d 三个操作
    do_publish=true
    do_zip=true
    do_site=true
fi

# 显示将要执行的操作
echo "执行计划:"
if $do_publish; then
    echo "  [1/3] ✓ 发布到插件市场 (publishPlugin)"
else
    echo "  [跳过] 发布到插件市场"
fi
if $do_zip; then
    echo "  [2/3] ✓ 上传 ZIP 到服务器"
else
    echo "  [跳过] 上传 ZIP"
fi
if $do_site; then
    echo "  [3/3] ✓ 部署 site 目录"
else
    echo "  [跳过] 部署 site 目录"
fi
if $deploy_nginx; then
    echo "  [额外] ✓ 部署 Nginx 配置"
fi
echo ""

############################################
# 1) 执行 Gradle publishPlugin
############################################
if $do_publish; then
    echo "[1/3] 执行 Gradle 发布 :publishPlugin ..."
    (cd "$PLUGIN_DIR" && ./gradlew clean publishPlugin --no-daemon)
    echo "✓ 插件发布完成"
    if [ -n "$PLUGIN_ID" ]; then
        echo "  插件市场地址: https://plugins.jetbrains.com/plugin/$PLUGIN_ID"
    fi
else
    echo "[跳过] Gradle 发布 (根据参数设置)"
fi

############################################
# 2) 上传插件 ZIP 到服务器目录
############################################
if $do_zip; then
    echo "[2/3] 上传插件 ZIP 到服务器目录 ..."
    echo "查找构建产物 ZIP ..."
    if [ ! -d "$ZIP_DIR" ]; then
        echo "未找到构建目录 $ZIP_DIR，尝试先执行构建..."
        (cd "$PLUGIN_DIR" && ./gradlew buildPlugin --no-daemon)
    fi

    # 选取最新的插件 ZIP 文件
    ZIP_FILE=$(ls -t "$ZIP_DIR"/${PLUGIN_DIR_NAME}-*.zip 2>/dev/null | head -n1 || true)
    if [ -z "$ZIP_FILE" ]; then
        echo "未找到 $ZIP_DIR/${PLUGIN_DIR_NAME}-*.zip，尝试先执行构建..."
        (cd "$PLUGIN_DIR" && ./gradlew buildPlugin --no-daemon)
        ZIP_FILE=$(ls -t "$ZIP_DIR"/${PLUGIN_DIR_NAME}-*.zip 2>/dev/null | head -n1 || true)
        if [ -z "$ZIP_FILE" ]; then
            echo "错误: 构建后仍未找到 $ZIP_DIR/${PLUGIN_DIR_NAME}-*.zip"
            exit 1
        fi
    fi

    echo "✓ 找到 ZIP 文件: $ZIP_FILE"
    echo "正在上传 ZIP 到 $REMOTE_HOST:$REMOTE_BASE_DIR/$DEST_ZIP_NAME ..."
    # 创建远程基础目录（如果不存在）
    ssh "$REMOTE_HOST" "mkdir -p $REMOTE_BASE_DIR"
    rsync -avz --progress \
        "$ZIP_FILE" \
        "$REMOTE_HOST:$REMOTE_BASE_DIR/$DEST_ZIP_NAME"

    echo "设置 ZIP 文件权限..."
    ssh "$REMOTE_HOST" "chmod 644 $REMOTE_BASE_DIR/$DEST_ZIP_NAME"
    echo "✓ ZIP 文件上传完成"
else
    echo "[跳过] 上传 ZIP (根据参数设置)"
fi

############################################
# 3) 部署 site 整个目录 (landing/docs/静态资源等)
############################################
if $do_site; then
    echo "[3/3] 部署 site 整个目录 ..."

    # 检查 site 目录是否存在
    if [ ! -d "$SITE_DIR" ]; then
        echo "错误: 找不到 site 目录: $SITE_DIR"
        exit 1
    fi
    if [ ! -f "$LANDING_FILE" ]; then
        echo "错误: 找不到 $LANDING_FILE (请先生成新的落地页)"
        exit 1
    fi

    # 如果存在生成文档清单脚本，先生成 docs-list.json
    GENERATE_DOCS_LIST_SCRIPT="$SCRIPT_DIR/generate-docs-list.sh"
    if [ -f "$GENERATE_DOCS_LIST_SCRIPT" ]; then
        echo "执行 generate-docs-list.sh 生成文档清单..."
        bash "$GENERATE_DOCS_LIST_SCRIPT" "$DOCS_DIR"
        if [ $? -eq 0 ]; then
            echo "✓ 文档清单生成成功"
        else
            echo "警告: 文档清单生成失败，继续部署..."
        fi
    else
        echo "提示: 未找到 generate-docs-list.sh，跳过文档清单生成"
    fi

    echo "正在全量同步 site 目录到 $REMOTE_HOST:$REMOTE_DIR ..."
    ssh "$REMOTE_HOST" "mkdir -p $REMOTE_DIR"

    # 全量同步：确保目标目录与源目录完全一致
    # -a: archive mode (保持权限、时间戳等)
    # -v: verbose
    # -z: compress during transfer
    # --progress: 显示传输进度
    # --delete: 删除目标目录中源目录不存在的文件（确保完全一致）
    # --ignore-times: 忽略时间戳，强制检查所有文件（可选，如果需要强制全量传输）
    rsync -avz --delete --progress \
        --exclude 'node_modules' \
        --exclude '.DS_Store' \
        --exclude '*.log' \
        "$SITE_DIR/" \
        "$REMOTE_HOST:$REMOTE_DIR/"

    echo "正在上传新版 landing 页面并覆盖为 landing.html ..."
    rsync -avz --progress \
        "$LANDING_FILE" \
        "$REMOTE_HOST:$REMOTE_DIR/landing.html"

    echo "✓ site 目录部署完成"
else
    echo "[跳过] 部署 site 目录 (根据参数设置)"
fi

# 收尾输出
echo "================================"
echo "✓ 部署完成！"
if $do_publish; then
    echo "  - 插件已发布到 JetBrains Marketplace"
fi
if $do_zip; then
    echo "  - ZIP: $REMOTE_HOST:$REMOTE_BASE_DIR/$DEST_ZIP_NAME"
    echo "  - 下载地址: https://ideaplugin.dong4j.site/$PLUGIN_PATH/$DEST_ZIP_NAME"
fi
if $do_site; then
    echo "  - site 目录: $REMOTE_HOST:$REMOTE_DIR"
    echo "  - 访问地址: https://ideaplugin.dong4j.site/$PLUGIN_PATH/docs.html"
    echo "  - 访问地址: https://ideaplugin.dong4j.site/$PLUGIN_PATH/landing.html"
fi
if $deploy_nginx; then
    echo "  - Nginx: 配置已部署到 /etc/nginx/conf.d 并完成重载"
fi
echo "================================"
