# 部署说明文档

本文档说明 IntelliAI 插件套件的部署流程和相关工具的使用方法。

## 目录

- [快速操作](#快速操作)
- [详细说明](#详细说明)（待完善）

---

## 快速操作

### 版本更新

更新所有白名单插件的版本号：

```bash
# 更新所有插件版本号为指定版本
./update_version.sh 2025.3.1
```

### 插件部署

#### 单个插件部署

```bash
# 部署 engine 插件（完整流程：更新版本 → 发布 → 上传 ZIP → 部署 site → 部署 Nginx）
./deploy.sh engine -v 2025.3.1

# 只指定插件名，按顺序执行 -p, -z, -d 三个操作
./deploy.sh engine

# 仅上传 ZIP 文件
./deploy.sh engine -z

# 仅部署 site 目录
./deploy.sh engine -d

# 仅发布到插件市场
./deploy.sh engine -p

# 部署 Nginx 配置（可与其他操作组合）
./deploy.sh engine -n
```

#### 支持的插件名称

- `engine` - intelli-ai-engine
- `javadoc` - intelli-ai-javadoc
- `changelog` - intelli-ai-changelog
- `nacos` - intelli-ai-nacos
- `tracer` - intelli-ai-tracer
- `swagger` - intelli-ai-swagger
- `archiver` - archiver-man
- `helper` - zks-dev-helper

#### 全局操作

```bash
# 部署全局 Nginx 配置（无需指定插件）
./deploy.sh -n

# 部署 latest.html（What's New 聚合页面）
./deploy.sh -w
```

### 文档生成

```bash
# 生成文档清单（docs-list.json）
./generate-docs-list.sh <docs-dir>
# 例如：
./generate-docs-list.sh intelli-ai-engine/site/docs

# 生成 What's New 聚合页面（latest.html）
./generate-whatsnew.sh
```

### Makefile 快捷操作

```bash
# 快速构建所有插件
make quick-build

# 快速清理所有插件
make quick-clean

# 快速部署子插件（并发执行）
make quick-deploy

# 查看所有插件版本
make version

# 构建并拷贝 ZIP 到指定目录
make copy-zips TARGET_DIR=/path/to/dir

# 安装插件到本地 IDEA 插件目录
make install-plugins
```

### 常用部署流程

#### 完整发布流程

```bash
# 1. 更新所有插件版本号
./update_version.sh 2025.3.1

# 2. 生成 What's New 页面
./generate-whatsnew.sh

# 3. 部署 What's New 页面
./deploy.sh -w

# 4. 部署各个插件（可并发执行）
./deploy.sh engine -v 2025.3.1
./deploy.sh javadoc -v 2025.3.1
./deploy.sh changelog -v 2025.3.1
# ... 其他插件

# 5. 部署 Nginx 配置（如果需要）
./deploy.sh -n
```

#### 仅更新文档

```bash
# 生成文档清单
./generate-docs-list.sh intelli-ai-javadoc/site/docs

# 仅部署 site 目录
./deploy.sh javadoc -d
```

#### 仅发布到市场

```bash
# 更新版本号
./update_version.sh 2025.3.1

# 仅发布到插件市场
./deploy.sh engine -p
```

---

## 详细说明

> 待完善：后续将添加各工具的详细使用说明和配置说明。

### 相关文件

- `deploy.sh` - 统一插件发布与部署脚本
- `update_version.sh` - 版本更新脚本
- `generate-docs-list.sh` - 文档清单生成脚本
- `generate-whatsnew.sh` - What's New 聚合页面生成脚本
- `ideaplugin.dong4j.site.conf` - Nginx 配置文件
- `buy-me-a-coffee.html` - 捐赠页面
- `latest.html` - What's New 聚合页面
- `Makefile` - 构建和部署快捷操作

