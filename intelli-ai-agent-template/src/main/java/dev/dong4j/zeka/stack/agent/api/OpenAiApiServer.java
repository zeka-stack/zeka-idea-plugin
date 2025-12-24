package dev.dong4j.zeka.stack.agent.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;

import dev.dong4j.zeka.stack.agent.client.YourAIServiceClient;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenAI 兼容 API 服务器
 * <p>
 * 实现标准的 OpenAI API 接口，将请求转换为原始协议请求，并将响应转换为 OpenAI 格式。
 * <p>
 * 必须实现的端点：
 * <ul>
 *   <li>{@code GET /health} - 健康检查</li>
 *   <li>{@code GET /v1/models} - 模型列表</li>
 *   <li>{@code POST /v1/chat/completions} - 聊天完成（支持流式和非流式）</li>
 * </ul>
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class OpenAiApiServer {
    /**
     * 原始协议客户端
     */
    private final YourAIServiceClient client;
    /**
     * 服务端口（固定为 8765）
     */
    private final int port;
    /**
     * HTTP 服务器实例
     */
    private HttpServer httpServer;

    /**
     * 构造函数
     *
     * @param client 原始协议客户端
     * @param port   服务端口
     */
    public OpenAiApiServer(YourAIServiceClient client, int port) {
        this.client = client;
        this.port = port;
    }

    /**
     * 启动服务器
     *
     * @throws IOException 如果启动失败
     */
    public void start() throws IOException {
        if (httpServer != null) {
            return;
        }

        // 创建 HTTP 服务器，绑定到 127.0.0.1:8765
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

        // 注册端点
        httpServer.createContext("/health", this::handleHealth);
        httpServer.createContext("/v1/models", this::handleModels);
        httpServer.createContext("/v1/chat/completions", new ChatCompletionsHandler());

        // 设置线程池
        httpServer.setExecutor(Executors.newCachedThreadPool());

        // 启动服务器
        httpServer.start();

        log.info("OpenAI API Server started on http://127.0.0.1:{}", port);
    }

    /**
     * 停止服务器
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
            log.info("OpenAI API Server stopped");
        }
    }

    /**
     * 处理健康检查请求
     * <p>
     * 响应格式：{@code {"status":"ok"}}
     *
     * @param exchange HTTP 交换对象
     * @throws IOException 如果写入响应失败
     */
    private void handleHealth(HttpExchange exchange) throws IOException {
        String response = "{\"status\":\"ok\"}";
        writeJsonResponse(exchange, 200, response);
    }

    /**
     * 处理模型列表请求
     * <p>
     * 响应格式：{@code {"object":"list","data":[{"id":"model-id","object":"model","owned_by":"service-name"}]}}
     *
     * @param exchange HTTP 交换对象
     * @throws IOException 如果写入响应失败
     */
    private void handleModels(HttpExchange exchange) throws IOException {
        // 构建模型列表响应
        // 注意：这里使用示例模型 ID，实际应该从配置或原始服务获取
        String response = """
            {
              "object": "list",
              "data": [
                {
                  "id": "your-model-id",
                  "object": "model",
                  "owned_by": "your-service-name"
                }
              ]
            }
            """;
        writeJsonResponse(exchange, 200, response);
    }

    /**
     * 聊天完成请求处理器
     */
    private class ChatCompletionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 只支持 POST 方法
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeError(exchange, 405, "Method not allowed. Only POST is supported");
                return;
            }

            try {
                // 读取请求体
                String body = readRequestBody(exchange.getRequestBody());

                // 解析 JSON 请求（这里简化处理，实际应使用 JSON 库）
                // JSONObject request = JSON.parseObject(body);

                // 提取参数（示例，实际需要解析 JSON）
                String question = extractQuestion(body); // 简化示例
                boolean stream = body.contains("\"stream\":true");

                if (stream) {
                    handleStreamRequest(question, exchange);
                } else {
                    handleStandardRequest(question, exchange);
                }
            } catch (Exception e) {
                writeError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }

        /**
         * 处理标准（非流式）请求
         */
        private void handleStandardRequest(String question, HttpExchange exchange) throws IOException {
            try {
                // 调用原始协议客户端获取完整响应
                String answer = client.ask(question);

                // 构建 OpenAI 格式的响应
                String response = buildStandardResponse(answer);

                // 返回响应
                writeJsonResponse(exchange, 200, response);
            } catch (Exception e) {
                writeError(exchange, 500, "Failed to generate answer: " + e.getMessage());
            }
        }

        /**
         * 处理流式请求
         */
        private void handleStreamRequest(String question, HttpExchange exchange) throws IOException {
            // 设置 SSE 响应头
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=utf-8");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, 0);

            OutputStream os = exchange.getResponseBody();
            String requestId = "chatcmpl-" + UUID.randomUUID();
            long created = System.currentTimeMillis() / 1000;

            try {
                // 调用原始协议客户端的流式方法
                client.askStream(
                    question,
                    chunk -> {
                        // 每个 chunk 转换为 OpenAI SSE 格式
                        String sseChunk = buildSseChunk(requestId, "your-model-id", created, chunk, false);
                        writeSse(os, sseChunk);
                    },
                    () -> {
                        // 完成
                        String doneChunk = buildSseChunk(requestId, "your-model-id", created, "", true);
                        writeSse(os, doneChunk);
                        writeSse(os, "[DONE]");
                        try {
                            os.close();
                        } catch (IOException ignored) {
                        }
                    }
                                );
            } catch (Exception e) {
                writeSse(os, "{\"error\":\"" + e.getMessage() + "\"}");
                writeSse(os, "[DONE]");
                try {
                    os.close();
                } catch (IOException ignored) {
                }
            }
        }

        /**
         * 构建标准响应（JSON 格式）
         */
        private String buildStandardResponse(String answer) {
            // 简化示例，实际应使用 JSON 库构建
            return String.format("""
                                     {
                                       "id": "chatcmpl-%s",
                                       "object": "chat.completion",
                                       "created": %d,
                                       "model": "your-model-id",
                                       "choices": [
                                         {
                                           "index": 0,
                                           "message": {
                                             "role": "assistant",
                                             "content": "%s"
                                           },
                                           "finish_reason": "stop"
                                         }
                                       ]
                                     }
                                     """, UUID.randomUUID(), System.currentTimeMillis() / 1000, escapeJson(answer));
        }

        /**
         * 构建 SSE chunk
         */
        private String buildSseChunk(String requestId, String model, long created, String content, boolean finished) {
            // 简化示例，实际应使用 JSON 库构建
            if (finished) {
                return String.format("""
                                         {
                                           "id": "%s",
                                           "object": "chat.completion.chunk",
                                           "created": %d,
                                           "model": "%s",
                                           "choices": [
                                             {
                                               "index": 0,
                                               "delta": {},
                                               "finish_reason": "stop"
                                             }
                                           ]
                                         }
                                         """, requestId, created, model);
            } else {
                return String.format("""
                                         {
                                           "id": "%s",
                                           "object": "chat.completion.chunk",
                                           "created": %d,
                                           "model": "%s",
                                           "choices": [
                                             {
                                               "index": 0,
                                               "delta": {
                                                 "content": "%s"
                                               },
                                               "finish_reason": null
                                             }
                                           ]
                                         }
                                         """, requestId, created, model, escapeJson(content));
            }
        }

        /**
         * 写入 SSE 数据
         */
        private void writeSse(OutputStream os, String data) {
            try {
                os.write(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException e) {
                // 处理错误
                log.error("Failed to write SSE data", e);
            }
        }

        /**
         * 从请求体中提取问题（简化示例）
         */
        private String extractQuestion(String body) {
            // 简化示例，实际应使用 JSON 库解析
            // 这里假设从 messages 数组中提取最后一个 user 消息的 content
            // 实际实现应使用 JSON 解析库（如 fastjson、Jackson 等）
            return "Hello"; // 占位符
        }

        /**
         * JSON 转义（简化示例）
         */
        private String escapeJson(String str) {
            if (str == null) {
                return "";
            }
            return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        }
    }

    /**
     * 读取请求体
     */
    private String readRequestBody(InputStream is) throws IOException {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 写入 JSON 响应
     */
    private void writeJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * 写入错误响应
     */
    private void writeError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String errorJson = String.format("""
                                             {
                                               "error": {
                                                 "message": "%s",
                                                 "type": "invalid_request_error"
                                               }
                                             }
                                             """, message.replace("\"", "\\\""));
        writeJsonResponse(exchange, statusCode, errorJson);
    }
}

