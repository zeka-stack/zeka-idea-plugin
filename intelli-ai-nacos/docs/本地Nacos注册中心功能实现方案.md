# 本地 Nacos 注册中心功能实现方案

## 一、功能概述

在现有 IntelliAI Nacos 插件中补充“本地 Nacos 注册中心”能力，为用户提供一键下载、启动、停止内置 Nacos 的体验，并与远程 Nacos 配置互斥。该功能主要满足以下场景：

- 没有可用远程环境时，可直接在 IDE 内拉起本地 Nacos
- 需要离线或内网环境下快速调试配置
- 结合现有工具窗口能力，统一管理本地与远程配置

## 二、实现方案

1. **配置入口扩展**
    - 在 `NacosSettingsPanel` 顶部新增“本地 Nacos 注册中心”分组
    - 提供“使用本地注册中心”复选框，以及“启动 / 停止”按钮
    - 勾选后禁用远程地址、用户名、密码与测试连接按钮，确保互斥

2. **服务层封装**
    - 新建 `LocalNacosService`，封装下载、启动、停止逻辑
    - 复用 `LocalRegistryManager`、`LocalRegistryContext`，并强制选择 `LocalRegistry.NACOS`
    - 使用 `AppExecutorUtil` 统一调度后台任务，避免阻塞 EDT
    - 采用 `NotificationUtil` 输出启动成功、已运行、停止成功、失败等提示
    - 状态监测通过异步轮询 `LocalRegistryManager.localRegistryStarted`，并在 UI 中展示

3. **线程与状态管理**
    - Settings 面板在执行 start/stop 时进入 busy 状态，禁用相关控件
    - 操作完成后恢复 UI，确保按钮状态与复选框一致
    - 若操作被取消或失败，通过通知提示原因
    - 取消“使用本地注册中心”勾选时自动触发停服，避免残留进程
    - 允许在启用本地注册中心时继续配置下方自建地址/凭据，可选填用户名和密码
    - 设置页提供状态标签与超链接：未运行时显示红色提示，运行中时显示绿色提示 + 一键打开浏览器

4. **端口校验修复**
    - `LocalRegistryManager.startRegistryFromPreferencePage` 依据注册中心类型选择检测端口集合，避免强制校验轻量级专用端口

## 三、技术细节

| 模块    | 说明                                                                 |
|-------|--------------------------------------------------------------------|
| UI    | `FormBuilder + TitledSeparator` 构建新的设置分组，使用 `FlowLayout` 放置启停按钮    |
| 服务    | `LocalNacosService` 使用 `CompletableFuture.runAsync` 执行耗时操作，统一捕获异常  |
| 下载/启动 | 复用阿里原插件中的 `LocalRegistryManager` 全部逻辑，自动下载 ZIP、校验端口、设置 `JAVA_HOME` |
| 通知    | 所有用户可见文字均放入 `messages*.properties`，通过 `NotificationUtil` 展示        |
| 国际化   | 新增设置文案与通知文案的中英文翻译                                                  |

## 四、涉及文件

- `dev/dong4j/zeka/stack/idea/plugin/nacos/local/LocalNacosService.java`
- `dev/dong4j/zeka/stack/idea/plugin/nacos/settings/ui/NacosSettingsPanel.java`
- `com/alibabacloud/intellij/service/edas/registry/local/LocalRegistryManager.java`
- `src/main/resources/messages.properties`
- `src/main/resources/messages_zh_CN.properties`
- 文档：`docs/用户手册.md`、`includes/pluginChanges.html`

## 五、测试计划

1. **UI 行为**
    - 勾选 / 取消“使用本地注册中心”时，远程字段与按钮互斥
    - 操作进行中，按钮与复选框正确禁用并展示 “Starting…” / “Stopping…”

2. **启动流程**
    - 首次点击“启动”自动下载 ZIP，完成后收到“启动成功”通知
    - 二次启动提示“已在运行”
    - 取消下载 / 启动时收到“已取消”通知

3. **停止流程**
    - 在本地 Nacos 运行时点击“停止”，收到“已停止”通知
    - 未运行时点击“停止”，提示“未运行”且无异常

4. **端口占用**
    - 启动前占用 8848 端口，校验应提示端口被占用（沿用原逻辑）

5. **国际化**
    - 中英文 IDE 下均显示正确的按钮文案、通知文本

## 六、后续扩展

- 提供本地 Nacos 的运行状态探测，实时反馈到设置面板
- 支持配置默认下载目录与缓存清理
- 在工具窗口中直接暴露“本地 / 远程”切换入口

