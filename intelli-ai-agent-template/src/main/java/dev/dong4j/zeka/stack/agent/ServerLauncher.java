package dev.dong4j.zeka.stack.agent;

import dev.dong4j.zeka.stack.agent.api.OpenAiApiServer;
import dev.dong4j.zeka.stack.agent.client.YourAIServiceClient;

/**
 * 服务启动入口
 * <p>
 * 这是 JAR 包的主启动类，通过 {@code java -jar intelli-ai-agent-template.jar} 启动服务。
 * 服务启动后会监听 8765 端口，提供 OpenAI 兼容的 API 接口。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.0.0
 */
public class ServerLauncher {
    /**
     * 默认端口号（固定为 8765）
     */
    private static final int DEFAULT_PORT = 8765;

    /**
     * 主方法
     *
     * @param args 命令行参数，支持 {@code --port=xxx} 参数（但 Engine 会固定使用 8765）
     */
    public static void main(String[] args) {
        // 解析端口（可选，默认 8765）
        int port = resolvePort(args);

        System.out.println("Starting AI Agent Service...");
        System.out.println("Port: " + port);

        // 1. 初始化原始协议客户端（这里以 WebSocket 为例）
        YourAIServiceClient client = new YourAIServiceClient();
        if (!client.connect()) {
            System.err.println("Failed to connect to AI service");
            System.exit(1);
        }

        // 2. 创建并启动 OpenAI API 服务器
        OpenAiApiServer server = new OpenAiApiServer(client, port);
        try {
            server.start();
            System.out.println("OpenAI API Server started on http://127.0.0.1:" + port);
            System.out.println("Health check: http://127.0.0.1:" + port + "/health");
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }

        // 3. 注册关闭钩子，确保服务关闭时正确清理资源
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down...");
            server.stop();
            client.disconnect();
            System.out.println("Service stopped");
        }));

        // 4. 保持主线程运行
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Service interrupted");
        }
    }

    /**
     * 解析端口号
     * <p>
     * 支持从命令行参数 {@code --port=xxx} 或环境变量 {@code AGENT_PORT} 读取端口号，
     * 但默认使用 8765（Engine 会固定使用此端口）。
     *
     * @param args 命令行参数
     * @return 端口号
     */
    private static int resolvePort(String[] args) {
        // 从命令行参数解析
        for (String arg : args) {
            if (arg != null && arg.startsWith("--port=")) {
                try {
                    int port = Integer.parseInt(arg.substring("--port=".length()));
                    if (port > 0 && port < 65536) {
                        return port;
                    }
                } catch (NumberFormatException ignored) {
                    // 忽略无效的端口号
                }
            }
        }

        // 从环境变量读取
        String envPort = System.getenv("AGENT_PORT");
        if (envPort != null) {
            try {
                int port = Integer.parseInt(envPort);
                if (port > 0 && port < 65536) {
                    return port;
                }
            } catch (NumberFormatException ignored) {
                // 忽略无效的端口号
            }
        }

        // 默认端口
        return DEFAULT_PORT;
    }
}

