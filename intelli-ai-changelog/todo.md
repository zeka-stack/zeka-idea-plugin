# IntelliAI Changelog 插件开发计划

## 2025.11.27

1. 修改化生成的记录的表情
2. 添加提交记录引用(需要修改代码和提示词并做成可选配置)
3. 需要修改 engine(将高级配置参数绑定到可用模型上, 现在高级参数配置是全局设置)

## 2025.12.31

- [ ] 生成项目变更文件
- [ ] 生成美化项目的 html 变更记录

- [x] 插件内置“按需下载”（首次使用时按平台下载并缓存）
- [x] 提供“离线包”模式（允许用户手动放入指定目录）

## 2026.01.04

- [x] commit message 生成时, 允许读取消息框中的文本作为上下文以提高准确率(使用用户输入的自然语言 + code diff + 提示词来重写commit message)

## 2026.01.09

- [ ] 提交输入作为上下文有问题, 不会生成 scope
- [ ] scope 不是每次都会生成, 这个需要检查一下
- [ ] 日报 周报项目级隔离

## 2026.01.14

- [x] 优化删除大量文件的 diff
- [x] 优化单个文件大量改动的 diff
- [x] 优化大量文件改动的 diff

- [ ] 替换 git-cliff 中的 github 链接地址

```
commit_preprocessors = [
    # 将问题编号替换为链接模板，在 `changelog.postprocessors` 中更新
    #{ pattern = '\((\w+\s)?#([0-9]+)\)', replace = "([#${2}](<REPO>/issues/${2}))"},
    # 使用 https://github.com/crate-ci/typos 检查提交消息的拼写
    # 如果拼写不正确，将自动修复
    #{ pattern = '.*', replace_command = 'typos --write-changes -' },
    { pattern = '\((\w+\s)?#([0-9]+)\)', replace = "([#${2}](https://github.com/zeka-stack/zeka-idea-plugin/issues/${2}))" },
]
```

## 2026.05.24

- [ ] 增加一个开关: 是否在生成 commit message 时附带上协助者, 使用: `Co-authored-by: ZekaStack <zeka.stack@gmail.com>`

