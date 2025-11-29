# Archive Man 功能规划清单

本文作为 Archive Man 插件的能力路线图，结合“压缩包可编辑”目标与参考插件调研结果，分阶段明确功能范围、优先级、交付物与依赖，为后续排期与实施提供依据。

---

## 1. 总体目标

1. 在 IntelliJ Project View 中像文件夹一样浏览各类归档（Zip、Jar、Tar、Gz、7z 等），并支持嵌套归档。
2. 在 IDE 内直接编辑归档内的文本/代码文件，保存时安全写回并支持备份和冲突检测。
3. 提供常用的归档增强能力（快速导出、Jar 反编译、批量操作等）。
4. 构建模块化的归档处理基础设施，便于扩展新的格式与高级功能。

---

## 2. 分阶段功能规划

### 阶段 P0：基础只读浏览（v0.3.x）

| 功能              | 描述                                                | 依赖/备注                                 |
|-----------------|---------------------------------------------------|---------------------------------------|
| Project View 浏览 | 在 Project View 中展开 Zip/Jar，显示为目录结构                | 复用 IntelliJ `ArchiveFileType`；启用/禁用开关 |
| 嵌套归档读取          | 识别 `!/` 路径，复制到临时目录再挂载                             | `NestedArchiveCacheService`           |
| Zip/Jar Handler | 抽象 `ArchiveFormatProvider`，实现 Zip/Jar 的只读 Handler | 借鉴 `file-expander-plugin`             |
| 错误降级机制          | 解析失败时回落至默认节点并记录日志                                 | 避免 Project View 崩溃                    |

### 阶段 P1：多格式拓展（v0.4.x）

| 功能            | 描述                                      | 依赖/备注                          |
|---------------|-----------------------------------------|--------------------------------|
| Tar/Gz/Tgz 支持 | 基于 Apache Commons Compress 实现 Handler   | `ArchiveHandlerBase` 风格        |
| 7z/Zstd 支持    | 集成 SevenZip-JBinding + Zstd 库，覆盖常见镜像类格式 | 需处理 Apple Silicon native 库     |
| 临时目录管理        | 缓存大小上限、IDE 退出清理、手动清空入口                  | `NestedArchiveCacheService` 强化 |
| Handler 测试    | 补齐单元测试（Zip/Tar/Gz/7z，含嵌套）               | 借鉴 `file-expander-plugin` 测试   |

### 阶段 P2：可编辑能力（v0.5.x）

| 功能        | 描述                                                                   | 依赖/备注                    |
|-----------|----------------------------------------------------------------------|--------------------------|
| 可编辑文件打开流程 | 以 `EditableArchiveVirtualFile` 形式打开文本条目，绑定 `LightVirtualFile` & 文档监听 | 参考现有 ZIP 编辑方案            |
| 保存写回管线    | 文档保存时写入临时文件 → Zip/Tar 重写条目 → 更新 VFS                                  | `EditableArchiveService` |
| 冲突检测与备份   | 校验时间戳/CRC，保存前生成 `.bak`，支持自动恢复                                        | 配置项可控制                   |
| 批量写入 & 队列 | 支持批量修改后集中写入，后台任务展示进度                                                 | `ZipWriteTask`           |
| 设置页面      | 新增“Archive Editor” 选项卡，管理备份、大小限制、性能模式                                | 遵循配置持久化规范                |

### 阶段 P3：增强功能（v0.6.x+）

| 功能        | 描述                                     | 依赖/备注                   |
|-----------|----------------------------------------|-------------------------|
| Jar 反编译动作 | 迁移 `DecompileJarAction`，支持一键导出 `.java` | 基于 Project View 右键菜单    |
| 快速导出/解压   | 在节点右键中提供“导出条目”“复制到临时目录”动作              | 复用 `ArchiveVfsCore` API |
| 历史记录集成    | 与 Local History/VCS diff 联动，支持归档条目比对   | 需要自定义 `VirtualFile` 标识  |
| 性能监控与日志   | 收集大文件读取/写入耗时，提供诊断日志入口                  | 便于定位 JNI/IO 问题          |
| IDE 启动同步  | `StartupActivity` 检查残留临时文件、版本迁移        | 遵循线程安全规则                |

---

## 3. 功能分层与依赖关系

1. **基础层（必须优先完成）**
    - `ArchiveFormatProvider` 接口及 Zip/Jar 实现
    - `NestedArchiveCacheService`
    - `ArchiveManTreeStructureProvider`
2. **扩展层**
    - 额外格式 Handler（Tar/Gz/7z/Zstd）
    - 临时目录管理 UI + CLI
3. **编辑层**
    - `EditableArchiveService`、`EditableArchiveVirtualFile`
    - 文档监听与写回任务
4. **增值层**
    - Jar 反编译、导出动作、历史记录集成等

每一层向上依赖下层能力，推荐在完成同一层的关键路径后再进入下一层，避免重复返工。

---

## 4. 配置与用户体验

- **设置项**（Tools → Archive Man）：
    - `enableArchiveBrowser`（开关 Project View 展开）
    - `enableEditableMode`（启用可编辑能力，默认关闭）
    - `maxEditableFileSizeMB`
    - `autoBackup`、`backupRetention`
    - `cacheDirectory`、`cacheLimitMB`
- **状态反馈**：
    - Project View 节点图标区分“只读”与“可编辑”
    - 编辑器状态栏展示“Archive Mode”“只读/可写”
    - 写回时通过 Notification + Task.Backgroundable 展示进度

---

## 5. 测试与验证

| 测试类型 | 覆盖内容                                                        |
|------|-------------------------------------------------------------|
| 单元测试 | 各格式 Handler 的 `createEntriesMap`、`contentsToByteArray`、嵌套处理 |
| 集成测试 | Project View 展开/收起、嵌套缓存命中、设置开关生效                            |
| 功能测试 | 文档编辑 → 保存 → 写回结果校验，备份/恢复、冲突提示                               |
| 性能测试 | 大文件/多文件同时打开与写入，缓存清理效率                                       |

---

## 6. 后续行动

1. 根据阶段拆分任务到 issue/todo。
2. 为 P0 制定详细开发顺序：VFS 抽象 → 缓存服务 → Project View → Zip/Jar Handler → UI 开关。
3. 每次交付更新 `includes/pluginChanges.html` 与 `docs/用户手册.md`，同步说明新能力与使用方式。

以上规划可根据用户反馈与资源情况滚动调整，但建议保持“先稳定浏览体验，再叠加编辑与增值能力”的节奏。

