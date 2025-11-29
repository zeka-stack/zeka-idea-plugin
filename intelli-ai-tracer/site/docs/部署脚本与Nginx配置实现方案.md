# 部署脚本与 Nginx 配置实现方案

## 功能概述

- 为 `intelli-ai-tracer` 插件新增标准化部署脚本，执行发布、打包、部署全流程
- 构建 `tra.dong4j.site` 的 Nginx 配置，提供 Landing、ZIP 下载、Docs 文档访问

## 需求细化

1. Shell 脚本需求
    - 文件：`intelli-ai-tracer/deploy.sh`
    - 复用 `-v/-l/-z/-d` 选项，行为与 `intelli-ai-javadoc/deploy.sh` 相同
    - ZIP 文件匹配：`build/distributions/intelli-ai-tracer-*.zip`
    - 上传后重命名为 `tra.zip`
    - Landing 源文件：`landing.html`
    - Docs 源目录：`docs/`
    - 远程路径（待确认）：`/var/www/tra-landing`、`/var/www/tra-docs`
2. Nginx 配置
    - 新增 `intelli-ai-tracer/tra.dong4j.site.conf`
    - HTTP 转 HTTPS
    - 使用 `/etc/nginx/encrypt/fullchain.pem` 与 `/etc/nginx/encrypt/privkey.pem`
    - `root /var/www/tra-landing; index landing.html;`
    - `/tra.zip` 提供下载
    - `/docs` alias `/var/www/tra-docs/`，`try_files $uri $uri/ /guide/index.html`

## 实现方案

### Shell 脚本

1. 复制 `intelli-ai-javadoc/deploy.sh`
2. 替换常量与日志中的产品名：
    - `REMOTE_DIR=/var/www/tra-landing`
    - `REMOTE_DOCS_DIR=/var/www/tra-docs`
    - `DEST_ZIP_NAME="tra.zip"`
    - ZIP 匹配模式 `intelli-ai-tracer-*.zip`
    - 下载地址输出 `https://tra.dong4j.site/tra.zip`
3. 其余逻辑保持一致，包括 `rsync`、权限设置、`update_version.sh` 调用等

### Nginx 配置

1. 在 `intelli-ai-tracer` 中复制 `aij.dong4j.site.conf`
2. 修改 `server_name`、`root`、`alias` 等为 tracer 专用路径
3. 更换 ZIP 与 docs 路径

## 涉及文件

- `intelli-ai-tracer/deploy.sh`
- `intelli-ai-tracer/tra.dong4j.site.conf`
- 参考文件：`intelli-ai-javadoc/deploy.sh`、`intelli-ai-javadoc/aij.dong4j.site.conf`

## 技术细节

- Shell：`set -e`、`getopts` 解析参数
- `rsync` 排除 `node_modules/.DS_Store/*.log`
- 远程权限控制：ZIP & HTML 设为 `644`，目录 `755`
- Nginx 需在服务器执行 `nginx -t` 校验并 reload

## 测试计划

1. 本地运行 `./deploy.sh -l` 等快速验证参数路径
2. 远程执行带版本号的完整流程
3. 部署后访问 `https://tra.dong4j.site/`、`/tra.zip`、`/docs`

---

> **等待确认**：请确认远程目录、域名命名方案与脚本需求无误，确认后再进入编码实现。

