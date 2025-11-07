#!/bin/bash

# 版本更新脚本
# 用法: ./update_version.sh <version>
# 示例: ./update_version.sh 1.4.0

set -e

# 检查参数
if [ $# -eq 0 ]; then
    echo "错误: 请提供版本号"
    echo "用法: $0 <version>"
    echo "示例: $0 1.4.0"
    exit 1
fi

NEW_VERSION="$1"

# 验证版本号格式 (x.y.z)
if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "错误: 版本号格式不正确，应为 x.y.z (例如: 1.4.0)"
    exit 1
fi

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 提取主版本号 (x.y) 用于 Version badge
MAJOR_VERSION=$(echo "$NEW_VERSION" | cut -d. -f1,2)

echo "正在更新版本号..."
echo "新版本: $NEW_VERSION"
echo "主版本: $MAJOR_VERSION.x"

# 1. 更新 gradle.properties 中的 pluginVersion
GRADLE_PROPERTIES="gradle.properties"
if [ ! -f "$GRADLE_PROPERTIES" ]; then
    echo "错误: 找不到 $GRADLE_PROPERTIES 文件"
    exit 1
fi

# 更新 pluginVersion
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS 使用 sed -i ''
    sed -i '' "s/^pluginVersion=.*/pluginVersion=$NEW_VERSION/" "$GRADLE_PROPERTIES"
else
    # Linux 使用 sed -i
    sed -i "s/^pluginVersion=.*/pluginVersion=$NEW_VERSION/" "$GRADLE_PROPERTIES"
fi

echo "✓ 已更新 $GRADLE_PROPERTIES 中的 pluginVersion 为 $NEW_VERSION"

# 2. 更新 landing.html 中的 Version badge
LANDING_HTML="landing.html"
if [ ! -f "$LANDING_HTML" ]; then
    echo "警告: 找不到 $LANDING_HTML 文件，跳过更新"
else
    # 更新 Version badge (匹配 Version x.y.x 格式)
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS 使用 sed -i '' 和 -E 选项启用扩展正则表达式
        sed -i '' -E "s/Version [0-9]+\.[0-9]+\.x/Version $MAJOR_VERSION.x/g" "$LANDING_HTML"
    else
        # Linux 使用 sed -i 和 -E 选项启用扩展正则表达式
        sed -i -E "s/Version [0-9]+\.[0-9]+\.x/Version $MAJOR_VERSION.x/g" "$LANDING_HTML"
    fi
    
    echo "✓ 已更新 $LANDING_HTML 中的 Version badge 为 Version $MAJOR_VERSION.x"
fi

# 验证更新结果
echo ""
echo "更新完成！"
echo ""
echo "验证结果:"
echo "  - $GRADLE_PROPERTIES:"
grep "^pluginVersion=" "$GRADLE_PROPERTIES" || echo "    警告: 未找到 pluginVersion"
echo "  - $LANDING_HTML:"
grep -oE "Version [0-9]+\.[0-9]+\.x" "$LANDING_HTML" 2>/dev/null || echo "    警告: 未找到 Version badge"

