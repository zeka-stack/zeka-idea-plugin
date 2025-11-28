# 部署说明

## 部署脚本

### 功能说明

`deploy.sh` 负责版本号更新、Gradle `publishPlugin`、构建产物上传、Landing 页面与文档部署，确保 `intelli-ai-changelog` 发布流程标准化。

### 使用方法

1. 在仓库根目录执行：
   ```bash
   cd /Users/dong4j/Developer/0.Worker/opensource/zeka.stack/zeka-idea-plugin/intelli-ai-changelog
   bash deploy.sh -v 1.0.1
   ```
2. 常用参数：
    - `-v <version>`：调用 `update_version.sh` 同步版本号。
    - `-l`：仅部署 `landing.html`。
    - `-z`：仅上传 ZIP（重命名为 `chl.zip`）。
    - `-d`：仅同步 `docs/`。
3. 部署完成后可访问：
    - Landing：`https://chl.dong4j.site/`
    - ZIP：`https://chl.dong4j.site/chl.zip`
    - Docs：`https://chl.dong4j.site/docs`

### 注意事项

- 运行脚本前需确保 `gradlew`、`update_version.sh` 具备可执行权限。
- 服务器别名默认 `aliyun`，必要时可在脚本顶部自定义。
- `docs/` 同步采用 `rsync --delete`，确认本地内容正确后再执行。

## Nginx 配置

### 功能说明

`chl.dong4j.site.conf` 提供生产环境的站点配置，负责 HTTPS 强制跳转、ZIP 下载类型注册以及 Docsify 文档路由。

### 部署步骤

1. 将配置复制到服务器 `/etc/nginx/sites-available/chl.dong4j.site.conf`。
2. 创建软链接至 `sites-enabled/` 并执行 `nginx -t`。
3. 重载服务：`sudo systemctl reload nginx`。

### 目录映射

- `/var/www/chl-landing`：`landing.html` 与 `chl.zip`。
- `/var/www/chl-docs`：Docsify 文档根目录（默认 `guide/index.html`）。


