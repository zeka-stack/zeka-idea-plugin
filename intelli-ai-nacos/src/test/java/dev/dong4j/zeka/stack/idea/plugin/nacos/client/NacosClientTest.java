package dev.dong4j.zeka.stack.idea.plugin.nacos.client;

import com.alibaba.nacos.api.exception.NacosException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Nacos 客户端测试类
 *
 * @author dong4j
 * @since 1.0.0
 */
@Disabled("需要实际的 Nacos 服务器进行测试")
class NacosClientTest {

    @Test
    void testGetInstance() throws NacosException {
        NacosClient client1 = NacosClient.getInstance("http://localhost:8848", "nacos", "nacos");
        NacosClient client2 = NacosClient.getInstance("http://localhost:8848", "nacos", "nacos");

        assertNotNull(client1);
        assertSame(client1, client2, "应该返回相同的实例");
    }

    @Test
    void testGetClientWithDifferentCredentials() throws NacosException {
        NacosClient client1 = NacosClient.getInstance("http://localhost:8848", "nacos", "nacos");
        NacosClient client2 = NacosClient.getInstance("http://localhost:8848", "user1", "password1");

        assertNotNull(client1);
        assertNotNull(client2);
        assertNotSame(client1, client2, "应该返回不同的实例");
    }

    @Test
    void testClientProperties() throws NacosException {
        String serverAddr = "http://localhost:8848";
        String username = "nacos";
        String password = "nacos";

        NacosClient client = NacosClient.getInstance(serverAddr, username, password);

        assertEquals(serverAddr, client.getServerAddr(), "服务器地址应该匹配");
        assertEquals(username, client.getUsername(), "用户名应该匹配");
    }
}