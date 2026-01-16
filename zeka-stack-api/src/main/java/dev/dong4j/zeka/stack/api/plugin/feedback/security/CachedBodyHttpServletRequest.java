package dev.dong4j.zeka.stack.api.plugin.feedback.security;

import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * 缓存请求体的 HTTP 请求包装器类
 * <p> 用于在 Servlet 请求中缓存原始请求体内容, 以便后续多次读取. 该类通过包装 {@link HttpServletRequest} 实现, 将请求输入流的内容缓存到内存中, 从而支持多次调用 {@link #getInputStream()} 或
 * {@link #getReader()} 方法.</p>
 * <p> 该类主要用于需要多次读取请求体的场景, 例如日志记录, 请求参数校验, 请求体签名验证等, 避免因输入流只能读取一次而导致的数据丢失.</p>
 * <p> 内部实现了一个嵌套类 {@link CachedBodyServletInputStream}, 用于封装缓存的字节数组, 并实现 {@link ServletInputStream} 接口, 支持流式读取.</p>
 * <p> 注意: 该类不负责请求处理, 仅作为基础设施层的辅助工具, 用于封装请求体的可重读性.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    /** 缓存的请求体字节数组, 用于支持多次读取 HttpServletRequest 输入流 */
    private final byte[] cachedBody;

    /**
     * 构造函数, 用于创建缓存请求体的 HttpServletRequest 包装类
     * <p>
     * 在初始化时, 从原始请求中读取输入流并缓存为字节数组, 以便后续多次读取.
     * 此设计解决了 HttpServletRequest 输入流只能读取一次的问题.
     *
     * @param request HTTP 请求对象
     * @throws IOException 当读取请求输入流时发生 I/O 错误
     */
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        // 读取并缓存请求体
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    /**
     * 获取缓存的请求体字节数组
     * <p> 该方法用于获取在构造函数中缓存的 HTTP 请求体数据, 以便在后续多次读取请求内容时使用.
     *
     * @return 缓存的请求体字节数组, 用于重复读取请求内容
     */
    public byte[] getCachedBody() {
        return cachedBody;
    }

    /**
     * 获取缓存的请求体输入流
     * <p> 重写父类方法, 返回一个封装了缓存字节数组的 {@code ServletInputStream} 实例, 允许重复读取请求体.
     *
     * @return 缓存的请求体输入流, 用于多次读取请求数据
     * @throws IOException 当输入流操作失败时抛出
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CachedBodyServletInputStream(cachedBody);
    }

    /**
     * 获取缓存的请求体作为字符输入流
     * <p> 通过缓存的字节数组创建 ByteArrayInputStream, 并包装为 BufferedReader, 以便多次读取请求体内容.
     *
     * @return 包装后的 BufferedReader, 用于读取缓存的请求体内容
     * @throws IOException 当创建输入流时发生 I/O 错误
     */
    @Override
    public BufferedReader getReader() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        return new BufferedReader(new InputStreamReader(byteArrayInputStream));
    }

    /**
     * 缓存请求体的 Servlet 输入流实现类
     * <p> 用于在 Servlet 请求处理过程中缓存原始请求体内容, 以便后续多次读取. 该类不负责请求处理本身, 仅作为基础设施层的辅助组件, 避免业务逻辑与底层 I/O 细节耦合.
     * <p> 通过包装 ByteArrayInputStream 实现对原始字节数组的只读访问, 支持标准 ServletInputStream 接口方法, 但不支持异步读取 (抛出 UnsupportedOperationException).
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.16
     * @since 1.0.0
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {
        /** 缓存的字节数据输入流, 用于重放请求体内容 */
        private final ByteArrayInputStream buffer;

        /**
         * 初始化缓存的 ServletInputStream 实例
         * <p> 使用指定字节数组内容创建一个可重复读取的 ServletInputStream 实现, 该实现内部使用 ByteArrayInputStream 进行数据缓冲.
         *
         * @param contents 用于初始化输入流的字节数组
         */
        public CachedBodyServletInputStream(byte[] contents) {
            this.buffer = new ByteArrayInputStream(contents);
        }

        /**
         * 从缓存字节流中读取一个字节
         * <p> 该方法从内部的 {@code ByteArrayInputStream} 中读取下一个字节, 若无更多字节则返回 - 1
         *
         * @return 下一个字节的值, 若已无数据则返回 - 1
         * @throws IOException 当底层输入流发生读取错误时抛出
         */
        @Override
        public int read() throws IOException {
            return buffer.read();
        }

        /**
         * 判断缓存输入流是否已读完
         * <p> 当缓冲区中无剩余字节时, 表示数据已全部读取完毕
         *
         * @return 如果缓冲区中无剩余字节则返回 true, 否则返回 false
         */
        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        /**
         * 判断当前输入流是否就绪
         * <p> 该方法始终返回 true, 表示输入流当前可读
         *
         * @return 始终返回 true, 表示输入流就绪
         */
        @Override
        public boolean isReady() {
            return true;
        }

        /**
         * 设置读取监听器
         * <p> 当前实现不支持读取监听器, 调用此方法将抛出不支持操作异常
         *
         * @param listener 读取监听器, 当前实现不支持
         */
        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException("ReadListener is not supported");
        }
    }
}

