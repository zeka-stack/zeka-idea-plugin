#!/bin/bash

# 快速验证测试是否可以正常运行
# 此脚本仅运行少量测试以快速验证设置

set -e

echo "========================================"
echo "快速测试验证"
echo "========================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}运行快速验证测试...${NC}"
echo ""

# 运行单个测试类作为快速验证
./gradlew test --tests "dev.dong4j.zeka.stack.idea.plugin.settings.SettingsStateTest" --quiet

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ 验证通过！测试环境配置正确。${NC}"
    echo ""
    echo "现在可以运行完整的测试套件："
    echo "  ./run-tests.sh"
    echo ""
    echo "或使用 Gradle 直接运行："
    echo "  ./gradlew test"
else
    echo ""
    echo -e "${RED}✗ 验证失败！${NC}"
    echo ""
    echo "请检查："
    echo "  1. 是否已安装 JDK 17+"
    echo "  2. 是否有网络连接（用于下载依赖）"
    echo "  3. 查看详细错误信息"
    exit 1
fi

echo "========================================"
echo "验证完成"
echo "========================================"

