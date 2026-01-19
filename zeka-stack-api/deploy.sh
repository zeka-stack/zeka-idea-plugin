#!/bin/zsh

# 部署脚本：构建后端并发布到 m2

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 服务器配置
SERVER_ALIAS="m2"
REMOTE_DIR="~/Developer/Server"

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_DIR="${SCRIPT_DIR}/target"

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Zeka Stack API 部署工具${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 步骤 1: Maven 打包
echo -e "${YELLOW}[步骤 1/3] Maven 打包...${NC}"
echo ""

cd "${SCRIPT_DIR}"
mvn clean package -Dcheckstyle.skip=true -Dpmd.skip=true -Dmaven.test.skip=true

if [ $? -ne 0 ]; then
  echo -e "${RED}打包失败${NC}"
  exit 1
fi

echo ""
echo -e "${GREEN}✓${NC} 打包完成"
echo ""

# 步骤 2: 上传 tar.gz
echo -e "${YELLOW}[步骤 2/3] 上传 tar.gz 到 ${SERVER_ALIAS}:${REMOTE_DIR}...${NC}"
echo ""

TAR_FILE=$(ls -t "${TARGET_DIR}"/*.tar.gz 2>/dev/null | head -n 1)

if [ -z "${TAR_FILE}" ]; then
  echo -e "${RED}错误: 未找到 tar.gz 包${NC}"
  exit 1
fi

TAR_NAME=$(basename "${TAR_FILE}")

ssh ${SERVER_ALIAS} "mkdir -p ${REMOTE_DIR}"
if [ $? -ne 0 ]; then
  echo -e "${RED}无法连接到服务器或创建目录失败${NC}"
  exit 1
fi

rsync -avz --progress "${TAR_FILE}" "${SERVER_ALIAS}:${REMOTE_DIR}/"

if [ $? -ne 0 ]; then
  echo -e "${RED}上传失败${NC}"
  exit 1
fi

echo ""
echo -e "${GREEN}✓${NC} 上传完成: ${TAR_NAME}"
echo ""

# 步骤 3: 远程解压并启动
echo -e "${YELLOW}[步骤 3/3] 远程解压并启动...${NC}"
echo ""

REMOTE_CMD="cd ${REMOTE_DIR} && tar -zxvf ${TAR_NAME} && cd ${TAR_NAME%.tar.gz} && bin/launcher -r m2 -it"
ssh -t ${SERVER_ALIAS} "bash -lc '${REMOTE_CMD}'"

if [ $? -ne 0 ]; then
  echo -e "${RED}远程部署失败${NC}"
  exit 1
fi

echo ""
echo -e "${GREEN}✓${NC} 部署完成"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}🎉 全部部署完成！${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}部署信息:${NC}"
echo -e "  - 服务器: ${BLUE}${SERVER_ALIAS}${NC}"
echo -e "  - 目录: ${BLUE}${REMOTE_DIR}${NC}"
echo -e "  - 包名: ${BLUE}${TAR_NAME}${NC}"
echo ""
