## 自定义 JavaDoc 标签持久化问题说明

### 1. 问题背景

在 IntelliAI Javadoc 插件中，自定义 JavaDoc 标签配置通过 `SettingsState` 持久化：

- 配置类：`SettingsState`（实现 `PersistentStateComponent<SettingsState>`）
- 字段：`public List<CustomJavadocTag> customJavadocTags`
- 对应 UI：`CustomJavadocTagsPanel` + `JavadocSettingsPanel` + `JavadocSettingsConfigurable`

用户在「设置 → Tools → IntelliAI Javadoc」中修改自定义标签（如 author / date / email 的默认值）并点击 **Apply** 后：

- 运行时内存中的配置是生效的；
- 但在实际持久化的 XML 文件中看不到 `customJavadocTags`，IDE 重启后部分修改无法恢复。

### 2. 现象描述

典型现象：

- 只修改已有标签（如 `author`、`date`、`email`）的 **默认值**，不新增/删除标签时：
    - 设置页面点击 **Apply** 没有报错；
    - 插件运行时可以读到新的默认值；
    - 但持久化 XML 中没有 `customJavadocTags` 相关节点；
    - IDE 重启后，默认值恢复为代码里的默认值。

- 如果新增一个全新的标签（例如 `role`），再点击 **Apply**：
    - 持久化 XML 中会出现 `customJavadocTags`；
    - 新增的标签和部分默认值可以正确恢复。

### 3. 根因分析

IntelliJ 持久化 `PersistentStateComponent` 时的核心逻辑：

1. 框架会先创建一个「默认」实例：`new SettingsState()`；
2. 将当前要保存的实例与默认实例做字段级比较；
3. **只有“和默认值不同”的字段才会写入 XML**，相同的字段会被认为「不需要持久化」。

在本项目中：

- 默认值中定义了：

  ```java
  public List<CustomJavadocTag> customJavadocTags = new ArrayList<>() {
      {
          add(new CustomJavadocTag("author", "zeka.stack.team"));
          add(new CustomJavadocTag("date", "yyyy.MM.dd"));
          add(new CustomJavadocTag("email", "mailto:zeka.stack@gmail.com"));
      }
  };
  ```

- `CustomJavadocTag` 的 `equals` / `hashCode` 实现为（历史实现）：

  ```java
  @Override
  public boolean equals(Object o) {
      if (this == o) {
          return true;
      }
      if (o == null || getClass() != o.getClass()) {
          return false;
      }

      CustomJavadocTag that = (CustomJavadocTag) o;

      // 仅比较标签名，忽略 defaultValue
      return tagName.equalsIgnoreCase(that.tagName);
  }

  @Override
  public int hashCode() {
      return tagName.toLowerCase().hashCode();
  }
  ```

这会导致一个关键问题：

- 对于 `author/date/email` 这三条默认标签，只要 **标签名不变**，无论 `defaultValue` 怎么改，`equals` 仍然认为「两个标签相等」；
- 在列表比较时，默认实例和当前实例的 `customJavadocTags` 会被视为「相同列表」；
- 框架据此判断「该字段未修改」，因此 **不会把 `customJavadocTags` 持久化到 XML**；
- 最终表现为：运行时修改生效，但重启后丢失。

### 4. 解决方案（当前实现）

为快速修复「自定义 JavaDoc 标签无法持久化」问题，目前采取的方案是：

- **移除 `CustomJavadocTag` 中自定义的 `equals` / `hashCode` 实现**，回退为 `Object` 默认实现。

这样会带来两个效果：

1. 列表比较不再基于“只看 tagName 的内容相等”，而是基于实例引用等默认语义；
2. 一旦用户通过 UI 修改了标签列表（包括修改默认值），当前实例与默认实例在 `customJavadocTags` 字段上就一定不相等；

从而：

- IntelliJ 持久化框架认为 `customJavadocTags` 字段已修改；
- 在保存时会把该字段完整写入 XML；
- 下次启动时通过 `loadState()` 能正确还原所有自定义标签和默认值。

### 5. 方案权衡与注意事项

移除 `equals` / `hashCode` 的影响与注意点：

- **好处**
    - 彻底避免“因为自定义相等逻辑过于宽松，导致框架误判字段未修改”的问题；
    - 行为更接近普通 DTO / 配置 Bean，更符合 XML 序列化场景。

- **潜在影响**
    - 如果未来在其他地方（例如 `Set<CustomJavadocTag>` 或基于 `contains` / `remove` 等操作）依赖「按 tagName 去重」的语义，需要在对应业务代码中显式处理，而不是依赖
      `equals`。
    - 当前代码中对于自定义标签去重，已经在 UI 层和 `SettingsState.getNormalizedCustomJavaDocTags()` 里通过 `tagName.toLowerCase()` 显式处理，因此移除
      `equals` 对现有逻辑影响可控。

### 6. 类似问题排查建议

以后如果再遇到「某个配置字段无法持久化」的问题，可以按下面的思路排查：

1. **确认字段是否真的“被认为修改了”**
    - 在 `isModified()` 中对该字段做对比，看 UI 侧是否能检测到变化；
    - 若 `isModified()` 是 `true`，但 XML 中没有对应字段，说明问题在组件持久化阶段。

2. **检查默认值与当前值的比较方式**
    - 是否给字段类型自定义了 `equals` / `hashCode`；
    - 是否只比较了部分字段，导致某些修改被“忽略不计”；
    - 是否使用了只看 key 的相等逻辑（典型如只比较 name，不比较 value）。

3. **调整相等语义或持久化注解**
    - 优先方式：让 `equals` / `hashCode` 真实反映「我们期望被认为是同一个配置」的全部字段；
    - 或在极端情况下，考虑使用 `@Property(alwaysWrite = true)`（但这会增加 XML 体积，应谨慎使用）。

### 7. 总结

本次问题的根因是：**配置对象的相等语义与 IntelliJ 持久化框架的默认比较策略叠加后，导致字段被错误地认为“未修改”，从而没有写入 XML**。
通过移除 `CustomJavadocTag` 的自定义 `equals` / `hashCode`，我们让持久化框架能正确识别 `customJavadocTags` 已被修改，从而实现自定义 JavaDoc
标签配置的稳定持久化。
后续在设计持久化配置类时，应特别注意：自定义相等逻辑会直接影响字段是否被写入磁盘。


