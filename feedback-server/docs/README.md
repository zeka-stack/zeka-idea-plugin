# Feedback Server

为 IntelliJ IDEA 插件提供反馈接口的后端服务，内部对接 GitHub Discussions API。

## 功能概述

- 提供 REST API 接口接收插件反馈
- 自动将反馈提交到 GitHub Discussions
- 支持多种反馈类型（Bug 报告、功能建议、使用问题等）

## GitHub Discussions 配置

### 仓库信息

- **仓库**: [zeka-stack/zeka-idea-plugin](https://github.com/zeka-stack/zeka-idea-plugin)
- **Discussions 地址**: https://github.com/zeka-stack/zeka-idea-plugin/discussions

### 讨论类别

仓库已启用以下讨论类别：

- 📣 **Announcements** - 公告
- 💬 **General** - 一般讨论（适合一般反馈）
- 💡 **Ideas** - 想法建议（适合功能建议）
- 🗳️ **Polls** - 投票
- 🙏 **Q&A** - 问答（适合使用问题）
- 🙌 **Show and tell** - 展示

## 快速开始

### 1. 获取仓库信息

使用提供的脚本获取仓库 ID 和类别 ID：

```bash
# Linux/macOS
cd feedback-server/scripts
./get-repo-info.sh YOUR_GITHUB_TOKEN

# Windows
cd feedback-server\scripts
get-repo-info.bat YOUR_GITHUB_TOKEN
```

脚本会返回：

- `repository.id` - 仓库的全局 ID（用于 `repositoryId`）
- `discussionCategories.nodes[].id` - 各个类别的 ID（用于 `categoryId`）

### 2. 配置环境变量

创建 `.env` 文件或设置环境变量：

```bash
GITHUB_TOKEN=your_github_token
GITHUB_REPOSITORY_ID=repository_id_from_step_1
GITHUB_CATEGORY_ID_GENERAL=category_id_for_general
GITHUB_CATEGORY_ID_IDEAS=category_id_for_ideas
GITHUB_CATEGORY_ID_QA=category_id_for_qa
```

### 3. 启动服务

```bash
./gradlew bootRun
```

## API 文档

详见 [API 文档](./API.md)（待创建）

## 技术栈

- Spring Boot
- Spring Web
- OkHttp / Apache HttpClient（用于调用 GitHub GraphQL API）
- Jackson（JSON 处理）

## 参考文档

- [GitHub Discussions API 参数整理](./GitHub%20Discussions%20API%20参数整理.md)
- [用户身份识别方案](./用户身份识别方案.md) - 如何识别和记录反馈提交人信息

