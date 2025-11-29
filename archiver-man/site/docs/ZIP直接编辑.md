# ZIP 压缩包内文件直接编辑功能实现方案

## 1. 功能概述

在 IntelliJ IDEA 中直接打开并编辑 ZIP 压缩包内的文本或代码文件，避免手动解压与重新打包。用户可以：

- 从 Project View / Recent Files 中直接打开 `.zip`、`.jar` 等压缩文件
- 像普通文件一样修改、保存、撤销、对比版本
- 保存时自动写回压缩包，并支持备份、增量写入与冲突检测

## 2. 竞品调研

- File Expander（仅支持将压缩包内容展开为虚拟树进行浏览，不支持写操作）
- Archive Browser（聚焦多种归档格式的快速浏览和提取，同样缺少编辑能力）

两者均缺乏编辑流程、同步机制与冲突处理，因此需要自建写入管线。

## 3. 目标使用场景

1. 查看/调整第三方 SDK 附带的资源或示例文件
2. 快速修复压缩包配置（如隧道脚本、Lambda 部署包）
3. 在插件或脚手架生成的离线包内直接修改脚本/配置后重新发布

## 4. 关键功能需求

| 模块    | 需求                                |
|-------|-----------------------------------|
| 打开体验  | 双击压缩包即可在 Project View 内展开树，支持模糊搜索 |
| 编辑体验  | 支持 IDE 现有编辑能力（语法高亮、格式化、Diff）      |
| 保存策略  | 保存单文件回写；支持自动备份 `.zip.bak`；冲突检测    |
| 多文件更改 | 批量更改后一次性写入，减少压缩 IO 次数             |
| 历史记录  | 与 Local History、VCS diff 集成       |
| 安全性   | 只读模式、提示大包写入耗时、沙箱/临时目录隔离           |

## 5. 技术方案

### 5.1 虚拟文件系统扩展

- 复用 `ArchiveFileSystem` 做只读视图
- 自定义 `EditableArchiveVirtualFile`，在 `getOutputStream()` 时挂接写入逻辑
- 引入 `EditableArchiveFileSystem`，维护：
    - 打开的压缩包句柄与 `ZipFileSystem` 映射
    - 条目缓存（内存或临时文件）
    - 修改标记、自动刷新

### 5.2 缓存与编辑流程

1. 用户打开条目 → 若为文本，复制到 `LightVirtualFile` 并绑定文档监听
2. 编辑器文档更改 → 标记为 `dirty`，记录对应条目及修改时间
3. 保存流程：
    - 触发 `FileDocumentManagerListener.beforeDocumentSaving`
    - 将文档内容写入临时文件
    - 使用 `ZipOutputStream` 重写目标条目（可采用 Apache Commons Compress）
    - 更新压缩包 CRC、size，同步刷新 VFS
4. 若启用批处理：集中在 `bulkSave()` 中处理所有 `dirty` 条目，减少多次解压/重压

### 5.3 冲突与备份

- 保存前计算原条目的 `timestamp` 与 `CRC`
- 若压缩包在磁盘被外部修改，提示用户进行三方合并
- 保存前创建 `.zip.bak`
- 配置项允许关闭备份或指定备份数量

### 5.4 性能与资源管理

- 对大文件采用流式写入，避免一次性加载
- 引入 `SoftReference` 缓存文本内容
- 通过 `Alarm` 延迟写入，合并频繁保存
- 提供设置项控制最大可编辑文件大小、后台线程数量

### 5.5 UI 与交互

- 在 Project View 中对可编辑压缩包添加专有图标/标记
- 在编辑器状态栏显示“ZIP 模式”“只读”等提示
- 在保存时弹出通知/进度条（基于 `Task.Backgroundable`）
- 设置页面（`Tools → ZIP Editor`）：
    - 是否默认备份
    - 自动刷新间隔
    - 最大可编辑文件大小

## 6. 类与模块拆分

- `EditableArchiveService`：负责压缩包句柄管理、数据同步
- `EditableArchiveFileSystem`：自定义 VFS
- `EditableArchiveVirtualFile`：条目的虚拟文件实现
- `EditableArchiveDocumentListener`：监听文档保存事件
- `ZipWriteTask`：后台写入任务
- `ZipEditorSettings` + 面板：配置持久化

## 7. 测试计划

| 用例      | 内容                       |
|---------|--------------------------|
| 单文件编辑   | 打开、修改、保存，校验 CRC 与内容      |
| 多文件批量写入 | 同时修改多个文件，确保最终包内容一致       |
| 外部修改冲突  | IDE 打开期间，外部修改同一条目，保存时应提示 |
| 大文件处理   | 50MB+ 文件的编辑与写入性能         |
| 只读模式    | 用户选择只读时应拒绝写入并提示          |
| 备份恢复    | 保存失败后能恢复 `.bak` 内容       |

## 8. 风险与缓解

- **性能瓶颈**：采用批处理、异步写入与增量重写策略
- **损坏风险**：写前备份 + 写后校验 + 异常回滚
- **多线程安全**：所有写入集中在单线程执行，使用 `ReadWriteLock` 控制
- **格式兼容**：初期仅支持 ZIP，后续可扩展 tar.gz/7z

## 9. 里程碑

1. POC：实现单条目读写链路，验证 VFS 扩展可行性
2. Beta：补齐 UI、批量保存、备份/冲突处理
3. Release：完善设置、国际化、用户手册、插件市场发布

## 10. POC 进展（Archiver Man 0.2.0）

- `EditableArchiveService`：负责解析条目、创建 `LightVirtualFile`，以及通过 JDK ZipFS 写回内容
- `EditableArchiveVirtualFile`：承载临时副本并挂接压缩包元数据
- `EditableArchiveDocumentListener`：监听保存动作，调度写入任务
- `ArchiveContextAction`：只在 ZIP/JAR 条目上显示，负责打开可编辑副本
- 写回流程：自动创建 `.bak` 备份 → ZipFS 覆盖条目 → 刷新 VFS → 状态栏提示
- 冲突检测：比较压缩包 `timestamp`，若外部已修改则拒绝写入并提示用户重新打开

---

以上方案完成后，可等待用户确认再开始编码实现。
