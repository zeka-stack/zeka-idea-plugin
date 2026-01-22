#!/bin/bash

# 部署脚本：构建 WebUI、上传静态资源并同步 Nginx 配置

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 服务器配置
SERVER_ALIAS="aliyun"
REMOTE_DIR="/var/www/zeka-idea-webui/dist"
REMOTE_NGINX_DIR="/etc/nginx/conf.d"
NGINX_CONF_LOCAL="zekastack.dong4j.site.conf"

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_DIR="${SCRIPT_DIR}/dist"

# 解析命令行参数
NGINX_ONLY=false
while getopts "n" opt; do
  case $opt in
    n)
      NGINX_ONLY=true
      ;;
    \?)
      echo -e "${RED}用法: $0 [-n]${NC}"
      echo -e "  -n  仅部署 Nginx 配置文件（跳过构建和静态资源上传）"
      exit 1
      ;;
  esac
done

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Zeka Stack Site 部署工具${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 如果只部署 Nginx 配置，跳过构建和上传步骤
if [ "$NGINX_ONLY" = true ]; then
  echo -e "${YELLOW}[模式] 仅部署 Nginx 配置${NC}"
  echo ""
  # 直接跳转到步骤 3
else
  # 步骤 1: 构建 WebUI
  echo -e "${YELLOW}[步骤 1/3] 构建 WebUI...${NC}"
  echo ""

cd "${SCRIPT_DIR}"

# 检查 node_modules 是否存在
if [ ! -d "node_modules" ]; then
  echo -e "${YELLOW}正在安装依赖...${NC}"
  pnpm install
  if [ $? -ne 0 ]; then
    echo -e "${RED}依赖安装失败${NC}"
    exit 1
  fi
fi

# 执行构建
pnpm run build

if [ $? -ne 0 ]; then
  echo -e "${RED}WebUI 构建失败${NC}"
  exit 1
fi

  echo ""
  echo -e "${GREEN}✓${NC} WebUI 构建完成"
  echo ""

  # 步骤 2: 部署到服务器
  echo -e "${YELLOW}[步骤 2/3] 部署到服务器 ${SERVER_ALIAS}:${REMOTE_DIR}...${NC}"
  echo ""

  # 检查构建产物是否存在
  if [ ! -d "${DIST_DIR}" ]; then
    echo -e "${RED}错误: 构建产物目录不存在: ${DIST_DIR}${NC}"
    exit 1
  fi

  # 在远程服务器创建目录（如果不存在）
  ssh ${SERVER_ALIAS} "mkdir -p ${REMOTE_DIR}"

  if [ $? -ne 0 ]; then
    echo -e "${RED}无法连接到服务器或创建目录失败${NC}"
    exit 1
  fi

  # 使用 rsync 同步文件（增量同步，删除远程多余文件）
  rsync -avz --delete --progress "${DIST_DIR}/" "${SERVER_ALIAS}:${REMOTE_DIR}/"

  if [ $? -ne 0 ]; then
    echo -e "${RED}文件上传失败${NC}"
    exit 1
  fi

  echo ""
  echo -e "${GREEN}✓${NC} 部署完成"
  echo ""
fi

# 步骤 3: 同步 Nginx 配置
if [ "$NGINX_ONLY" = true ]; then
  echo -e "${YELLOW}[步骤 1/1] 同步 Nginx 配置...${NC}"
else
  echo -e "${YELLOW}[步骤 3/3] 同步 Nginx 配置...${NC}"
fi
echo ""

if [ ! -f "${SCRIPT_DIR}/${NGINX_CONF_LOCAL}" ]; then
  echo -e "${RED}错误: 找不到 Nginx 配置文件: ${SCRIPT_DIR}/${NGINX_CONF_LOCAL}${NC}"
  exit 1
fi

rsync -avz --progress "${SCRIPT_DIR}/${NGINX_CONF_LOCAL}" "${SERVER_ALIAS}:${REMOTE_NGINX_DIR}/"

if [ $? -ne 0 ]; then
  echo -e "${RED}Nginx 配置上传失败${NC}"
  exit 1
fi

ssh ${SERVER_ALIAS} "nginx -t && systemctl restart nginx"

if [ $? -ne 0 ]; then
  echo -e "${RED}Nginx 重载失败${NC}"
  exit 1
fi

echo ""
echo -e "${GREEN}✓${NC} Nginx 配置已同步"
echo ""

# 完成
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}🎉 全部部署完成！${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}部署信息:${NC}"
echo -e "  - 服务器: ${BLUE}${SERVER_ALIAS}${NC}"
echo -e "  - 目录: ${BLUE}${REMOTE_DIR}${NC}"
echo -e "  - 站点: ${BLUE}https://zekastack.dong4j.site${NC}"
echo ""
echo -e "${YELLOW}本地命令:${NC}"
echo -e "  - 运行 ${BLUE}pnpm run dev${NC} 启动开发服务器"
echo -e "  - 运行 ${BLUE}pnpm run build${NC} 构建站点"
echo -e "  - 运行 ${BLUE}pnpm run preview${NC} 预览构建结果"
echo ""
