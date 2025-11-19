package dev.dong4j.zeka.stack.idea.plugin.nacos.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CacheUtils 单元测试
 *
 * @author dong4j
 * @since 1.0.0
 */
class CacheUtilsTest {

    @BeforeEach
    void setUp() {
        CacheUtils.clear();
    }

    @Test
    void testPutAndGet() {
        String key = "testKey";
        String value = "testValue";

        CacheUtils.put(key, value);
        String result = CacheUtils.get(key);

        assertEquals(value, result);
    }

    @Test
    void testGetNonExistentKey() {
        String result = CacheUtils.get("nonExistentKey");
        assertNull(result);
    }

    @Test
    void testContains() {
        String key = "testKey";
        String value = "testValue";

        assertFalse(CacheUtils.contains(key));

        CacheUtils.put(key, value);
        assertTrue(CacheUtils.contains(key));
    }

    @Test
    void testRemove() {
        String key = "testKey";
        String value = "testValue";

        CacheUtils.put(key, value);
        assertTrue(CacheUtils.contains(key));

        CacheUtils.remove(key);
        assertFalse(CacheUtils.contains(key));
        assertNull(CacheUtils.get(key));
    }

    @Test
    void testClear() {
        CacheUtils.put("key1", "value1");
        CacheUtils.put("key2", "value2");
        CacheUtils.put("key3", "value3");

        assertEquals(3, CacheUtils.size());

        CacheUtils.clear();
        assertEquals(0, CacheUtils.size());
        assertNull(CacheUtils.get("key1"));
        assertNull(CacheUtils.get("key2"));
        assertNull(CacheUtils.get("key3"));
    }

    @Test
    void testSize() {
        assertEquals(0, CacheUtils.size());

        CacheUtils.put("key1", "value1");
        assertEquals(1, CacheUtils.size());

        CacheUtils.put("key2", "value2");
        assertEquals(2, CacheUtils.size());

        CacheUtils.remove("key1");
        assertEquals(1, CacheUtils.size());
    }

    @Test
    void testExpiration() throws InterruptedException {
        String key = "expiringKey";
        String value = "expiringValue";

        // 存储数据，设置 100ms 过期时间
        CacheUtils.put(key, value, 100);

        assertTrue(CacheUtils.contains(key));
        assertEquals(value, CacheUtils.get(key));

        // 等待过期
        Thread.sleep(150);

        assertFalse(CacheUtils.contains(key));
        assertNull(CacheUtils.get(key));
    }

    @Test
    void testDefaultExpiration() throws InterruptedException {
        String key = "defaultExpireKey";
        String value = "defaultValue";

        // 使用默认过期时间（5分钟）
        CacheUtils.put(key, value);

        assertTrue(CacheUtils.contains(key));
        assertEquals(value, CacheUtils.get(key));
    }
}