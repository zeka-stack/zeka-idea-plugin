package dev.dong4j.zeka.stack.idea.plugin.nacos.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * NacosClientUtils 单元测试
 *
 * @author dong4j
 * @since 1.0.0
 */
class NacosClientUtilsTest {

    @BeforeEach
    void setUp() {
        // 清空客户端缓存
        NacosClientUtils.clearAllClients();
    }

    @Test
    @Disabled("需要实际的 Nacos 服务器进行测试")
    void testGetClient() {
        String serverAddr = "http://localhost:8848";
        String username = "nacos";
        String password = "nacos";

        NacosClient client1 = NacosClientUtils.getClient(serverAddr, username, password);
        NacosClient client2 = NacosClientUtils.getClient(serverAddr, username, password);

        assertNotNull(client1);
        assertSame(client1, client2);
    }

    @Test
    void testGetClientWithInvalidParameters() {
        // 测试空服务器地址
        assertNull(NacosClientUtils.getClient("", "user", "pass"));

        // 测试空用户名
        assertNull(NacosClientUtils.getClient("http://localhost:8848", "", "pass"));

        // 测试 null 参数
        assertNull(NacosClientUtils.getClient(null, "user", "pass"));
    }

    @Test
    @Disabled("需要实际的 Nacos 服务器进行测试")
    void testRemoveClient() {
        String serverAddr = "http://localhost:8848";
        String username = "nacos";
        String password = "nacos";

        NacosClient client = NacosClientUtils.getClient(serverAddr, username, password);
        assertNotNull(client);

        NacosClientUtils.removeClient(serverAddr, username);
        NacosClient newClient = NacosClientUtils.getClient(serverAddr, username, password);
        assertNotNull(newClient);
        assertNotSame(client, newClient);
    }

    @Test
    void testClearAllClients() {
        // 添加一些客户端到缓存
        NacosClientUtils.getClient("http://server1:8848", "user1", "pass1");
        NacosClientUtils.getClient("http://server2:8848", "user2", "pass2");

        // 清空缓存
        NacosClientUtils.clearAllClients();

        // 由于我们无法直接访问缓存，这里只是确保方法执行没有异常
        assertDoesNotThrow(() -> NacosClientUtils.clearAllClients());
    }
}