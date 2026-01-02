#!/bin/bash

# 版本更新脚本
# 用法: ./update_version.sh <version>
# 示例: ./update_version.sh 2025.3.1
#
# 功能:
# 1. 只处理白名单中指定的插件
# 2. 更新每个插件 gradle.properties 中的 pluginVersion、kitVersion、engineVersion
# 3. 只更新存在的字段，不存在的字段跳过

set -e

# 检查参数
if [ $# -eq 0 ]; then
    echo "错误: 请提供版本号"
    echo "用法: $0 <version>"
    echo "示例: $0 2025.3.1"
    exit 1
fi

NEW_VERSION="$1"

# 验证版本号格式 (x.y.z 或 x.y.z.w)
if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)+$ ]]; then
    echo "错误: 版本号格式不正确，应为 x.y.z 或 x.y.z.w (例如: 2025.3.1)"
    exit 1
fi

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 定义白名单插件列表（插件目录名）
PLUGIN_WHITELIST=(
    "intelli-ai-engine"
    "intelli-ai-javadoc"
    "intelli-ai-changelog"
    "intelli-ai-nacos"
    "intelli-ai-swagger"
    "intelli-ai-tracer"
    "idea-plugin-kit"
    "zks-dev-helper"
    "archiver-man"
)

# 需要更新的字段列表
FIELDS_TO_UPDATE=(
    "pluginVersion"
    "kitVersion"
    "engineVersion"
)

echo "=========================================="
echo "版本更新脚本"
echo "=========================================="
echo "新版本: $NEW_VERSION"
echo "白名单插件数量: ${#PLUGIN_WHITELIST[@]}"
echo ""

# 统计信息
SUCCESS_COUNT=0
SKIP_COUNT=0
ERROR_COUNT=0

# 遍历白名单中的每个插件
for PLUGIN_DIR in "${PLUGIN_WHITELIST[@]}"; do
    PLUGIN_PATH="$SCRIPT_DIR/$PLUGIN_DIR"
    GRADLE_PROPERTIES="$PLUGIN_PATH/gradle.properties"
    
    echo "----------------------------------------"
    echo "处理插件: $PLUGIN_DIR"
    
    # 检查插件目录是否存在
    if [ ! -d "$PLUGIN_PATH" ]; then
        echo "  ⚠️  跳过: 插件目录不存在"
        ((SKIP_COUNT++))
        continue
    fi
    
    # 检查 gradle.properties 文件是否存在
    if [ ! -f "$GRADLE_PROPERTIES" ]; then
        echo "  ⚠️  跳过: gradle.properties 文件不存在"
        ((SKIP_COUNT++))
        continue
    fi
    
    # 更新每个字段
    UPDATED_FIELDS=()
    SKIPPED_FIELDS=()
    
    for FIELD in "${FIELDS_TO_UPDATE[@]}"; do
        # 检查字段是否存在
        if grep -q "^${FIELD}=" "$GRADLE_PROPERTIES"; then
            # 字段存在，进行更新
            if [[ "$OSTYPE" == "darwin"* ]]; then
                # macOS 使用 sed -i ''
                sed -i '' "s/^${FIELD}=.*/${FIELD}=${NEW_VERSION}/" "$GRADLE_PROPERTIES"
            else
                # Linux 使用 sed -i
                sed -i "s/^${FIELD}=.*/${FIELD}=${NEW_VERSION}/" "$GRADLE_PROPERTIES"
            fi
            UPDATED_FIELDS+=("$FIELD")
        else
            # 字段不存在，跳过
            SKIPPED_FIELDS+=("$FIELD")
        fi
    done
    
    # 输出更新结果
    if [ ${#UPDATED_FIELDS[@]} -gt 0 ]; then
        echo "  ✓ 已更新字段: ${UPDATED_FIELDS[*]}"
        ((SUCCESS_COUNT++))
    fi
    
    if [ ${#SKIPPED_FIELDS[@]} -gt 0 ]; then
        echo "  ⊘ 跳过字段（不存在）: ${SKIPPED_FIELDS[*]}"
    fi
    
    # 验证更新结果
    echo "  验证结果:"
    for FIELD in "${UPDATED_FIELDS[@]}"; do
        CURRENT_VALUE=$(grep "^${FIELD}=" "$GRADLE_PROPERTIES" | cut -d'=' -f2)
        if [ "$CURRENT_VALUE" = "$NEW_VERSION" ]; then
            echo "    ✓ ${FIELD}=${CURRENT_VALUE}"
        else
            echo "    ✗ ${FIELD}=${CURRENT_VALUE} (期望: ${NEW_VERSION})"
            ((ERROR_COUNT++))
        fi
    done
done

# 输出总结
echo ""
echo "=========================================="
echo "更新完成！"
echo "=========================================="
echo "成功更新: $SUCCESS_COUNT 个插件"
echo "跳过: $SKIP_COUNT 个插件"
if [ $ERROR_COUNT -gt 0 ]; then
    echo "错误: $ERROR_COUNT 个字段更新失败"
    exit 1
fi
echo ""
echo "所有插件版本已更新为: $NEW_VERSION"
