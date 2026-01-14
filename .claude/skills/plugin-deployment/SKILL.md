---
name: plugin-deployment
description: 将 IntelliJ IDEA 插件部署到 JetBrains Marketplace 和远程服务器。用于发布插件、更新版本、部署站点文档或管理插件发布。支持版本管理、自动化部署工作流和文档生成。
---

# IntelliJ IDEA 插件部署

本 Skill 提供将 IntelliJ IDEA 插件部署到 JetBrains Marketplace 和远程服务器的指导。涵盖版本管理、自动化部署工作流、文档生成和服务器部署。

## 何时使用本 Skill

在以下场景使用本 Skill：

- 发布插件到 JetBrains Marketplace
- 部署插件 ZIP 文件到远程服务器
- 更新多个模块的插件版本
- 部署站点文档和落地页
- 生成 What's New 页面和文档清单
- 管理插件托管的 Nginx 配置
- 执行批量部署操作

## 部署脚本概览

### 1. `deploy.sh` - 统一部署脚本

**位置**: 项目根目录

**用途**: 发布和部署插件的主脚本。

**用法**:

```bash
# 部署插件并更新版本
./deploy.sh <plugin-name> -v <version>

# 仅执行特定操作
./deploy.sh <plugin-name> -p    # 仅发布到市场
./deploy.sh <plugin-name> -z    # 仅上传 ZIP
./deploy.sh <plugin-name> -d    # 仅部署站点目录
./deploy.sh <plugin-name> -n    # 部署 Nginx 配置

# 全局操作（无需指定插件名称）
./deploy.sh -n    # 部署全局 Nginx 配置
./deploy.sh -w    # 部署 latest.html (What's New 页面)
```

**支持的插件**:

- `engine` - intelli-ai-engine
- `javadoc` - intelli-ai-javadoc
- `changelog` - intelli-ai-changelog
- `nacos` - intelli-ai-nacos
- `tracer` - intelli-ai-tracer
- `swagger` - intelli-ai-swagger
- `archiver` - archiver-man
- `helper` - zks-dev-helper

**部署步骤** (未指定特定标志时):

1. **发布到市场** (`-p`): 执行 `gradlew publishPlugin`
2. **上传 ZIP** (`-z`): 上传插件 ZIP 到远程服务器
3. **部署站点** (`-d`): 部署站点目录（落地页、文档等）

**远程服务器配置**:

- 主机: `aliyun` (SSH 别名)
- 根目录: `/var/www/zeka-idea-plugin`
- 数据目录: `/var/www/data/intelli-ai-plugin`
- Nginx 配置: `/etc/nginx/conf.d`

### 2. `update_version.sh` - 版本管理脚本

**位置**: 项目根目录

**用途**: 更新所有白名单插件的版本。

**用法**:

```bash
# 将所有插件更新到指定版本
./update_version.sh 2025.3.1
```

**更新内容**:

1. **每个插件目录中的 gradle.properties**:
    - `pluginVersion`
    - `kitVersion`
    - `engineVersion` (如果存在)

2. **Nginx 配置** (`ideaplugin.dong4j.site.conf`):
    - 响应头中的版本号

**白名单插件**:

- intelli-ai-engine
- intelli-ai-javadoc
- intelli-ai-changelog
- intelli-ai-nacos
- intelli-ai-swagger
- intelli-ai-tracer
- idea-plugin-kit
- zks-dev-helper
- archiver-man

**版本格式**: `x.y.z` 或 `x.y.z.w` (例如: `2025.3.1`)

### 3. `generate-whatsnew.sh` - What's New 页面生成器

**位置**: 项目根目录

**用途**: 从所有插件的更新日志生成聚合的 What's New 页面。

**用法**:

```bash
# 生成 latest.html
./generate-whatsnew.sh
```

**功能**:

1. 扫描所有插件的 `includes/pluginChanges.html` 文件
2. 验证插件（检查 `plugin.xml`）
3. 从 `plugin.xml` 提取插件名称
4. 生成包含聚合更新日志的 `latest.html`
5. 复制到 `intelli-ai-engine/src/main/resources/whatsnew/latest.html`
6. 提取最新 Engine 版本并创建版本特定的 HTML 文件
7. 更新 `InternalWhatsNewProvider.java` 的版本映射

**输出文件**:

- `latest.html` (项目根目录) - 聚合更新日志页面
- `intelli-ai-engine/src/main/resources/whatsnew/latest.html` - 用于 IDE 显示的副本
- `intelli-ai-engine/src/main/resources/whatsnew/{version}.html` - 版本特定的更新日志

**允许的插件** (按顺序):

- intelli-ai-engine
- intelli-ai-javadoc
- intelli-ai-changelog
- intelli-ai-nacos
- intelli-ai-tracer

### 4. `generate-docs-list.sh` - 文档清单生成器

**位置**: 项目根目录

**用途**: 从文档目录中的 Markdown 文件生成 `docs-list.json`。

**用法**:

```bash
# 为插件生成 docs-list.json
./generate-docs-list.sh <docs-dir>
# 示例:
./generate-docs-list.sh intelli-ai-engine/site/docs
```

**功能**:

1. 扫描指定文档目录中的 `.md` 文件
2. 生成包含文件名和路径的 `docs-list.json`
3. 输出格式: 包含 `name` 和 `path` 字段的 JSON 数组

**输出格式**:

```json
[
  {
    "name": "quick-start",
    "path": "./docs/quick-start.md"
  },
  {
    "name": "user-guide",
    "path": "./docs/user-guide.md"
  }
]
```

### 5. `Makefile` - 构建和部署快捷操作

**位置**: 项目根目录

**用途**: 为常见的构建和部署操作提供便捷快捷方式。

**常用命令**:

```bash
# 构建操作
make build-engine          # 构建 engine 插件
make build-javadoc          # 构建 javadoc 插件
make quick-build            # 构建所有插件（并行）

# 清理操作
make clean-engine           # 清理 engine 插件
make quick-clean            # 清理所有插件（并行）

# 部署操作
make deploy-engine          # 部署 engine 插件
make deploy-javadoc        # 部署 javadoc 插件
make quick-deploy           # 部署子插件（并行）

# 版本管理
make version                # 显示所有插件版本

# 本地安装
make copy-zips              # 构建并复制 ZIP 到 Downloads
make install-plugins        # 安装插件到本地 IDEA
make copy-install-upload    # 复制、安装并上传到服务器
```

**插件目录**:

- `idea-plugin-kit` - 共享工具库
- `intelli-ai-engine` - AI 引擎插件
- `intelli-ai-javadoc` - Javadoc 生成插件
- `intelli-ai-changelog` - 更新日志插件
- `intelli-ai-nacos` - Nacos 集成插件
- `intelli-ai-tracer` - 追踪插件

## 常见部署工作流

### 完整发布工作流

```bash
# 1. 更新所有插件版本
./update_version.sh 2025.3.1

# 2. 生成 What's New 页面
./generate-whatsnew.sh

# 3. 部署 What's New 页面
./deploy.sh -w

# 4. 部署每个插件（可并行执行）
./deploy.sh engine -v 2025.3.1
./deploy.sh javadoc -v 2025.3.1
./deploy.sh changelog -v 2025.3.1
./deploy.sh nacos -v 2025.3.1
./deploy.sh tracer -v 2025.3.1

# 5. 部署 Nginx 配置（如需要）
./deploy.sh -n
```

### 仅文档更新

```bash
# 1. 生成文档清单
./generate-docs-list.sh intelli-ai-javadoc/site/docs

# 2. 仅部署站点目录
./deploy.sh javadoc -d
```

### 仅市场发布

```bash
# 1. 更新版本
./update_version.sh 2025.3.1

# 2. 仅发布到市场
./deploy.sh engine -p
```

### 快速批量部署

```bash
# 使用 Makefile 进行并行操作
make quick-build            # 并行构建所有插件
make quick-deploy           # 并行部署子插件
```

## 插件特定部署

### Engine 插件（核心依赖）

**插件 ID**: 29152

**部署**:

```bash
# 完整部署并更新版本
./deploy.sh engine -v 2025.3.1

# 仅市场发布
./deploy.sh engine -p

# 仅站点部署
./deploy.sh engine -d
```

**重要**: Engine 插件必须首先部署，因为其他插件依赖它。

### Javadoc 插件

**插件 ID**: 28835

**部署**:

```bash
./deploy.sh javadoc -v 2025.3.1
```

### Changelog 插件

**插件 ID**: 29154

**部署**:

```bash
./deploy.sh changelog -v 2025.3.1
```

### Nacos 插件

**插件 ID**: 29156

**部署**:

```bash
./deploy.sh nacos -v 2025.3.1
```

### Tracer 插件

**插件 ID**: 29155

**部署**:

```bash
./deploy.sh tracer -v 2025.3.1
```

## 远程服务器配置

### 服务器访问

- **SSH 别名**: `aliyun`
- **根目录**: `/var/www/zeka-idea-plugin`
- **数据目录**: `/var/www/data/intelli-ai-plugin`
- **Nginx 配置**: `/etc/nginx/conf.d`

### 目录结构

```
/var/www/zeka-idea-plugin/
├── intelli-ai-engine/
│   ├── site/              # 站点目录（如果存在）
│   │   ├── landing.html
│   │   ├── docs.html
│   │   └── docs/
│   └── intelli-ai-engine.zip
├── intelli-ai-javadoc/
│   └── ...
└── ...

/var/www/data/intelli-ai-plugin/
├── intelli-ai-engine.zip
├── intelli-ai-javadoc.zip
└── ...
```

### Nginx 配置

**全局配置**: `ideaplugin.dong4j.site.conf`

**部署**:

```bash
# 部署全局 Nginx 配置
./deploy.sh -n
```

**功能**:

1. 上传 Nginx 配置文件到 `/etc/nginx/conf.d/`
2. 测试 Nginx 配置 (`nginx -t`)
3. 重载 Nginx 服务 (`systemctl restart nginx`)

## 文件位置

### 项目根目录文件

- `deploy.sh` - 主部署脚本
- `update_version.sh` - 版本更新脚本
- `generate-whatsnew.sh` - What's New 生成器
- `generate-docs-list.sh` - 文档清单生成器
- `Makefile` - 构建快捷方式
- `DEPLOY.md` - 部署文档
- `ideaplugin.dong4j.site.conf` - Nginx 配置
- `download.dong4j.site.conf` - 下载服务器 Nginx 配置
- `latest.html` - 生成的 What's New 页面
- `buy-me-a-coffee.html` - 捐赠页面

### 插件特定文件

每个插件目录包含:

- `includes/pluginChanges.html` - 更新日志文件
- `site/` - 站点目录（可选）
    - `landing-v2.html` - 落地页
    - `docs.html` - 文档索引
    - `docs/` - 文档 Markdown 文件
- `build/distributions/` - 构建产物（ZIP 文件）
- `gradle.properties` - 插件配置（版本等）

## 最佳实践

### 版本管理

1. **始终先更新版本**:
   ```bash
   ./update_version.sh 2025.3.1
   ```

2. **验证版本更新**:
   ```bash
   make version
   ```

3. **使用语义化版本**: `x.y.z` 或 `x.y.z.w`

### 部署顺序

1. **Engine 插件优先** (必需依赖)
2. **核心插件** (javadoc, changelog)
3. **集成插件** (nacos, tracer)
4. **Nginx 配置** (如果更改)

### 文档更新

1. **部署站点前生成文档清单**:
   ```bash
   ./generate-docs-list.sh <plugin>/site/docs
   ```

2. **重大发布前更新 What's New**:
   ```bash
   ./generate-whatsnew.sh
   ./deploy.sh -w
   ```

### 错误处理

1. **部署前检查构建产物**:
   ```bash
   ls -la <plugin>/build/distributions/*.zip
   ```

2. **验证站点目录存在**:
   ```bash
   ls -la <plugin>/site/landing-v2.html
   ```

3. **重载前测试 Nginx 配置**:
   ```bash
   ssh aliyun "nginx -t"
   ```

## 故障排查

### 构建失败

**问题**: `gradlew buildPlugin` 失败

**解决方案**:

- 检查 Java 版本（需要 Java 17+）
- 验证 Gradle wrapper 可执行
- 检查缺失的依赖
- 查看构建日志中的具体错误

### 部署失败

**问题**: `rsync` 或 `ssh` 连接失败

**解决方案**:

- 验证 SSH 别名 `aliyun` 已配置
- 检查 SSH 密钥认证
- 验证远程目录权限
- 测试连接: `ssh aliyun "ls -la /var/www/zeka-idea-plugin"`

### 版本更新问题

**问题**: 版本未在所有文件中更新

**解决方案**:

- 验证插件在白名单中
- 检查 `gradle.properties` 存在
- 验证字段名称匹配 (`pluginVersion`, `kitVersion`, `engineVersion`)
- 使用详细输出再次运行更新脚本

### Nginx 配置问题

**问题**: Nginx 重载失败

**解决方案**:

- 测试配置: `ssh aliyun "nginx -t"`
- 检查配置文件中的语法错误
- 验证文件权限
- 查看 Nginx 错误日志: `ssh aliyun "tail -f /var/log/nginx/error.log"`

## 参考

- 部署文档: `DEPLOY.md`
- 插件开发: 参见 `idea-plugin-dev` skill
- 项目结构: 参见项目根目录
- 示例插件: `intelli-ai-javadoc/`, `intelli-ai-changelog/`

## 示例

### 部署新版本

```bash
# 1. 更新所有版本
./update_version.sh 2025.3.2

# 2. 生成 What's New
./generate-whatsnew.sh

# 3. 部署 What's New 页面
./deploy.sh -w

# 4. 部署所有插件
./deploy.sh engine -v 2025.3.2
./deploy.sh javadoc -v 2025.3.2
./deploy.sh changelog -v 2025.3.2
./deploy.sh nacos -v 2025.3.2
./deploy.sh tracer -v 2025.3.2

# 5. 部署 Nginx 配置
./deploy.sh -n
```

### 快速文档更新

```bash
# 更新 javadoc 插件的文档
./generate-docs-list.sh intelli-ai-javadoc/site/docs
./deploy.sh javadoc -d
```

### 仅市场发布

```bash
# 发布到市场而不部署站点
./update_version.sh 2025.3.2
./deploy.sh engine -p
./deploy.sh javadoc -p
```
