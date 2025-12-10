#!/bin/bash

# IntelliJ IntelliAI Javadoc 插件 - 测试运行脚本
# 此脚本用于运行所有单元测试

set -e

echo "========================================"
echo "IntelliJ IntelliAI Javadoc Plugin - 测试运行"
echo "========================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 运行测试
echo -e "${YELLOW}正在运行所有单元测试...${NC}"
echo ""

./gradlew clean test --info

# 检查测试结果
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ 所有测试通过！${NC}"
    echo ""
    echo "测试报告位置:"
    echo "  build/reports/tests/test/index.html"
    echo ""
    echo "测试覆盖率报告（如果生成）:"
    echo "  build/reports/jacoco/test/html/index.html"
else
    echo ""
    echo -e "${RED}✗ 测试失败！${NC}"
    echo ""
    echo "请查看详细错误信息："
    echo "  build/reports/tests/test/index.html"
    exit 1
fi

echo "========================================"
echo "测试完成"
echo "========================================"

