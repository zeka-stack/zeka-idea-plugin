package dev.dong4j.zeka.stack.feedback.security;

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
 * 缓存请求体的 HttpServletRequest 包装类
 * <p>
 * 由于 HttpServletRequest 的输入流只能读取一次，需要缓存请求体以便多次使用。
 * 这个类在第一次读取时缓存请求体，后续可以重复读取。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.12.23
 * @since 1.0.0
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] cachedBody;

    /**
     * 构造函数
     *
     * @param request HTTP 请求
     * @throws IOException IO 异常
     */
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        // 读取并缓存请求体
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    /**
     * 获取缓存的请求体
     *
     * @return 请求体字节数组
     */
    public byte[] getCachedBody() {
        return cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        return new BufferedReader(new InputStreamReader(byteArrayInputStream));
    }

    /**
     * 缓存的 ServletInputStream 实现
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream buffer;

        public CachedBodyServletInputStream(byte[] contents) {
            this.buffer = new ByteArrayInputStream(contents);
        }

        @Override
        public int read() throws IOException {
            return buffer.read();
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException("ReadListener is not supported");
        }
    }
}

