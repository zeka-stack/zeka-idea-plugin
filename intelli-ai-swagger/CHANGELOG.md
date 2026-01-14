# 变更日志

## 2026.01.14

- 入口与校验：右键仅在 Java 文件启用，定位当前方法并校验是否为 Spring 控制器方法后再触发生成，见
  intelli-ai-swagger/src/main/java/dev/dong4j/zeka/stack/idea/plugin/swagger/action/SwaggerAction.java
- AI 请求与 Prompt：按“类/映射/方法签名/参数/返回值”组装上下文，强制 OpenAPI 3 注解片段输出，见
  intelli-ai-swagger/src/main/java/dev/dong4j/zeka/stack/idea/plugin/swagger/ai/SwaggerAIRequestComposer.java、intelli-ai-swagger/src/main/java/dev/dong4j/zeka/stack/idea/plugin/swagger/util/SwaggerPromptBuilder.java
- 写回与 import：清理旧 Swagger 注解、补全全限定名、写入并通过 JavaCodeStyleManager 生成 import，见
  intelli-ai-swagger/src/main/java/dev/dong4j/zeka/stack/idea/plugin/swagger/util/SwaggerAnnotationWriter.java、intelli-ai-swagger/src/main/java/dev/dong4j/zeka/stack/idea/plugin/swagger/util/SwaggerAnnotationUtil.java
- 持久化与文案：新增覆盖开关默认值与提示词模板，补充提示文案，见
  intelli-ai-swagger/src/main/java/dev/dong4j/zeka/stack/idea/plugin/swagger/settings/SettingsState.java、intelli-ai-swagger/src/main/resources/messages/SwaggerBundle.properties、intelli-ai-swagger/src/main/resources/messages/SwaggerBundle_zh_CN.properties

