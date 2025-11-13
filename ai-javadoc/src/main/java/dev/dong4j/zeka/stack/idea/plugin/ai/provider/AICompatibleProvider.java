// package dev.dong4j.zeka.stack.idea.plugin.ai.provider;
//
// import com.google.gson.JsonArray;
// import com.google.gson.JsonElement;
// import com.google.gson.JsonObject;
// import com.google.gson.JsonParser;
//
// import com.intellij.openapi.diagnostic.Logger;
// import com.intellij.openapi.project.Project;
// import com.intellij.util.io.HttpRequests;
//
// import org.jetbrains.annotations.NotNull;
// import org.jetbrains.annotations.Nullable;
//
// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.TimeUnit;
//
// import dev.dong4j.zeka.stack.idea.plugin.ai.AIServiceException;
// import dev.dong4j.zeka.stack.idea.plugin.ai.ValidationResult;
// import dev.dong4j.zeka.stack.idea.plugin.console.JavaDocConsoleView;
// import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
// import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
// import dev.dong4j.zeka.stack.idea.plugin.util.TokenCounter;
//
// /**
//  * OpenAI 兼容的服务提供商抽象类
//  *
//  * <p>为兼容 OpenAI API 格式的服务提供商提供通用实现。
//  * 大多数现代 AI 服务都提供 OpenAI 兼容的 API 接口，
//  * 通过继承此类可以快速实现对新提供商的支持。
//  *
//  * <p>该抽象类实现了 AIServiceProvider 接口的大部分功能，
//  * 包括 HTTP 请求处理、错误处理、重试机制等通用逻辑。
//  * 子类只需要关注提供商特定的配置和元数据。
//  *
//  * <p>核心功能：
//  * <ul>
//  *   <li>HTTP 请求构建和发送</li>
//  *   <li>响应解析和错误处理</li>
//  *   <li>重试机制和指数退避</li>
//  *   <li>日志记录和调试支持</li>
//  *   <li>配置验证</li>
//  * </ul>
//  *
//  * <p>子类只需要实现以下方法：
//  * <ul>
//  *   <li>{@link #getProviderId()} - 获取提供商标识符</li>
//  *   <li>{@link #getProviderName()} - 获取提供商显示名称</li>
//  *   <li>{@link #getSupportedModels()} - 获取支持的模型列表</li>
//  *   <li>{@link #getDefaultModel()} - 获取默认模型</li>
//  *   <li>{@link #getDefaultBaseUrl()} - 获取默认 Base URL</li>
//  *   <li>{@link #requiresApiKey()} - 是否需要 API Key</li>
//  * </ul>
//  *
//  * @author dong4j
//  * @version 1.0.0
//  * @since 1.0.0
//  */
// @SuppressWarnings( {"LoggingSimilarMessage", "D", "DuplicatedCode"})
// public abstract class AICompatibleProvider implements AIServiceProvider {
//
//     /** AI 兼容提供者日志记录器 */
//     private static final Logger LOG = Logger.getInstance(AICompatibleProvider.class);
//
//     /** ThreadLocal 存储当前项目实例，用于 Console 日志输出 */
//     private static final ThreadLocal<Project> CURRENT_PROJECT = new ThreadLocal<>();
//
//     /** 用户界面设置状态对象 */
//     protected final SettingsState settings;
//     /** 提供商配置信息，用于存储和管理提供商相关的配置参数 */
//     protected final SettingsState.ProviderConfig providerConfig;
//
//     /**
//      * 初始化 AI 兼容提供者
//      * <p>
//      * 使用给定的设置状态初始化 AI 兼容提供者，用于后续 AI 相关功能的配置和调用。
//      *
//      * @param settings 设置状态对象，用于配置 AI 兼容提供者的相关参数
//      */
//     protected AICompatibleProvider(SettingsState settings, SettingsState.ProviderConfig providerConfig) {
//         this.settings = settings;
//         this.providerConfig = providerConfig;
//     }
//
//     /**
//      * 获取当前AI服务提供商的ID
//      * <p>
//      * 返回AI服务提供商的唯一标识符，该标识符由AIProviderType枚举中的LM_STUDIO类型定义。
//      *
//      * @return AI服务提供商的ID
//      */
//     @Override
//     @NotNull
//     public String getProviderId() {
//         return providerConfig.providerType.getProviderId();
//     }
//
//     /**
//      * 获取当前AI服务提供商的名称
//      * <p>
//      * 返回AI服务提供商的显示名称，该名称由AIProviderType枚举中的LM_STUDIO值提供。
//      *
//      * @return AI服务提供商的显示名称
//      */
//     @Override
//     @NotNull
//     public String getProviderName() {
//         return providerConfig.providerType.getDisplayName();
//     }
//
//     /**
//      * 获取当前AI提供者支持的模型列表
//      * <p>
//      * 调用AI提供者类型对应的获取支持模型方法，返回支持的模型名称列表
//      *
//      * @return 支持的模型名称列表
//      */
//     @Override
//     @NotNull
//     public List<String> getSupportedModels() {
//         return providerConfig.providerType.getSupportedModels();
//     }
//
//     /**
//      * 获取默认模型名称
//      * <p>
//      * 返回当前AI提供者默认的模型名称。
//      *
//      * @return 默认模型名称
//      */
//     @Override
//     @NotNull
//     public String getDefaultModel() {
//         return providerConfig.providerType.getDefaultModel();
//     }
//
//     /**
//      * 获取默认的基础URL
//      * <p>
//      * 返回AI服务提供商类型LM_STUDIO对应的基础URL。
//      *
//      * @return 默认的基础URL
//      */
//     @Override
//     @NotNull
//     public String getDefaultBaseUrl() {
//         return providerConfig.providerType.getDefaultBaseUrl();
//     }
//
//     /**
//      * 判断当前AI服务提供商是否需要API密钥
//      * <p>
//      * 调用对应AI服务提供商的requiresApiKey方法，返回是否需要API密钥
//      *
//      * @return 是否需要API密钥
//      */
//     @Override
//     public boolean requiresApiKey() {
//         return providerConfig.providerType.requiresApiKey();
//     }
//
//     /**
//      * 设置当前项目实例（用于 Console 日志输出）
//      *
//      * @param project 项目实例
//      */
//     public static void setCurrentProject(@Nullable Project project) {
//         if (project != null) {
//             CURRENT_PROJECT.set(project);
//         } else {
//             CURRENT_PROJECT.remove();
//         }
//     }
//
//     /**
//      * 获取当前项目实例
//      *
//      * @return 项目实例，如果不存在返回 null
//      */
//     @Nullable
//     protected static Project getCurrentProject() {
//         return CURRENT_PROJECT.get();
//     }
//
//     /**
//      * 生成文档注释内容
//      * <p>
//      * 根据提供的代码、任务类型和语言生成对应的文档注释。该方法会尝试多次调用AI服务生成文档，若失败则抛出异常。
//      *
//      * @param code     代码内容
//      * @param type     文档生成任务类型
//      * @param language 文档语言
//      * @return 生成的文档注释内容
//      * @throws AIServiceException 当AI服务调用失败时抛出
//      */
//     @SuppressWarnings("D")
//     @Override
//     @NotNull
//     public String generateDocumentation(@NotNull String code,
//                                         @NotNull DocumentationTask.TaskType type,
//                                         @NotNull String language) throws AIServiceException {
//
//         if (settings.verboseLogging) {
//             LOG.debug("=== Generate Documentation ===");
//             LOG.debug("Type: " + type);
//             LOG.debug("Language: " + language);
//             LOG.debug("Code Length: " + code.length() + " characters");
//             LOG.debug("Code Preview:\n " + truncateForLog(code, 300));
//         }
//
//         String prompt = buildPrompt(code, type, language);
//
//         if (settings.verboseLogging) {
//             LOG.debug("Built Prompt Length: " + prompt.length() + " characters");
//         }
//
//         int attempts = 0;
//         AIServiceException lastException = null;
//
//         while (attempts < settings.maxRetries) {
//             try {
//                 if (settings.verboseLogging) {
//                     LOG.debug("Attempt " + (attempts + 1) + "/" + settings.maxRetries + " to generate documentation");
//                 }
//
//                 String result = sendRequest(prompt);
//
//                 if (settings.verboseLogging) {
//                     LOG.debug("Successfully generated documentation on attempt " + (attempts + 1));
//                 }
//
//                 return result;
//             } catch (AIServiceException e) {
//                 lastException = e;
//                 attempts++;
//
//                 if (!e.isRetryable() || attempts >= settings.maxRetries) {
//                     LOG.info("Generation failed after " + attempts + " attempts: " + e.getMessage());
//                     break;
//                 }
//
//                 // 指数退避
//                 long waitTime = (long) (settings.waitDuration * Math.pow(2, attempts - 1));
//                 LOG.warn("Request failed, retrying in " + waitTime + "ms (attempt " + attempts + "/" +
//                          settings.maxRetries + "): " + e.getMessage());
//
//                 try {
//                     TimeUnit.MILLISECONDS.sleep(waitTime);
//                 } catch (InterruptedException ie) {
//                     Thread.currentThread().interrupt();
//                     throw new AIServiceException("Interrupted during retry",
//                                                  AIServiceException.ErrorCode.UNKNOWN_ERROR, ie);
//                 }
//             }
//         }
//
//         throw lastException != null ? lastException :
//               new AIServiceException("Failed after " + attempts + " attempts");
//     }
//
//     /**
//      * 发送请求到 AI 服务
//      *
//      * <p>构建完整的 HTTP 请求并发送到 AI 服务，
//      * 处理响应并返回生成的文本内容。
//      * 这是与 AI 服务交互的核心方法。
//      *
//      * <p>请求处理流程：
//      * <ol>
//      *   <li>构建 HTTP 请求头</li>
//      *   <li>构建请求体（JSON 格式）</li>
//      *   <li>发送 POST 请求到 /chat/completions 端点</li>
//      *   <li>处理响应并解析结果</li>
//      * </ol>
//      *
//      * <p>错误处理策略：
//      * <ul>
//      *   <li>客户端错误（4xx）：转换为相应的 AIServiceException</li>
//      *   <li>服务器错误（5xx）：转换为相应的 AIServiceException</li>
//      *   <li>网络错误：转换为 NETWORK_ERROR 类型的 AIServiceException</li>
//      *   <li>其他异常：转换为 UNKNOWN_ERROR 类型的 AIServiceException</li>
//      * </ul>
//      *
//      * @param prompt 提示词，包含代码和生成指令
//      * @return AI 生成的文本内容
//      * @throws AIServiceException 当请求失败时抛出，包含详细的错误信息
//      * @see #buildRequestBody(String)
//      * @see #parseResponse(String)
//      */
//     protected String sendRequest(String prompt) throws AIServiceException {
//         JsonObject body = buildRequestBody(prompt);
//         // 非设置页面调用, 使用持久化配置
//         String apiKey = SettingsState.getApiKey(providerConfig.md5);
//         return sendRequestWithBody(body, "AI Request", TokenCounter.estimateTokens(prompt), apiKey, this::parseResponse);
//     }
//
//     /**
//      * 发送轻量级验证请求
//      *
//      * <p>专门用于配置验证的轻量级请求方法，
//      * 避免使用完整的注释生成提示词，减少 token 消耗。
//      * 使用最小化的测试内容来验证服务连接和配置正确性。
//      *
//      * <p>优化策略：
//      * <ul>
//      *   <li>使用简化的系统提示词，只包含基本的角色设定</li>
//      *   <li>使用极简的用户提示词，只要求简单响应</li>
//      *   <li>设置较小的 max_tokens 限制</li>
//      *   <li>降低 temperature 参数以提高响应一致性</li>
//      * </ul>
//      *
//      * <p>验证目标：
//      * <ul>
//      *   <li>验证 API Key 是否有效</li>
//      *   <li>验证 Base URL 是否可访问</li>
//      *   <li>验证模型名称是否正确</li>
//      *   <li>验证服务是否正常运行</li>
//      * </ul>
//      *
//      * @return AI 服务的响应内容
//      * @throws AIServiceException 当验证请求失败时抛出
//      */
//     protected String sendValidationRequest(String apiKey) throws AIServiceException {
//         JsonObject body = buildValidationRequestBody();
//         return sendRequestWithBody(body, "Validation Request", 0, apiKey, this::parseValidationResponse);
//     }
//
//     /**
//      * 响应解析器函数接口
//      * 允许抛出 AIServiceException 的解析函数
//      */
//     @FunctionalInterface
//     private interface ResponseParser {
//         /**
//          * 解析响应内容并返回解析结果
//          * <p>
//          * 将接收到的响应内容进行解析，提取所需数据并返回
//          *
//          * @param responseBody 响应内容字符串
//          * @return 解析后的结果
//          * @throws AIServiceException 如果解析过程中发生错误
//          */
//         String parse(String responseBody) throws AIServiceException;
//     }
//
//     /**
//      * 向指定的API端点发送带有请求体的HTTP请求，并解析返回的响应内容
//      * <p>
//      * 该方法用于构建并发送HTTP POST请求，处理API Key验证、请求日志记录、超时设置、响应解析等操作。
//      * 如果请求成功且响应内容非空，则返回解析后的结果；否则抛出相应的异常。
//      *
//      * @param body           请求体，使用JsonObject格式
//      * @param logPrefix      日志前缀，用于区分不同请求的日志信息
//      * @param promptLength   提示内容长度，用于日志记录
//      * @param responseParser 响应解析器，用于解析服务器返回的响应内容
//      * @return 解析后的响应结果字符串
//      * @throws AIServiceException 如果发生配置错误、网络错误、响应无效或未知错误
//      */
//     private String sendRequestWithBody(JsonObject body,
//                                        String logPrefix,
//                                        int promptLength,
//                                        String apiKey,
//                                        ResponseParser responseParser) throws AIServiceException {
//         try {
//             // 根据不同的客户端判断是否检查 apikey
//             if (requiresApiKey()) {
//                 if (apiKey == null || apiKey.trim().isEmpty()) {
//                     throw new AIServiceException("需要 API 密钥但未进行配置",
//                                                  AIServiceException.ErrorCode.CONFIGURATION_ERROR);
//                 }
//             }
//
//             String url = providerConfig.baseUrl + "/chat/completions";
//             String requestBody = body.toString();
//
//             // 调试日志：记录请求信息
//             if (settings.verboseLogging) {
//                 LOG.trace("=== " + logPrefix + " ===");
//                 LOG.trace("URL: " + url);
//                 LOG.trace("Model: " + providerConfig.modelName);
//                 LOG.trace("Request Body: " + truncateForLog(requestBody,
//                                                             "Validation Request".equals(logPrefix) ? 500 : 1000));
//                 if (promptLength > 0) {
//                     LOG.trace("Prompt Length: " + promptLength + " characters");
//                 }
//             }
//
//             // Console 日志：输出请求信息（仅非验证请求）
//             if (!"Validation Request".equals(logPrefix)) {
//                 Project project = getCurrentProject();
//                 if (project != null) {
//                     // 输出供应商和模型
//                     JavaDocConsoleView.printWithTimestamp(project, String.format("供应商: %s 模型: %s [%s性能模式] [%s代码压缩]",
//                                                                                  getProviderName(),
//                                                                                  providerConfig.modelName,
//                                                                                  settings.performanceMode ? "启用" : "关闭",
//                                                                                  settings.enableCodeCompression ? "启用" : "关闭"));
//                     JavaDocConsoleView.print(project, "");
//
//                     // 输出 System Prompt
//                     String systemPrompt = getSystemPrompt();
//                     JavaDocConsoleView.print(project, "=== System Prompt ===");
//                     JavaDocConsoleView.print(project, systemPrompt);
//                     JavaDocConsoleView.print(project, "");
//
//                     // 输出 User Prompt（从 body 中提取）
//                     JsonArray messages = body.getAsJsonArray("messages");
//                     if (messages != null && messages.size() >= 2) {
//                         JsonObject userMessage = messages.get(1).getAsJsonObject();
//                         if (userMessage.has("content")) {
//                             String userPrompt = userMessage.get("content").getAsString();
//                             JavaDocConsoleView.print(project, "=== User Prompt ===");
//                             JavaDocConsoleView.print(project, userPrompt);
//                             JavaDocConsoleView.print(project, "");
//                         }
//                     }
//
//                     // 输出原始请求
//                     JavaDocConsoleView.print(project, "=== 原始请求 ===");
//                     JavaDocConsoleView.print(project, requestBody);
//                     JavaDocConsoleView.print(project, "");
//                 }
//             }
//
//             // 使用IDEA SDK的HttpRequests发送请求
//             String responseBody = HttpRequests.post(url, "application/json")
//                 .tuner(connection -> {
//                     // 设置超时
//                     connection.setConnectTimeout(settings.timeout); // 使用设置中的连接超时
//                     connection.setReadTimeout(settings.timeout * 2); // 读取超时是连接超时的2倍
//
//                     // 设置Authorization头（如果需要）
//                     connection.setRequestProperty("Authorization", "Bearer " + apiKey);
//                 })
//                 .connect(request -> {
//                     // 写入请求体
//                     request.write(requestBody);
//                     // 读取响应
//                     return request.readString();
//                 });
//
//             // 调试日志：记录响应信息
//             if (settings.verboseLogging) {
//                 LOG.trace("=== " + logPrefix.replace("Request", "Response") + " ===");
//                 LOG.trace("Response Body: " + truncateForLog(responseBody,
//                                                              "Validation".equals(logPrefix) ? 1000 : 2000));
//             }
//
//             // Console 日志：输出原始响应（仅非验证请求）
//             if (!"Validation Request".equals(logPrefix)) {
//                 Project project = getCurrentProject();
//                 if (project != null) {
//                     JavaDocConsoleView.print(project, "=== 原始响应 ===");
//                     JavaDocConsoleView.print(project, responseBody);
//                     JavaDocConsoleView.print(project, "");
//                 }
//             }
//
//             if (!responseBody.trim().isEmpty()) {
//                 String result = responseParser.parse(responseBody);
//
//                 if (settings.verboseLogging) {
//                     LOG.trace("Parsed Result Length: " + result.length() + " characters");
//                     LOG.trace("Parsed Result:\n" + truncateForLog(result,
//                                                                   "Validation".equals(logPrefix) ? 200 : 1000));
//                 }
//                 return result;
//             }
//
//             throw new AIServiceException("Invalid response from AI service",
//                                          AIServiceException.ErrorCode.INVALID_RESPONSE);
//
//         } catch (IOException e) {
//             LOG.info("Network Error during " + logPrefix.toLowerCase() + ": " + e.getMessage());
//             throw new AIServiceException("Network error: " + e.getMessage(),
//                                          AIServiceException.ErrorCode.NETWORK_ERROR, e);
//         } catch (AIServiceException e) {
//             // 重新抛出AIServiceException
//             throw e;
//         } catch (Exception e) {
//             LOG.info("Unexpected error during " + logPrefix.toLowerCase(), e);
//             throw new AIServiceException("Unexpected error: " + e.getMessage(),
//                                          AIServiceException.ErrorCode.UNKNOWN_ERROR, e);
//         }
//     }
//
//     /**
//      * 截断长文本用于日志输出
//      *
//      * <p>为了避免日志文件过大和提高可读性，
//      * 对过长的文本进行截断处理。
//      * 主要用于记录请求和响应内容。
//      *
//      * <p>处理逻辑：
//      * <ul>
//      *   <li>如果文本长度小于等于最大长度，直接返回</li>
//      *   <li>如果文本长度大于最大长度，截断并添加提示信息</li>
//      *   <li>处理 null 值，返回 "null" 字符串</li>
//      * </ul>
//      *
//      * @param text      待处理的文本
//      * @param maxLength 最大长度限制
//      * @return 处理后的文本
//      */
//     private String truncateForLog(String text, int maxLength) {
//         if (text == null) {
//             return "null";
//         }
//         if (text.length() <= maxLength) {
//             return text;
//         }
//         return text.substring(0, maxLength) + "... (truncated, total length: " + text.length() + ")";
//     }
//
//
//     /**
//      * 构建请求体
//      *
//      * <p>根据 OpenAI API 规范构建 JSON 格式的请求体。
//      * 包含模型名称、消息内容、温度参数等配置。
//      *
//      * <p>请求体结构：
//      * <pre>
//      * {
//      *   "model": "模型名称",
//      *   "messages": [
//      *     {
//      *       "role": "system",
//      *       "content": "系统角色设定"
//      *     },
//      *     {
//      *       "role": "user",
//      *       "content": "用户请求内容"
//      *     }
//      *   ],
//      *   "temperature": 0.1,
//      *   "max_tokens": 1000
//      * }
//      * </pre>
//      *
//      * @param prompt 提示词内容
//      * @return 构建好的 JSON 请求体
//      */
//     public JsonObject buildRequestBody(String prompt) {
//
//         // 创建 system 消息
//         JsonObject systemMessage = new JsonObject();
//         systemMessage.addProperty("role", "system");
//         systemMessage.addProperty("content", getSystemPrompt());
//
//         // 创建 user 消息
//         JsonObject userMessage = new JsonObject();
//         userMessage.addProperty("role", "user");
//         userMessage.addProperty("content", prompt);
//
//         JsonObject body = new JsonObject();
//         body.addProperty("model", providerConfig.modelName);
//         // ollama 的参数
//         body.addProperty("think", false);
//         // openapi 兼容的参数
//         body.addProperty("enable_thinking", false);
//         // 关闭流式输出
//         body.addProperty("stream", false);
//
//         // 创建消息数组
//         JsonArray messagesArray = new JsonArray();
//         messagesArray.add(systemMessage);
//         messagesArray.add(userMessage);
//         body.add("messages", messagesArray);
//
//         body.addProperty("temperature", settings.temperature);
//         body.addProperty("max_tokens", settings.maxTokens);
//         body.addProperty("top_p", settings.topP);
//         body.addProperty("top_k", settings.topK);
//         body.addProperty("presence_penalty", settings.presencePenalty);
//
//         return body;
//     }
//
//     /**
//      * 获取系统提示词
//      *
//      * <p>返回用于设定 AI 角色和行为准则的系统提示词。
//      * 这个提示词会作为 system 消息发送给 AI 服务，
//      * 用于建立 AI 的基本角色和响应风格。
//      *
//      * <p>系统提示词的作用：
//      * <ul>
//      *   <li>设定 AI 的专业角色（Java 开发工程师）</li>
//      *   <li>建立响应格式要求（中文 JavaDoc）</li>
//      *   <li>定义输出规范（只返回注释，不返回代码）</li>
//      *   <li>确保一致性和专业性</li>
//      * </ul>
//      *
//      * <p>提示词来源：
//      * <ul>
//      *   <li>优先使用用户自定义的系统提示词</li>
//      *   <li>如果用户没有配置或配置为空，使用默认模板</li>
//      *   <li>确保系统提示词始终有效</li>
//      * </ul>
//      *
//      * @return 系统提示词内容
//      */
//     public String getSystemPrompt() {
//         String userSystemPrompt = settings.systemPromptTemplate;
//
//         // 如果用户没有配置或配置为空，使用默认模板
//         if (userSystemPrompt == null || userSystemPrompt.trim().isEmpty()) {
//             return SettingsState.getDefaultSystemPromptTemplate();
//         }
//
//         return userSystemPrompt;
//     }
//
//     /**
//      * 解析 AI 响应
//      *
//      * <p>解析 AI 服务返回的 JSON 响应，
//      * 提取生成的文本内容。
//      * 根据 OpenAI API 响应格式进行解析。
//      *
//      * <p>响应格式：
//      * <pre>
//      * {
//      *   "choices": [
//      *     {
//      *       "message": {
//      *         "content": "生成的文本内容"
//      *       }
//      *     }
//      *   ]
//      * }
//      * </pre>
//      *
//      * <p>特殊处理：
//      * <ul>
//      *   <li>过滤掉思考数据：移除 <think>...</think> 包裹的内容</li>
//      *   <li>只保留 </think> 之后的实际内容</li>
//      *   <li>确保返回的内容是纯净的 JavaDoc 注释</li>
//      * </ul>
//      *
//      * <p>异常处理：
//      * <ul>
//      *   <li>JSON 解析失败：记录错误日志并抛出 INVALID_RESPONSE 异常</li>
//      *   <li>字段缺失：记录错误日志并抛出 INVALID_RESPONSE 异常</li>
//      * </ul>
//      *
//      * @param responseBody 原始响应体字符串
//      * @return 解析出的文本内容，已过滤思考数据
//      * @throws AIServiceException 当解析失败时抛出 INVALID_RESPONSE 类型异常
//      */
//     protected String parseResponse(String responseBody) throws AIServiceException {
//         try {
//             JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
//             String content = json.getAsJsonArray("choices")
//                 .get(0).getAsJsonObject()
//                 .getAsJsonObject("message")
//                 .get("content").getAsString()
//                 .trim();
//
//             // 解析并输出 usage 信息
//             Project project = getCurrentProject();
//             if (project != null && json.has("usage")) {
//                 JsonObject usage = json.getAsJsonObject("usage");
//                 int promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").getAsInt() : 0;
//                 int completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").getAsInt() : 0;
//                 int totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").getAsInt() : 0;
//                 JavaDocConsoleView.print(project, "=== Token 使用统计 ===");
//
//                 JavaDocConsoleView.print(project, String.format("供应商: %s 模型: %s | Token 使用: 请求 %d | 响应 %d | 总计 %d",
//                                                                 getProviderName(),
//                                                                 providerConfig.modelName,
//                                                                 promptTokens, completionTokens, totalTokens));
//                 JavaDocConsoleView.print(project, "");
//             }
//
//             // 过滤思考数据，只保留实际内容
//
//             return filterThinkingContent(content);
//         } catch (Exception e) {
//             LOG.info("Failed to parse AI response: " + responseBody, e);
//             throw new AIServiceException("Failed to parse response",
//                                          AIServiceException.ErrorCode.INVALID_RESPONSE, e);
//         }
//     }
//
//     /**
//      * 解析验证响应
//      *
//      * <p>专门用于配置验证的响应解析方法，
//      * 通过检查 usage.completion_tokens 来验证服务是否正常工作。
//      * 只要有 token 消耗就说明服务正常响应。
//      *
//      * <p>验证策略：
//      * <ul>
//      *   <li>检查 usage.completion_tokens 是否存在且大于 0</li>
//      *   <li>不关心具体的响应内容，只关心是否有 token 消耗</li>
//      *   <li>返回固定的成功标识</li>
//      * </ul>
//      *
//      * <p>响应格式：
//      * <pre>
//      * {
//      *   "usage": {
//      *     "completion_tokens": 5,
//      *     "prompt_tokens": 10,
//      *     "total_tokens": 15
//      *   },
//      *   "choices": [...]
//      * }
//      * </pre>
//      *
//      * @param responseBody 原始响应体字符串
//      * @return 固定的成功标识 "OK"
//      * @throws AIServiceException 当解析失败或没有 token 消耗时抛出 INVALID_RESPONSE 类型异常
//      */
//     protected String parseValidationResponse(String responseBody) throws AIServiceException {
//         try {
//             JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
//
//             // 检查 usage.completion_tokens 是否存在且大于 0
//             if (json.has("usage")) {
//                 JsonObject usage = json.getAsJsonObject("usage");
//                 if (usage.has("completion_tokens")) {
//                     int completionTokens = usage.get("completion_tokens").getAsInt();
//                     if (completionTokens > 0) {
//                         if (settings.verboseLogging) {
//                             LOG.debug("Validation successful: completion_tokens=" + completionTokens);
//                         }
//                         return "OK";
//                     }
//                 }
//             }
//
//             // 如果没有 usage 信息，尝试解析 choices 作为备选方案
//             if (json.has("choices") && !json.getAsJsonArray("choices").isEmpty()) {
//                 if (settings.verboseLogging) {
//                     LOG.debug("Validation successful: response has choices");
//                 }
//                 return "OK";
//             }
//
//             throw new AIServiceException("No completion tokens found in response",
//                                          AIServiceException.ErrorCode.INVALID_RESPONSE);
//
//         } catch (Exception e) {
//             LOG.info("Failed to parse validation response: " + responseBody, e);
//             throw new AIServiceException("Failed to parse validation response",
//                                          AIServiceException.ErrorCode.INVALID_RESPONSE, e);
//         }
//     }
//
//     /**
//      * 过滤思考内容
//      *
//      * <p>移除 AI 模型返回的思考过程数据，
//      * 只保留实际的 JavaDoc 注释内容。
//      *
//      * <p>处理逻辑：
//      * <ul>
//      *   <li>查找 </think> 标签</li>
//      *   <li>如果找到，只保留该标签之后的内容</li>
//      *   <li>如果没有找到，返回原始内容</li>
//      *   <li>确保返回的内容是纯净的</li>
//      * </ul>
//      *
//      * <p>示例：
//      * <ul>
//      *   <li>输入包含思考标签的内容</li>
//      *   <li>输出过滤后的纯净 JavaDoc 注释</li>
//      * </ul>
//      *
//      * @param content 原始响应内容
//      * @return 过滤后的纯净内容
//      */
//     private String filterThinkingContent(String content) {
//         if (content == null || content.isEmpty()) {
//             return content;
//         }
//
//         // 查找 </think> 标签
//         String endTag = "</think>";
//         int endTagIndex = content.indexOf(endTag);
//
//         if (endTagIndex != -1) {
//             // 找到结束标签，只保留该标签之后的内容
//             String filteredContent = content.substring(endTagIndex + endTag.length()).trim();
//
//             if (settings.verboseLogging) {
//                 LOG.debug("Filtered thinking content. Original length: " + content.length() +
//                           ", Filtered length: " + filteredContent.length());
//             }
//
//             return filteredContent;
//         }
//
//         // 某些时候会没有 </think> 标签, 那如果存在 think 内容就不替换注释, 否则出现错误
//         if (content.contains("<think>")) {
//             return "";
//         }
//
//         // 没有找到思考标签，返回原始内容
//         return content;
//     }
//
//     /**
//      * 构建用户提示词
//      *
//      * <p>根据代码内容、文档类型和编程语言构建用户提示词。
//      * 这个提示词会作为 user 消息发送给 AI 服务，
//      * 包含具体的任务要求和代码内容。
//      *
//      * <p>构建流程：
//      * <ol>
//      *   <li>根据文档类型加载相应的 Prompt 模板</li>
//      *   <li>使用 String.format 将代码内容插入模板</li>
//      *   <li>返回完整的用户提示词</li>
//      * </ol>
//      *
//      * @param code     代码内容
//      * @param type     文档类型
//      * @param language 编程语言
//      * @return 构建好的用户提示词
//      * @see #loadPromptTemplate(DocumentationTask.TaskType, String)
//      */
//     protected String buildPrompt(String code, DocumentationTask.TaskType type, String language) {
//         String template = loadPromptTemplate(type, language);
//         return String.format(template, code);
//     }
//
//     /**
//      * 加载提示词模板
//      *
//      * <p>从设置中读取用户配置的 Prompt 模板。
//      * 用户可以在设置面板中自定义 Prompt，以满足不同的需求。
//      * 如果用户没有配置或配置为空，则使用 SettingsState 中定义的默认模板。
//      *
//      * @param type     文档类型（类、方法、字段、测试方法等）
//      * @param language 编程语言
//      * @return Prompt 模板字符串，包含 %s 占位符用于代码插入
//      */
//     protected String loadPromptTemplate(DocumentationTask.TaskType type, String language) {
//         // 根据文档类型选择相应的模板
//         String template;
//         String defaultTemplate;
//
//         switch (type) {
//             case CLASS, INTERFACE, ENUM -> {
//                 template = settings.classPromptTemplate;
//                 defaultTemplate = SettingsState.getDefaultClassPromptTemplate();
//             }
//             case FIELD -> {
//                 template = settings.fieldPromptTemplate;
//                 defaultTemplate = SettingsState.getDefaultFieldPromptTemplate();
//             }
//             case TEST_METHOD -> {
//                 template = settings.testPromptTemplate;
//                 defaultTemplate = SettingsState.getDefaultTestPromptTemplate();
//             }
//             default -> {
//                 template = settings.methodPromptTemplate;
//                 defaultTemplate = SettingsState.getDefaultMethodPromptTemplate();
//             }
//         }
//
//         // 如果用户没有配置或配置为空，使用默认模板
//         // 使用 isBlank() 检查：null、空字符串、只包含空白字符（包括空白行）的情况
//         if (template == null || template.isBlank()) {
//             return defaultTemplate;
//         }
//
//         return template;
//     }
//
//
//     /**
//      * 验证配置是否有效
//      * <p>
//      * 该方法用于验证当前配置是否正确，包括网络连接、服务响应等。如果验证成功，返回成功结果；如果失败，返回失败结果并附带错误信息。
//      *
//      * @return 验证结果，包含成功或失败的状态及对应信息
//      */
//     @NotNull
//     @Override
//     public ValidationResult validateConfiguration(String apiKey) {
//         try {
//
//             if (settings.verboseLogging) {
//                 LOG.debug("Starting configuration validation for provider: " + getProviderId());
//                 LOG.debug("Base URL: " + providerConfig.baseUrl);
//                 LOG.debug("Model: " + providerConfig.modelName);
//             }
//
//             String response = sendValidationRequest(apiKey);
//
//             if (response != null && !response.isEmpty()) {
//                 String successMessage = "连接成功！提供商: " + getProviderName() + ", 模型: " + providerConfig.modelName;
//                 if (settings.verboseLogging) {
//                     LOG.debug("Configuration validation successful");
//                 }
//                 return ValidationResult.success(successMessage);
//             } else {
//                 String errorMessage = "连接失败：服务返回空响应";
//                 LOG.warn(errorMessage);
//                 return ValidationResult.failure(errorMessage, "请检查网络连接和服务状态");
//             }
//         } catch (AIServiceException e) {
//             String errorMessage = "配置验证失败";
//             String errorDetails = AIServiceException.build(e);
//             LOG.warn("Configuration validation failed: " + errorDetails, e);
//             return ValidationResult.failure(errorMessage, errorDetails, e);
//         } catch (Exception e) {
//             String errorMessage = "配置验证异常";
//             String errorDetails = e.getMessage();
//             if (errorDetails == null || errorDetails.isEmpty()) {
//                 errorDetails = e.getClass().getSimpleName();
//             }
//             LOG.warn("Configuration validation error: " + errorDetails, e);
//             return ValidationResult.failure(errorMessage, errorDetails, e);
//         }
//     }
//
//
//     /**
//      * 构建验证请求体
//      *
//      * <p>为配置验证构建极简的 ping-pong 式请求体，
//      * 使用最小化的提示词和参数设置，
//      * 以减少 token 消耗并提高验证速度。
//      *
//      * <p>请求体特点：
//      * <ul>
//      *   <li>无系统提示词：减少不必要的角色设定</li>
//      *   <li>极简用户提示词：只发送 "ping" 进行连接测试</li>
//      *   <li>最小 max_tokens：限制响应为单个单词</li>
//      *   <li>最低 temperature：确保响应一致性</li>
//      * </ul>
//      *
//      * <p>请求体结构：
//      * <pre>
//      * {
//      *   "model": "模型名称",
//      *   "messages": [
//      *     {
//      *       "role": "user",
//      *       "content": "ping"
//      *     }
//      *   ],
//      *   "temperature": 0.0,
//      *   "max_tokens": 5
//      * }
//      * </pre>
//      *
//      * @return 构建好的验证请求体
//      */
//     protected JsonObject buildValidationRequestBody() {
//         // 创建 system 消息
//         JsonObject systemMessage = new JsonObject();
//         systemMessage.addProperty("role", "system");
//         systemMessage.addProperty("content", "i say ping, you say pong");
//
//         // 创建 user 消息
//         JsonObject userMessage = new JsonObject();
//         userMessage.addProperty("role", "user");
//         userMessage.addProperty("content", "ping");
//
//         JsonObject body = new JsonObject();
//         body.addProperty("model", providerConfig.modelName);
//         // ollama 的参数
//         body.addProperty("think", false);
//         // openapi 兼容的参数
//         body.addProperty("enable_thinking", false);
//         // 关闭流式输出
//         body.addProperty("stream", false);
//
//         // 创建消息数组
//         JsonArray messagesArray = new JsonArray();
//         messagesArray.add(systemMessage);
//         messagesArray.add(userMessage);
//         body.add("messages", messagesArray);
//
//         body.addProperty("temperature", 0.1);
//         body.addProperty("max_tokens", 32);
//         body.addProperty("top_p", 0.9);
//         return body;
//     }
//
//     /**
//      * 获取可用的模型列表
//      *
//      * <p>通过调用提供商的 API 接口获取当前可用的模型列表。
//      * 默认实现调用 /models 端点获取模型列表。
//      * 子类可以重写此方法以支持特定的 API 格式。
//      *
//      * <p>实现策略：
//      * <ul>
//      *   <li>发送 GET 请求到 {baseUrl}/models</li>
//      *   <li>解析 JSON 响应获取模型列表</li>
//      *   <li>处理各种异常情况</li>
//      *   <li>失败时返回空列表</li>
//      * </ul>
//      *
//      * <p>错误处理：
//      * <ul>
//      *   <li>网络异常：记录日志，返回空列表</li>
//      *   <li>认证失败：记录日志，返回空列表</li>
//      *   <li>响应格式错误：记录日志，返回空列表</li>
//      *   <li>超时：记录日志，返回空列表</li>
//      * </ul>
//      *
//      * @return 可用模型名称列表，如果获取失败返回空列表
//      */
//     @NotNull
//     @Override
//     public List<String> getAvailableModels(String apiKey) {
//         try {
//
//             if (settings.verboseLogging) {
//                 LOG.debug("Fetching available models from provider: " + getProviderId());
//                 LOG.debug("Base URL: " + providerConfig.baseUrl);
//             }
//
//             // 检查API Key配置
//             if (requiresApiKey()) {
//                 if (apiKey == null || apiKey.trim().isEmpty()) {
//                     LOG.warn("API Key is required but not configured for provider: " + getProviderId());
//                     return new ArrayList<>();
//                 }
//             }
//
//             String url = providerConfig.baseUrl + "/models";
//
//             if (settings.verboseLogging) {
//                 LOG.debug("Requesting models from: " + url);
//             }
//
//             // 使用IDEA SDK的HttpRequests发送GET请求
//             // 读取响应
//             String responseBody = HttpRequests.request(url)
//                 .tuner(connection -> {
//                     // 设置超时
//                     connection.setConnectTimeout(settings.timeout); // 使用设置中的连接超时
//                     connection.setReadTimeout(settings.timeout * 2); // 读取超时是连接超时的2倍
//
//                     // 设置Authorization头（如果需要）
//                     connection.setRequestProperty("Authorization", "Bearer " + apiKey);
//                 })
//                 .connect(HttpRequests.Request::readString);
//
//             if (!responseBody.trim().isEmpty()) {
//                 List<String> models = parseModelsResponse(responseBody);
//
//                 if (settings.verboseLogging) {
//                     LOG.debug("Successfully fetched " + models.size() + " models");
//                 }
//
//                 return models;
//             }
//
//             LOG.warn("Failed to fetch models: Empty response");
//             return new ArrayList<>();
//
//         } catch (IOException e) {
//             LOG.warn("Network error while fetching models: " + e.getMessage());
//             return new ArrayList<>();
//         } catch (Exception e) {
//             LOG.warn("Unexpected error while fetching models", e);
//             return new ArrayList<>();
//         }
//     }
//
//     /**
//      * 解析模型列表响应
//      *
//      * <p>解析 AI 服务返回的模型列表 JSON 响应。
//      * 默认实现支持标准的 OpenAI 兼容格式。
//      * 子类可以重写此方法以支持特定的响应格式。
//      *
//      * <p>标准响应格式：
//      * <pre>
//      * {
//      *   "data": [
//      *     {
//      *       "id": "model-name",
//      *       "object": "model",
//      *       "created": 1234567890,
//      *       "owned_by": "provider"
//      *     }
//      *   ],
//      *   "object": "list"
//      * }
//      * </pre>
//      *
//      * <p>解析逻辑：
//      * <ul>
//      *   <li>提取 data 数组</li>
//      *   <li>遍历每个模型对象</li>
//      *   <li>提取 id 字段作为模型名称</li>
//      *   <li>过滤掉无效的模型名称</li>
//      * </ul>
//      *
//      * @param responseBody JSON 响应体
//      * @return 模型名称列表
//      */
//     protected List<String> parseModelsResponse(String responseBody) {
//         List<String> models = new ArrayList<>();
//
//         try {
//             JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
//             if (json.has("data") && json.get("data").isJsonArray()) {
//                 JsonArray dataArray = json.getAsJsonArray("data");
//                 for (JsonElement element : dataArray) {
//                     JsonObject modelObj = element.getAsJsonObject();
//                     if (modelObj.has("id")) {
//                         String modelId = modelObj.get("id").getAsString();
//                         if (modelId != null && !modelId.trim().isEmpty()) {
//                             models.add(modelId.trim());
//                         }
//                     }
//                 }
//             }
//         } catch (Exception e) {
//             LOG.warn("Failed to parse models response: " + responseBody, e);
//         }
//
//         return models;
//     }
// }
//
