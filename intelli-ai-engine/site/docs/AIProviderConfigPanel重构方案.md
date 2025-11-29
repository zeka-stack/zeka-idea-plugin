# AIProviderConfigPanel 重构方案

## 问题分析

当前 `AIProviderConfigPanel` 类有 1583 行代码，职责过多，包含：

- UI 组件创建和管理
- 业务逻辑处理（连接测试、模型刷新、配置管理）
- 数据模型管理
- 事件监听处理

## 重构目标

将 UI 和业务逻辑分离，提高代码可维护性和可测试性。

## 重构方案

### 1. 类结构设计

```
AIProviderConfigPanel (门面类，约 200 行)
├── AIProviderConfigUI (UI 组件管理，约 600 行)
│   ├── ConnectionPanelBuilder
│   ├── AvailableProvidersPanelBuilder
│   ├── BasicPanelBuilder
│   └── AdvancedPanelBuilder
├── AIProviderConfigController (业务逻辑控制器，约 500 行)
│   ├── ConnectionTestHandler
│   ├── ModelRefreshHandler
│   └── ProviderConfigManager
└── 内部类（渲染器、表格模型等，约 300 行）
```

### 2. 职责划分

#### AIProviderConfigPanel (门面类)

- 协调 UI 和业务逻辑
- 提供公共 API（loadSettings, getSettings, isModified）
- 管理 UI 和 Controller 的生命周期

#### AIProviderConfigUI (UI 组件管理)

- 创建和管理所有 UI 组件
- 提供 UI 组件的访问接口
- 处理 UI 相关的辅助方法（创建提示标签、状态图标等）

#### AIProviderConfigController (业务逻辑控制器)

- 处理配置加载和保存
- 处理连接测试逻辑
- 处理模型刷新逻辑
- 处理可用提供商管理
- 处理配置验证和状态管理

### 3. 详细设计

#### 3.1 AIProviderConfigUI

**职责**：

- 创建所有 UI 组件
- 提供 UI 组件的 getter 方法
- UI 辅助方法（createSpinnerWithHint, createCheckBoxWithHint 等）

**主要方法**：

```java
public class AIProviderConfigUI {
    // UI 组件
    private ComboBox<String> providerComboBox;
    private ComboBox<String> modelComboBox;
    private JBTextField baseUrlField;
    // ... 其他组件
    
    // 创建方法
    public JPanel createMainPanel();
    private JPanel createConnectionPanel();
    private JPanel createAvailableProvidersPanel();
    private JPanel createBasicPanel();
    private JPanel createAdvancedPanel();
    
    // 辅助方法
    private JPanel createSpinnerWithHint(JSpinner spinner, String hintKey);
    private JPanel createCheckBoxWithHint(JBCheckBox checkBox, String hintKey);
    private Icon createStatusDotIcon(Color color);
    
    // Getter 方法
    public ComboBox<String> getProviderComboBox();
    public ComboBox<String> getModelComboBox();
    // ... 其他 getter
}
```

#### 3.2 AIProviderConfigController

**职责**：

- 配置管理（加载、保存、验证）
- 连接测试
- 模型刷新
- 可用提供商管理
- 状态管理

**主要方法**：

```java
public class AIProviderConfigController {
    private final AICredentialManager credentialManager;
    private final AIResponseListener responseListener;
    private final AIProviderConfigUI ui;
    private AIProviderSettings workingSettings;
    
    // 配置管理
    public void loadSettings(AIProviderSettings settings);
    public AIProviderSettings getSettings();
    public boolean isModified(AIProviderSettings baseline);
    
    // 连接测试
    public void testConnection();
    
    // 模型刷新
    public void refreshModels();
    
    // 可用提供商管理
    public void addAvailableProvider(AIProviderConfig config, AIProviderType providerType);
    public void removeAvailableProvider(String credentialId);
    
    // 状态更新
    public void updateTestButtonState(boolean verified);
    public void updateRefreshButtonState(Boolean success);
    public void updateBasicConnectionInfo();
}
```

#### 3.3 AIProviderConfigPanel (重构后)

**职责**：

- 作为门面，协调 UI 和 Controller
- 提供公共 API
- 设置事件监听器

**主要方法**：

```java
public final class AIProviderConfigPanel {
    private final AIProviderConfigUI ui;
    private final AIProviderConfigController controller;
    
    public AIProviderConfigPanel(AICredentialManager credentialManager, 
                                 AIResponseListener responseListener) {
        this.ui = new AIProviderConfigUI();
        this.controller = new AIProviderConfigController(credentialManager, 
                                                          responseListener, 
                                                          ui);
        setupListeners();
    }
    
    public JPanel getPanel() {
        return ui.getMainPanel();
    }
    
    public void loadSettings(AIProviderSettings settings) {
        controller.loadSettings(settings);
    }
    
    public AIProviderSettings getSettings() {
        return controller.getSettings();
    }
    
    public boolean isModified(AIProviderSettings baseline) {
        return controller.isModified(baseline);
    }
    
    private void setupListeners() {
        // 设置 UI 事件监听器，委托给 Controller
    }
}
```

### 4. 内部类处理

- `ProviderListCellRenderer` - 移到 `AIProviderConfigUI`
- `ProviderTableCellRenderer` - 移到 `AIProviderConfigUI`
- `AvailableProvidersTableModel` - 移到 `AIProviderConfigUI` 或独立类

### 5. 重构步骤

1. **创建 AIProviderConfigUI 类**
    - 移动所有 UI 组件字段
    - 移动所有 UI 创建方法
    - 移动 UI 辅助方法

2. **创建 AIProviderConfigController 类**
    - 移动业务逻辑方法
    - 通过 UI 接口访问组件
    - 处理所有业务逻辑

3. **重构 AIProviderConfigPanel**
    - 简化为门面类
    - 协调 UI 和 Controller
    - 设置事件监听器

4. **测试验证**
    - 确保功能正常
    - 确保 API 兼容性

### 6. 优势

- **职责清晰**：UI 和业务逻辑分离
- **易于测试**：可以单独测试业务逻辑
- **易于维护**：代码结构清晰，修改影响范围小
- **可扩展性**：可以轻松添加新的 UI 组件或业务逻辑

### 7. 注意事项

- 保持公共 API 不变，确保向后兼容
- UI 和 Controller 之间通过接口通信，避免直接依赖
- 保持事件监听器的正确设置
- 确保线程安全（UI 更新在 EDT 中执行）

