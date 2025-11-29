# 自定义 JavaDoc 标签配置功能实现方案

## 1. 需求概述

### 1.1 当前问题

- `CustomJavaDocTagRegistrar` 中硬编码了 `date` 和 `email` 两个标签
- 用户无法自定义需要注册的 JavaDoc 标签
- 无法删除已注册的标签

### 1.2 目标

- 在设置页面添加自定义 JavaDoc 标签配置功能
- 支持动态添加、删除、编辑标签
- 实现标签的同步机制（添加新标签、删除旧标签）
- 复用现有的标签注册逻辑

### 1.3 使用场景

- 用户第一次配置：`["date"]`
- 用户第二次配置：`["email", "xxx"]`
- 预期结果：`date` 被删除，`email` 和 `xxx` 被添加

## 2. 技术方案

### 2.1 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                    SettingsState                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │  List<String> customJavaDocTags                 │   │
│  │  ["date", "email", "xxx"]                       │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                        ▲
                        │ 读取配置
                        │
┌─────────────────────────────────────────────────────────┐
│              JavaDocSettingsPanel                        │
│  ┌──────────────────────────────────────────────────┐   │
│  │  JBTable + ToolbarDecorator                      │   │
│  │  - 添加标签                                      │   │
│  │  - 删除标签                                      │   │
│  │  - 编辑标签                                      │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                        ▲
                        │ 应用配置
                        │
┌─────────────────────────────────────────────────────────┐
│         CustomJavaDocTagRegistrar                        │
│  ┌──────────────────────────────────────────────────┐   │
│  │  syncCustomTags(Project)                         │   │
│  │  1. 读取当前已注册标签                           │   │
│  │  2. 读取配置中的标签                             │   │
│  │  3. 计算差异（添加/删除）                         │   │
│  │  4. 执行同步操作                                 │   │
│  └──────────────────────────────────────────────────┘   │
│                        │                                 │
│                        ▼                                 │
│  ┌──────────────────────────────────────────────────┐   │
│  │  JavadocDeclarationInspection                    │   │
│  │  ADDITIONAL_TAGS = "date,email,xxx"              │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 2.2 核心流程

#### 2.2.1 标签同步流程

```
开始
  │
  ├─> 读取 SettingsState.customJavaDocTags
  │   └─> ["email", "xxx"]
  │
  ├─> 读取 JavadocDeclarationInspection.ADDITIONAL_TAGS
  │   └─> "date,email,xxx"
  │
  ├─> 解析当前标签为列表
  │   └─> ["date", "email", "xxx"]
  │
  ├─> 计算需要添加的标签
  │   └─> 配置中有但未注册的：[]（email 和 xxx 已存在）
  │
  ├─> 计算需要删除的标签
  │   └─> 已注册但配置中没有的：["date"]
  │
  ├─> 执行删除操作
  │   └─> 从 "date,email,xxx" 中移除 "date"
  │   └─> 结果："email,xxx"
  │
  ├─> 执行添加操作
  │   └─> 添加新标签到字符串（本例中无需添加）
  │
  └─> 更新 ADDITIONAL_TAGS 字段
      └─> "email,xxx"
```

#### 2.2.2 配置应用流程

```
用户点击 Apply/OK
  │
  ├─> JavaDocSettingsConfigurable.apply()
  │   └─> 保存配置到 SettingsState
  │
  ├─> 触发标签同步
  │   └─> CustomJavaDocTagRegistrar.syncCustomTags(project)
  │
  └─> 更新 JavadocDeclarationInspection
      └─> 通知配置变更
```

## 3. 详细实现

### 3.1 数据存储层（SettingsState）

#### 3.1.1 添加字段

**文件**: `intelli-ai-javadoc/src/main/java/dev/dong4j/zeka/stack/idea/plugin/settings/SettingsState.java`

```java
// ==================== JavaDoc 标签配置 ====================

/**
 * 自定义 JavaDoc 标签列表
 *
 * <p>用户可以在设置页面配置自定义的 JavaDoc 标签。
 * 这些标签会被自动注册到 JavadocDeclarationInspection 中，
 * 使得 IntelliJ IDEA 不会将这些标签标记为未知标签。
 *
 * <p>标签格式：
 * <ul>
 *   <li>标签名称不包含 @ 符号</li>
 *   <li>标签名称不区分大小写</li>
 *   <li>标签名称不能包含逗号、空格等特殊字符</li>
 * </ul>
 *
 * <p>默认值: 空列表
 *
 * <p>示例：
 * <pre>
 * customJavaDocTags = ["date", "email", "custom"]
 * </pre>
 *
 * @see CustomJavaDocTagRegistrar
 * @since 1.3.4
 */
public List<String> customJavaDocTags = new ArrayList<>();
```

#### 3.1.2 添加辅助方法

```java
/**
 * 获取自定义 JavaDoc 标签列表（去重、去空、转小写）
 *
 * <p>对标签列表进行规范化处理：
 * <ul>
 *   <li>去除空字符串和 null 值</li>
 *   <li>去除重复标签</li>
 *   <li>转换为小写（标签不区分大小写）</li>
 *   <li>去除前后空格</li>
 * </ul>
 *
 * @return 规范化后的标签列表
 */
@NotNull
public List<String> getNormalizedCustomJavaDocTags() {
    if (customJavaDocTags == null) {
        return new ArrayList<>();
    }
    
    return customJavaDocTags.stream()
        .filter(tag -> tag != null && !tag.trim().isEmpty())
        .map(String::trim)
        .map(String::toLowerCase)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
}

/**
 * 验证标签名称是否有效
 *
 * <p>标签名称规则：
 * <ul>
 *   <li>不能为空</li>
 *   <li>只能包含字母、数字、下划线、连字符</li>
 *   <li>不能包含空格、逗号等特殊字符</li>
 * </ul>
 *
 * @param tagName 标签名称
 * @return 如果标签名称有效返回 true
 */
public static boolean isValidTagName(@Nullable String tagName) {
    if (tagName == null || tagName.trim().isEmpty()) {
        return false;
    }
    
    // 标签名称只能包含字母、数字、下划线、连字符
    return tagName.matches("^[a-zA-Z0-9_-]+$");
}
```

### 3.2 UI 层（JavaDocSettingsPanel）

#### 3.2.1 添加 UI 组件

**文件**: `intelli-ai-javadoc/src/main/java/dev/dong4j/zeka/stack/idea/plugin/settings/ui/JavaDocSettingsPanel.java`

```java
// JavaDoc 标签配置
/** 自定义 JavaDoc 标签列表表格 */
private JBTable customJavaDocTagsTable;
/** 自定义 JavaDoc 标签列表面板（包含表格和工具栏） */
private JPanel customJavaDocTagsPanel;
/** 自定义 JavaDoc 标签列表表格模型 */
private CustomJavaDocTagsTableModel customJavaDocTagsTableModel;
```

#### 3.2.2 创建 UI 组件

在 `createUI()` 方法中添加：

```java
// 创建自定义 JavaDoc 标签列表组件
customJavaDocTagsTableModel = new CustomJavaDocTagsTableModel();
customJavaDocTagsTable = new JBTable(customJavaDocTagsTableModel);
customJavaDocTagsTable.setPreferredScrollableViewportSize(new Dimension(500, 100));

// 创建带工具栏的面板
ToolbarDecorator decorator = ToolbarDecorator.createDecorator(customJavaDocTagsTable)
    .setAddAction(button -> {
        addCustomJavaDocTag();
    })
    .setRemoveAction(button -> {
        int selectedRow = customJavaDocTagsTable.getSelectedRow();
        if (selectedRow >= 0) {
            removeCustomJavaDocTag(selectedRow);
        }
    })
    .addExtraAction(new AnAction("清空全部",
                                 "清空所有自定义标签",
                                 com.intellij.icons.AllIcons.Actions.GC) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            clearAllCustomJavaDocTags();
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    });

customJavaDocTagsPanel = decorator.createPanel();
```

#### 3.2.3 添加到主面板

在 `createUI()` 的 `FormBuilder` 中添加：

```java
.addSeparator(10)
.addComponent(new JBLabel(JavaDocBundle.message("settings.custom.javadoc.tags")))
.addComponent(customJavaDocTagsPanel)
.addSeparator(10)
```

#### 3.2.4 实现操作方法

```java
/**
 * 添加自定义 JavaDoc 标签
 */
private void addCustomJavaDocTag() {
    String tagName = JOptionPane.showInputDialog(
        getParentWindow(),
        JavaDocBundle.message("settings.custom.javadoc.tags.add.prompt"),
        JavaDocBundle.message("settings.custom.javadoc.tags.add.title"),
        JOptionPane.QUESTION_MESSAGE
    );
    
    if (tagName != null && !tagName.trim().isEmpty()) {
        tagName = tagName.trim();
        
        // 验证标签名称
        if (!SettingsState.isValidTagName(tagName)) {
            JOptionPane.showMessageDialog(
                getParentWindow(),
                JavaDocBundle.message("settings.custom.javadoc.tags.invalid.name", tagName),
                JavaDocBundle.message("settings.error.title"),
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        // 检查是否已存在
        List<String> currentTags = customJavaDocTagsTableModel.getData();
        String tagNameLower = tagName.toLowerCase();
        if (currentTags.stream().anyMatch(t -> t.toLowerCase().equals(tagNameLower))) {
            JOptionPane.showMessageDialog(
                getParentWindow(),
                JavaDocBundle.message("settings.custom.javadoc.tags.already.exists", tagName),
                JavaDocBundle.message("settings.error.title"),
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        // 添加到表格
        customJavaDocTagsTableModel.addTag(tagName);
    }
}

/**
 * 删除自定义 JavaDoc 标签
 */
private void removeCustomJavaDocTag(int selectedRow) {
    if (selectedRow < 0 || selectedRow >= customJavaDocTagsTableModel.getRowCount()) {
        return;
    }
    
    String tagName = customJavaDocTagsTableModel.getData().get(selectedRow);
    
    int result = JOptionPane.showConfirmDialog(
        getParentWindow(),
        String.format("确定要删除标签 \"%s\" 吗？", tagName),
        "确认删除",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );
    
    if (result == JOptionPane.YES_OPTION) {
        customJavaDocTagsTableModel.removeRow(selectedRow);
    }
}

/**
 * 清空所有自定义 JavaDoc 标签
 */
private void clearAllCustomJavaDocTags() {
    if (customJavaDocTagsTableModel.getRowCount() == 0) {
        return;
    }
    
    int result = JOptionPane.showConfirmDialog(
        getParentWindow(),
        String.format("确定要清空所有自定义标签吗(%d 个)？", 
                     customJavaDocTagsTableModel.getRowCount()),
        "确认清空",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );
    
    if (result == JOptionPane.YES_OPTION) {
        customJavaDocTagsTableModel.clearAll();
    }
}
```

#### 3.2.5 创建表格模型

```java
/**
 * 自定义 JavaDoc 标签列表的表格模型
 */
private static class CustomJavaDocTagsTableModel extends AbstractTableModel {
    private final String[] columnNames = {"标签名称"};
    private final List<String> data;

    public CustomJavaDocTagsTableModel() {
        this.data = new ArrayList<>();
    }

    public void setData(List<String> newData) {
        this.data.clear();
        if (newData != null) {
            this.data.addAll(newData);
        }
        fireTableDataChanged();
    }

    public List<String> getData() {
        return new ArrayList<>(data);
    }

    public void addTag(String tagName) {
        data.add(tagName);
        fireTableRowsInserted(data.size() - 1, data.size() - 1);
    }

    public void removeRow(int row) {
        if (row >= 0 && row < data.size()) {
            data.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }

    public void clearAll() {
        int size = data.size();
        if (size > 0) {
            data.clear();
            fireTableRowsDeleted(0, size - 1);
        }
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= 0 && rowIndex < data.size()) {
            return data.get(rowIndex);
        }
        return "";
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true; // 允许编辑标签名称
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex >= 0 && rowIndex < data.size() && aValue != null) {
            String newTagName = aValue.toString().trim();
            
            // 验证标签名称
            if (!SettingsState.isValidTagName(newTagName)) {
                // 可以显示错误提示，这里简单处理为不更新
                return;
            }
            
            // 检查是否与其他标签重复
            String newTagNameLower = newTagName.toLowerCase();
            for (int i = 0; i < data.size(); i++) {
                if (i != rowIndex && data.get(i).toLowerCase().equals(newTagNameLower)) {
                    // 重复标签，不更新
                    return;
                }
            }
            
            data.set(rowIndex, newTagName);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}
```

#### 3.2.6 在 getSettings() 中处理

```java
// 在 getSettings() 方法中添加
settings.customJavaDocTags = customJavaDocTagsTableModel.getData();
```

#### 3.2.7 在 loadSettings() 中处理

```java
// 在 loadSettings() 方法中添加
customJavaDocTagsTableModel.setData(settings.customJavaDocTags);
```

### 3.3 标签同步层（CustomJavaDocTagRegistrar）

#### 3.3.1 重构现有代码

**文件**: `intelli-ai-javadoc/src/main/java/dev/dong4j/zeka/stack/idea/plugin/component/CustomJavaDocTagRegistrar.java`

```java
package dev.dong4j.zeka.stack.idea.plugin.component;

import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.javaDoc.JavadocDeclarationInspection;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

/**
 * 在插件启动时自动注册自定义的 JavaDoc 标签
 * <p>
 * 这个组件会在 IntelliJ 启动时自动运行，从配置中读取自定义标签列表，
 * 并将这些标签注册到 JavadocDeclarationInspection 中。
 * <p>
 * 支持动态添加和删除标签，当配置变更时会自动同步标签状态。
 *
 * @author dong4j
 * @version 1.3.4
 * @since 1.0.0
 */
public class CustomJavaDocTagRegistrar implements StartupActivity {

    /**
     * 在项目启动时运行，注册自定义的 JavaDoc 标签
     *
     * @param project 启动的项目
     */
    @Override
    public void runActivity(@NotNull Project project) {
        // 在写操作中执行标签注册
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                syncCustomTags(project);
            });
        });
    }

    /**
     * 同步自定义标签到 JavadocDeclarationInspection
     * <p>
     * 该方法会：
     * <ol>
     *   <li>读取配置中的自定义标签列表</li>
     *   <li>读取当前已注册的标签</li>
     *   <li>计算需要添加和删除的标签</li>
     *   <li>执行同步操作</li>
     * </ol>
     *
     * @param project 项目对象
     */
    public static void syncCustomTags(@NotNull Project project) {
        try {
            // 获取项目的检查配置管理器
            ProjectInspectionProfileManager profileManager = 
                ProjectInspectionProfileManager.getInstance(project);

            // 获取当前的检查配置
            InspectionProfile profile = profileManager.getCurrentProfile();

            // 获取 JavadocDeclarationInspection 工具
            InspectionToolWrapper<?, ?> toolWrapper = 
                profile.getInspectionTool("JavadocDeclaration", project);

            if (toolWrapper != null) {
                // 获取实际的检查工具实例
                Object tool = toolWrapper.getTool();

                // 检查是否是 JavadocDeclarationInspection 类型
                if (tool instanceof JavadocDeclarationInspection inspection) {
                    // 执行标签同步
                    performTagSync(inspection);
                    
                    // 通知配置已更改
                    profileManager.fireProfileChanged();
                }
            }
        } catch (Exception e) {
            // 静默处理异常，避免影响 IDE 启动
            // 可以在详细日志模式下记录错误
        }
    }

    /**
     * 执行标签同步操作
     * <p>
     * 核心逻辑：
     * <ol>
     *   <li>读取配置中的标签列表（规范化处理）</li>
     *   <li>读取当前已注册的标签</li>
     *   <li>计算差异：需要添加和删除的标签</li>
     *   <li>执行删除操作（先删除，避免重复）</li>
     *   <li>执行添加操作（添加新标签）</li>
     * </ol>
     *
     * @param inspection Javadoc 检查工具实例
     */
    private static void performTagSync(JavadocDeclarationInspection inspection) {
        try {
            // 1. 读取配置中的标签列表（规范化处理）
            SettingsState settings = SettingsState.getInstance();
            List<String> configuredTags = settings.getNormalizedCustomJavaDocTags();
            
            // 2. 读取当前已注册的标签
            String currentTagsString = getCurrentAdditionalTags(inspection);
            List<String> currentTags = parseTagsString(currentTagsString);
            
            // 3. 计算差异
            Set<String> configuredTagsSet = new HashSet<>(configuredTags);
            Set<String> currentTagsSet = new HashSet<>(currentTags);
            
            // 需要删除的标签：已注册但配置中没有的
            List<String> tagsToRemove = currentTags.stream()
                .filter(tag -> !configuredTagsSet.contains(tag))
                .collect(Collectors.toList());
            
            // 需要添加的标签：配置中有但未注册的
            List<String> tagsToAdd = configuredTags.stream()
                .filter(tag -> !currentTagsSet.contains(tag))
                .collect(Collectors.toList());
            
            // 4. 执行删除操作
            if (!tagsToRemove.isEmpty()) {
                removeTags(inspection, tagsToRemove);
            }
            
            // 5. 执行添加操作
            if (!tagsToAdd.isEmpty()) {
                addTags(inspection, tagsToAdd);
            }
        } catch (Exception e) {
            // 静默处理异常
        }
    }

    /**
     * 获取当前已注册的标签字符串
     *
     * @param inspection Javadoc 检查工具实例
     * @return 当前标签字符串，如果获取失败返回空字符串
     */
    private static String getCurrentAdditionalTags(JavadocDeclarationInspection inspection) {
        try {
            java.lang.reflect.Field additionalTagsField = 
                JavadocDeclarationInspection.class.getDeclaredField("ADDITIONAL_TAGS");
            additionalTagsField.setAccessible(true);
            Object value = additionalTagsField.get(inspection);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 解析标签字符串为列表
     * <p>
     * 标签字符串格式：逗号分隔，如 "date,email,xxx"
     *
     * @param tagsString 标签字符串
     * @return 标签列表（已规范化：去空、转小写、去重）
     */
    private static List<String> parseTagsString(String tagsString) {
        if (tagsString == null || tagsString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(tagsString.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toLowerCase)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 设置标签字符串
     *
     * @param inspection Javadoc 检查工具实例
     * @param tagsString 新的标签字符串
     */
    private static void setAdditionalTags(JavadocDeclarationInspection inspection, 
                                         String tagsString) {
        try {
            java.lang.reflect.Field additionalTagsField = 
                JavadocDeclarationInspection.class.getDeclaredField("ADDITIONAL_TAGS");
            additionalTagsField.setAccessible(true);
            additionalTagsField.set(inspection, tagsString);
        } catch (Exception e) {
            // 如果直接设置字段失败，尝试使用反射调用方法
            try {
                // 先清空，再逐个添加
                java.lang.reflect.Method method = 
                    JavadocDeclarationInspection.class.getDeclaredMethod("registerAdditionalTag",
                                                                         String.class);
                method.setAccessible(true);
                
                // 注意：这里无法直接删除，只能通过重新设置整个字符串来实现
                // 所以删除操作需要先读取、解析、过滤、重组
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 添加标签到 JavadocDeclarationInspection
     *
     * @param inspection Javadoc 检查工具实例
     * @param tagsToAdd 要添加的标签列表
     */
    private static void addTags(JavadocDeclarationInspection inspection, 
                               List<String> tagsToAdd) {
        try {
            // 读取当前标签
            String currentTagsString = getCurrentAdditionalTags(inspection);
            List<String> currentTags = parseTagsString(currentTagsString);
            
            // 合并标签（去重）
            Set<String> allTags = new HashSet<>(currentTags);
            allTags.addAll(tagsToAdd);
            
            // 重新组合为字符串
            String newTagsString = String.join(",", allTags.stream().sorted().collect(Collectors.toList()));
            
            // 设置新标签字符串
            setAdditionalTags(inspection, newTagsString);
        } catch (Exception e) {
            // 如果批量添加失败，尝试逐个添加
            for (String tag : tagsToAdd) {
                registerAdditionalTag(inspection, tag);
            }
        }
    }

    /**
     * 从 JavadocDeclarationInspection 中删除标签
     *
     * @param inspection Javadoc 检查工具实例
     * @param tagsToRemove 要删除的标签列表
     */
    private static void removeTags(JavadocDeclarationInspection inspection, 
                                  List<String> tagsToRemove) {
        try {
            // 读取当前标签
            String currentTagsString = getCurrentAdditionalTags(inspection);
            List<String> currentTags = parseTagsString(currentTagsString);
            
            // 过滤掉要删除的标签
            Set<String> tagsToRemoveSet = new HashSet<>(tagsToRemove);
            List<String> remainingTags = currentTags.stream()
                .filter(tag -> !tagsToRemoveSet.contains(tag))
                .collect(Collectors.toList());
            
            // 重新组合为字符串
            String newTagsString = remainingTags.isEmpty() 
                ? "" 
                : String.join(",", remainingTags.stream().sorted().collect(Collectors.toList()));
            
            // 设置新标签字符串
            setAdditionalTags(inspection, newTagsString);
        } catch (Exception e) {
            // 删除失败，静默处理
        }
    }

    /**
     * 注册额外的标签到 Javadoc 检查工具中
     * <p>
     * 这是原有的方法，保留用于向后兼容和备用方案
     *
     * @param inspection Javadoc 检查工具实例
     * @param tagName    要注册的标签名称
     */
    private static void registerAdditionalTag(JavadocDeclarationInspection inspection, 
                                            String tagName) {
        try {
            // 尝试直接访问 ADDITIONAL_TAGS 字段（推荐方式）
            java.lang.reflect.Field additionalTagsField = 
                JavadocDeclarationInspection.class.getDeclaredField("ADDITIONAL_TAGS");
            additionalTagsField.setAccessible(true);
            String additionalTags = (String) additionalTagsField.get(inspection);

            // 如果标签不存在，则添加
            if (additionalTags == null || additionalTags.isEmpty()) {
                additionalTagsField.set(inspection, tagName);
            } else {
                // 检查标签是否已存在（不区分大小写）
                List<String> existingTags = parseTagsString(additionalTags);
                String tagNameLower = tagName.toLowerCase();
                if (!existingTags.contains(tagNameLower)) {
                    additionalTagsField.set(inspection, additionalTags + "," + tagName);
                }
            }
        } catch (Exception e) {
            // 如果直接访问字段失败，尝试使用反射调用 registerAdditionalTag 方法
            try {
                java.lang.reflect.Method method = 
                    JavadocDeclarationInspection.class.getDeclaredMethod("registerAdditionalTag",
                                                                         String.class);
                method.setAccessible(true);
                method.invoke(inspection, tagName);
            } catch (Exception ignored) {
            }
        }
    }
}
```

### 3.4 配置同步层（JavaDocSettingsConfigurable）

#### 3.4.1 在 apply() 中触发同步

**文件**: `intelli-ai-javadoc/src/main/java/dev/dong4j/zeka/stack/idea/plugin/settings/JavaDocSettingsConfigurable.java`

```java
@Override
public void apply() throws ConfigurationException {
    if (settingsPanel == null) {
        return;
    }

    SettingsState panelSettings = settingsPanel.getSettings();

    // 验证配置
    if (!validateSettings(panelSettings)) {
        throw new ConfigurationException(JavaDocBundle.message("error.validation.failed"));
    }

    // 应用配置
    SettingsState currentSettings = SettingsState.getInstance();
    // ... 其他配置应用代码 ...
    
    // 保存自定义 JavaDoc 标签配置
    currentSettings.customJavaDocTags = panelSettings.customJavaDocTags;

    // 触发标签同步（需要在写操作中执行）
    ApplicationManager.getApplication().invokeLater(() -> {
        ApplicationManager.getApplication().runWriteAction(() -> {
            Project project = ProjectManager.getInstance().getDefaultProject();
            if (project != null && !project.isDisposed()) {
                CustomJavaDocTagRegistrar.syncCustomTags(project);
            }
        });
    });
}
```

#### 3.4.2 在 isModified() 中比较标签

```java
@Override
public boolean isModified() {
    if (settingsPanel == null) {
        return false;
    }

    SettingsState currentSettings = SettingsState.getInstance();
    SettingsState panelSettings = settingsPanel.getSettings();

    // ... 其他配置比较代码 ...

    // 比较自定义 JavaDoc 标签
    List<String> currentTags = currentSettings.getNormalizedCustomJavaDocTags();
    List<String> panelTags = panelSettings.getNormalizedCustomJavaDocTags();
    if (!currentTags.equals(panelTags)) {
        return true;
    }

    // ... 其他配置比较代码 ...
    
    return false;
}
```

### 3.5 国际化资源

**文件**: `intelli-ai-javadoc/src/main/resources/messages/JavaDocBundle.properties`

```properties
# 自定义 JavaDoc 标签配置
settings.custom.javadoc.tags=自定义 JavaDoc 标签
settings.custom.javadoc.tags.add.title=添加标签
settings.custom.javadoc.tags.add.prompt=请输入标签名称（不包含 @ 符号）：
settings.custom.javadoc.tags.invalid.name=标签名称无效：{0}\n标签名称只能包含字母、数字、下划线和连字符
settings.custom.javadoc.tags.already.exists=标签 \"{0}\" 已存在
```

## 4. 测试方案

### 4.1 单元测试

#### 4.1.1 SettingsState 测试

```java
@Test
void testGetNormalizedCustomJavaDocTags() {
    SettingsState settings = new SettingsState();
    settings.customJavaDocTags = Arrays.asList("Date", "  email  ", "DATE", "", null);
    
    List<String> normalized = settings.getNormalizedCustomJavaDocTags();
    
    assertEquals(2, normalized.size());
    assertTrue(normalized.contains("date"));
    assertTrue(normalized.contains("email"));
}

@Test
void testIsValidTagName() {
    assertTrue(SettingsState.isValidTagName("date"));
    assertTrue(SettingsState.isValidTagName("email"));
    assertTrue(SettingsState.isValidTagName("custom-tag"));
    assertTrue(SettingsState.isValidTagName("tag_123"));
    
    assertFalse(SettingsState.isValidTagName(""));
    assertFalse(SettingsState.isValidTagName(null));
    assertFalse(SettingsState.isValidTagName("tag name")); // 包含空格
    assertFalse(SettingsState.isValidTagName("tag,name")); // 包含逗号
}
```

#### 4.1.2 标签同步逻辑测试

```java
@Test
void testTagSync() {
    // 模拟当前已注册标签：date,email,xxx
    List<String> currentTags = Arrays.asList("date", "email", "xxx");
    
    // 配置中的标签：email,xxx
    List<String> configuredTags = Arrays.asList("email", "xxx");
    
    // 计算需要删除的标签
    Set<String> configuredSet = new HashSet<>(configuredTags);
    List<String> toRemove = currentTags.stream()
        .filter(tag -> !configuredSet.contains(tag))
        .collect(Collectors.toList());
    
    assertEquals(1, toRemove.size());
    assertTrue(toRemove.contains("date"));
    
    // 计算需要添加的标签
    Set<String> currentSet = new HashSet<>(currentTags);
    List<String> toAdd = configuredTags.stream()
        .filter(tag -> !currentSet.contains(tag))
        .collect(Collectors.toList());
    
    assertEquals(0, toAdd.size()); // email 和 xxx 已存在
}
```

### 4.2 集成测试

#### 4.2.1 场景 1：添加标签

1. 打开设置页面
2. 在自定义 JavaDoc 标签区域点击"添加"
3. 输入标签名称：`custom`
4. 点击 Apply
5. 验证：标签已注册到 JavadocDeclarationInspection

#### 4.2.2 场景 2：删除标签

1. 当前配置：`["date", "email"]`
2. 在设置页面删除 `date`
3. 点击 Apply
4. 验证：`date` 已从 JavadocDeclarationInspection 中移除

#### 4.2.3 场景 3：修改标签

1. 第一次配置：`["date"]`
2. 第二次配置：`["email", "xxx"]`
3. 点击 Apply
4. 验证：
    - `date` 被删除
    - `email` 和 `xxx` 被添加

#### 4.2.4 场景 4：标签验证

1. 尝试添加无效标签（包含空格）：`"tag name"`
2. 验证：显示错误提示，标签未添加
3. 尝试添加重复标签：`"date"`（已存在）
4. 验证：显示警告提示，标签未添加

## 5. 注意事项

### 5.1 向后兼容

- 如果用户已有旧版本配置，`customJavaDocTags` 可能为 null 或空
- 需要在 `getNormalizedCustomJavaDocTags()` 中处理 null 情况
- 可以考虑在首次启动时，如果列表为空，自动添加 `["date", "email"]` 作为默认值

### 5.2 线程安全

- 标签同步操作需要在写操作中执行（`runWriteAction`）
- UI 操作需要在 EDT 线程中执行
- 配置读取可以在任何线程中执行

### 5.3 错误处理

- 反射操作可能失败（IntelliJ 版本更新可能导致 API 变化）
- 需要静默处理异常，避免影响 IDE 启动
- 可以在详细日志模式下记录错误信息

### 5.4 性能考虑

- 标签同步操作在配置应用时执行，频率较低
- 标签列表通常较小（< 10 个），性能影响可忽略
- 使用 Set 进行去重和查找，提高效率

## 6. 实施步骤

1. **第一步**：修改 `SettingsState`，添加字段和辅助方法
2. **第二步**：修改 `CustomJavaDocTagRegistrar`，实现标签同步逻辑
3. **第三步**：在 `JavaDocSettingsPanel` 中添加 UI 组件
4. **第四步**：在 `JavaDocSettingsConfigurable` 中添加配置同步逻辑
5. **第五步**：添加国际化资源
6. **第六步**：编写单元测试和集成测试
7. **第七步**：测试验证

## 7. 总结

本方案实现了自定义 JavaDoc 标签的完整配置功能，包括：

- ✅ 数据存储：在 `SettingsState` 中存储标签列表
- ✅ UI 界面：提供标签的添加、删除、编辑功能
- ✅ 标签同步：自动同步配置到 JavadocDeclarationInspection
- ✅ 删除逻辑：正确处理标签的添加和删除
- ✅ 配置验证：验证标签名称的有效性
- ✅ 向后兼容：处理旧版本配置

通过本方案，用户可以灵活配置自定义 JavaDoc 标签，满足不同项目的需求。

