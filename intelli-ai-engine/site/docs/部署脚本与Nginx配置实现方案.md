# 部署脚本与 Nginx 配置实现方案

## 功能概述

- 为 `intelli-ai-engine` 插件提供独立的部署脚本，与 `intelli-ai-javadoc/deploy.sh` 保持一致
- 支持完整的发布流程：版本更新、Gradle 发布、ZIP 上传、Landing 页面与用户手册部署
- 新增 `eng.dong4j.site` 的 Nginx 配置以托管静态站点、ZIP 包与 Docsify 文档

## 需求细化

1. Shell 脚本
    - 脚本位置：`intelli-ai-engine/deploy.sh`
    - 支持参数：`-v`、`-l`、`-z`、`-d`
    - 远程主机：`aliyun`
    - 默认路径（待确认）：`/var/www/eng-landing`、`/var/www/eng-docs`
    - ZIP 处理：匹配 `build/distributions/intelli-ai-engine-*.zip`，上传并命名为 `eng.zip`
    - Landing 源文件：`landing.html`；文档目录：`docs/`
2. Nginx 配置
    - 新增文件：`intelli-ai-engine/eng.dong4j.site.conf`
    - HTTP -> HTTPS 重定向
    - HTTPS 证书：`/etc/nginx/encrypt/fullchain.pem`、`/etc/nginx/encrypt/privkey.pem`
    - 站点根目录：`/var/www/eng-landing`
    - `/eng.zip` 配置 `Content-Disposition` 下载
    - `/docs` 使用 `alias /var/www/eng-docs/`，`try_files $uri $uri/ /guide/index.html`

## 实现方案

### Shell 脚本

1. 基于 `intelli-ai-javadoc/deploy.sh` 复制
2. 替换变量
    - `ZIP_DIR` 路径、ZIP 文件名前缀
    - `DEST_ZIP_NAME="eng.zip"`
    - `REMOTE_DIR`、`REMOTE_DOCS_DIR`
    - Echo 输出中的产品名与下载地址
3. 保持 rsync/ssh/权限设置逻辑
4. 若 `update_version.sh` 不存在，后续决策是否新增；暂按 javadoc 逻辑调用

### Nginx 配置

1. 复制 `aij.dong4j.site.conf`
2. 修改 `server_name` 为 `eng.dong4j.site`
3. 更新 root 与 alias 目录
4. 更新 ZIP 路径为 `/eng.zip`
5. 保留 Docs 的 `try_files $uri $uri/ /guide/index.html`

## 涉及文件

- `intelli-ai-engine/deploy.sh`
- `intelli-ai-engine/eng.dong4j.site.conf`
- 参考：`intelli-ai-javadoc/deploy.sh`、`aij.dong4j.site.conf`

## 技术细节

- Shell 使用 `set -e`，对参数进行 `getopts` 解析
- `rsync` 命令排除 `node_modules` / `.DS_Store`
- 远程权限：文件 `644`，目录 `755`
- Nginx 配置需通过 `nginx -t` 校验后 reload

## 测试计划

1. 本地 dry-run：使用 `-l/-z/-d` 参数验证逻辑路径（必要时注释掉 rsync 执行）
2. 远程环境：执行完整 `./deploy.sh -v <version>` 观察四步日志
3. Nginx：`nginx -t && sudo systemctl reload nginx`，验证访问

---

> **等待确认**：请确认域名 `eng.dong4j.site` 以及远程目录 `eng-landing` / `eng-docs` 是否符合预期，确认后再进入编码阶段。

