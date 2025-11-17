# 部署脚本与 Nginx 配置实现方案

## 功能概述

- 为 `intelli-ai-changelog` 插件新增与 `intelli-ai-javadoc/deploy.sh` 等价的部署脚本
- 统一提供版本号更新、Gradle 发布、ZIP 构建上传、Landing 页面部署、Docs 部署四个步骤
- 配套新增 `chl.dong4j.site` 的 Nginx 配置，支持静态站点、ZIP 下载与 Docsify 文档访问

## 需求细化

1. Shell 脚本需求
    - 名称：`deploy.sh`
    - 位置：`intelli-ai-changelog/deploy.sh`
    - 参数：`-v`（版本号，需调用 `update_version.sh`）、`-l`（仅部署 landing）、`-z`（仅上传 zip）、`-d`（仅部署文档）
    - 默认执行顺序：`publishPlugin -> 上传 ZIP -> 部署 landing -> 部署 docs`
    - ZIP 命名：`chl.zip`（源文件 `build/distributions/intelli-ai-changelog-*.zip`）
    - Landing 与 Docs 源：分别为 `landing.html` 与 `docs/`
    - 远程目标（拟定，可确认）：`/var/www/chl-landing/`、`/var/www/chl-docs/`
    - 远程主机：沿用 `aliyun`
2. Nginx 配置需求
    - 新增文件：`intelli-ai-changelog/chl.dong4j.site.conf`
    - HTTP 自动跳转到 HTTPS
    - HTTPS 站点使用证书 `/etc/nginx/encrypt/fullchain.pem` 与 `/etc/nginx/encrypt/privkey.pem`
    - 根目录：`/var/www/chl-landing`，默认文档 `landing.html`
    - `/chl.zip` 提供下载并附带 `Content-Disposition`
    - `/docs` 使用 `alias /var/www/chl-docs/` 并 `try_files $uri $uri/ /guide/index.html`

## 实现方案

### Shell 脚本

1. 复制 `intelli-ai-javadoc/deploy.sh` 结构
2. 按以下内容替换
    - 插件名称与 ZIP 前缀替换为 `intelli-ai-changelog`
    - 常量替换：`DEST_ZIP_NAME="chl.zip"`、`REMOTE_DIR="/var/www/chl-landing"`、`REMOTE_DOCS_DIR="/var/www/chl-docs"` 等
    - 所有 echo 输出中的产品名改为 `intelli-ai-changelog`
3. 保持 rsync、权限设置、参数解析等逻辑一致
4. 复用同目录下的 `update_version.sh`（若不存在需后续规划）

### Nginx 配置

1. 复制 `intelli-ai-javadoc/aij.dong4j.site.conf`
2. 替换域名为 `chl.dong4j.site`
3. 替换 root/alias 目录为 changelog 对应路径
4. 将 ZIP 路径替换为 `/chl.zip`
5. 保持 Docs `try_files` 逻辑一致

## 涉及文件

- `intelli-ai-changelog/deploy.sh`
- `intelli-ai-changelog/aij.dong4j.site.conf`（参考文件，仅阅读）
- `intelli-ai-changelog/chl.dong4j.site.conf`（新建）

## 技术细节

- Shell 兼容 bash，开启 `set -e`
- 通过 `rsync` + `ssh` 与远程服务器交互
- 需要 `chmod` 设置远程文件权限
- `nginx` 配置需在服务器上额外 `ln -s` 到 `sites-enabled` 并 reload

## 测试计划

1. **本地**：`bash deploy.sh -l` / `-z` / `-d` 确认参数解析正确（通过 `echo` 观察）
2. **远程**：在测试服务器执行 `bash deploy.sh -l` 等，确认文件上传路径正确
3. **Nginx**：在服务器上 `nginx -t` 校验配置后 reload，访问 `https://chl.dong4j.site/`、`/chl.zip`、`/docs`

---

> **等待确认**：请确认远程目录 (`/var/www/chl-landing`、`/var/www/chl-docs`) 与域名 (`chl.dong4j.site`) 是否符合预期，确认后开始编码。

