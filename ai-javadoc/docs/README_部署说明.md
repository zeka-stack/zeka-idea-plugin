# Docsify 文档部署说明

## 📦 本地开发

### 安装依赖

```bash
npm install
```

### 启动本地服务器

```bash
npm run dev
# 或
npm start
```

访问 http://localhost:3000 查看文档。

## 🌐 部署到 GitHub Pages

### 方法一：使用 GitHub Actions（推荐）

1. 在项目根目录创建 `.github/workflows/docs.yml`：

```yaml
name: Deploy Docs

on:
  push:
    branches:
      - main
    paths:
      - 'ai-javadoc/docs/**'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Deploy to GitHub Pages
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./ai-javadoc/docs
```

2. 在 GitHub 仓库设置中启用 GitHub Pages，选择 `gh-pages` 分支。

### 方法二：手动部署

1. 安装 docsify-cli：

```bash
npm install -g docsify-cli
```

2. 初始化并预览：

```bash
cd ai-javadoc/docs
docsify serve .
```

3. 构建并推送到 gh-pages 分支：

```bash
# 克隆仓库
git clone <your-repo>
cd <repo-name>

# 创建 gh-pages 分支
git checkout --orphan gh-pages
git rm -rf .

# 复制 docs 目录内容
cp -r ai-javadoc/docs/* .

# 提交
git add .
git commit -m "Deploy docs"
git push origin gh-pages
```

## 🔧 配置说明

### 修改文档标题

编辑 `index.html` 中的 `window.$docsify.name`。

### 修改侧边栏

编辑 `_sidebar.md` 文件。

### 添加搜索功能

搜索功能已在 `index.html` 中启用，使用 docsify 内置搜索插件。

### 自定义主题

可以在 `index.html` 中修改 CSS 链接：

```html
<!-- 使用其他主题 -->
<link rel="stylesheet" href="//cdn.jsdelivr.net/npm/docsify/lib/themes/buble.css">
```

## 📝 注意事项

1. 确保所有 Markdown 文件使用 UTF-8 编码
2. 图片路径使用相对路径，如 `./imgs/image.png`
3. 如果部署到 GitHub Pages，需要 `.nojekyll` 文件
4. 侧边栏文件名必须是 `_sidebar.md`

## 🐛 常见问题

### 图片不显示

检查图片路径是否正确，建议使用相对路径。

### 侧边栏不显示

确保 `_sidebar.md` 文件存在且 `loadSidebar: true` 已配置。

### 搜索不工作

确保已引入搜索插件脚本。

