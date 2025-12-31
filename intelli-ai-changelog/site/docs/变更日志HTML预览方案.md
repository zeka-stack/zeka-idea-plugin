# IntelliAI Changelog 变更日志 HTML 预览方案

## 背景

Markdown 变更日志在 IDE 内查看不够直观，也难以直接分享。需要内置一份精美 HTML 模板，解析 `changelog.md` 并在浏览器中展示，形成发布页风格的“更新记录”。

## 目标

- 内置 HTML 模板，无需网络资源即可渲染
- 支持解析 `changelog.md` 并在浏览器打开
- 提供目录、搜索、折叠、版本切换等阅读体验

## 方案概述

插件内置 HTML 模板与渲染逻辑，运行时将 `changelog.md` 解析为 HTML，注入模板后生成临时文件，通过系统浏览器打开。

## 技术选型

- Markdown 解析：`flexmark` 或 IntelliJ 平台自带 Markdown 引擎
- HTML 预览：`Desktop.browse()` 打开系统浏览器
- 资源加载：本地内置 CSS / JS / 字体文件

## 渲染流程

1. 读取 `changelog.md`
2. 解析 Markdown → HTML
3. 生成目录（TOC）与锚点
4. 注入主题模板与样式
5. 写入临时 HTML 文件
6. 调用系统浏览器打开

## HTML 结构建议

```html
<main class="content">
  <aside class="toc"></aside>
  <article class="changelog"></article>
</main>
```

样式特点：

- 轻量渐变背景与柔和阴影
- 版本区块卡片化
- “新增 / 修复 / 优化”以颜色标签区分
- 顶部固定搜索栏 + 版本跳转

## 交互能力

- **搜索**：关键词过滤变更项
- **折叠**：按版本折叠详情
- **高亮**：定位最近版本
- **锚点分享**：支持复制某版本链接

## 文件与缓存策略

- 生成目录：`$IDEA_SYSTEM/cache/intelli-ai-changelog/`
- 文件命名：`changelog-preview-{hash}.html`
- 若 `changelog.md` 未变化则复用缓存文件

## 安全策略

- 禁止解析 Markdown 中的原始 HTML
- 过滤脚本与不安全链接
- 仅加载本地内置资源

## 兼容性

- Windows / macOS / Linux
- 不依赖外部 CDN 或网络字体
- 缺少默认字体时回退系统字体

## 失败与降级

- 无 `changelog.md`：提示先生成
- 无默认浏览器：提示手动打开生成文件
- 解析失败：展示原始 Markdown

## 体验增强（可选）

- 支持暗色/浅色主题切换
- 支持导出为 PDF
- 支持复制“版本摘要”到剪贴板

