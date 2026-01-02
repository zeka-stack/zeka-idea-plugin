package dev.dong4j.zeka.stack.idea.plugin.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import dev.dong4j.zeka.stack.idea.plugin.PluginContents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationUtil 单元测试类
 * <p>
 * 用于测试 NotificationUtil 工具类中各类通知方法的正确性, 包括信息, 警告, 错误通知, 以及任务完成状态通知等.
 * 通过 Mockito 模拟相关依赖对象, 验证通知是否按预期创建并发送.
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@DisplayName("NotificationUtil 单元测试")
public class NotificationUtilTest {
    /**
     * 用于测试的模拟项目对象
     * <p>
     * 该字段通过 {@link org.mockito.Mock} 注解创建, 用于在单元测试中模拟 Project 类的行为
     */
    @Mock
    private Project mockProject;
    /** NotificationGroupManager mock 实例, 用于单元测试 */
    @Mock
    private NotificationGroupManager mockGroupManager;
    /** 模拟的 NotificationGroup 对象, 用于单元测试 */
    @Mock
    private NotificationGroup mockNotificationGroup;
    /** 模拟的 Notification 对象, 用于单元测试 */
    @Mock
    private Notification mockNotification;

    /**
     * 初始化测试环境, 用于在每个测试方法执行前设置模拟对象
     * <p>
     * 该方法使用 MockitoAnnotations.openMocks 方法初始化所有使用 @Mock 注解的模拟对象,
     * 为测试提供必要的模拟依赖.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试通知信息功能 (Info 类型)
     * <p>
     * 测试场景: 模拟 NotificationGroupManager 获取实例, 并验证通知是否正确创建和触发
     * 预期结果: 调用 notifyInfo 方法后, 应通过 mockNotificationGroup 创建指定类型的通知, 并调用 mockNotification 的 notify 方法
     * <p>
     * 测试使用了 MockedStatic 来模拟 NotificationGroupManager 的静态方法 getInstance, 确保在测试过程中使用模拟对象
     */
    @Test
    @DisplayName("测试通知信息 - Info")
    void testNotifyInfo() {
        try (MockedStatic<NotificationGroupManager> mockedStatic =
                 mockStatic(NotificationGroupManager.class)) {

            mockedStatic.when(NotificationGroupManager::getInstance)
                .thenReturn(mockGroupManager);
            when(mockGroupManager.getNotificationGroup(anyString()))
                .thenReturn(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            NotificationUtil.showInfo(mockProject, "测试内容");

            verify(mockNotificationGroup).createNotification(
                eq("测试标题"),
                eq("测试内容"),
                eq(NotificationType.INFORMATION)
                                                            );
            verify(mockNotification).notify(mockProject);
        }
    }

    /**
     * 测试通知信息 -Warning 功能
     * <p>
     * 测试场景: 调用 notifyWarning 方法发送警告通知
     * 预期结果: 应正确创建警告通知并调用通知的 notify 方法
     * <p>
     * 该测试使用 Mockito 模拟 NotificationGroupManager 和相关对象, 验证 createNotification 和 notify 方法是否被正确调用
     */
    @Test
    @DisplayName("测试通知信息 - Warning")
    void testNotifyWarning() {
        try (MockedStatic<NotificationGroupManager> mockedStatic =
                 mockStatic(NotificationGroupManager.class)) {

            mockedStatic.when(NotificationGroupManager::getInstance)
                .thenReturn(mockGroupManager);
            when(mockGroupManager.getNotificationGroup(anyString()))
                .thenReturn(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            NotificationUtil.showWarning(mockProject, "警告内容");

            verify(mockNotificationGroup).createNotification(
                eq("警告内容"),
                eq(NotificationType.WARNING)
                                                            );
            verify(mockNotification).notify(mockProject);
        }
    }

    /**
     * 测试通知功能中错误类型的通知流程
     * <p>
     * 测试场景: 模拟 NotificationGroupManager 获取实例并创建错误类型通知, 验证是否正确调用通知方法
     * 预期结果: 应调用 createNotification 方法并传入正确的参数, 随后调用通知对象的 notify 方法
     * <p>
     * 特殊说明: 该测试使用了 MockedStatic 来模拟 NotificationGroupManager 的静态方法, 需确保相关依赖已正确引入
     */
    @Test
    @DisplayName("测试通知信息 - Error")
    void testNotifyError() {
        try (MockedStatic<NotificationGroupManager> mockedStatic =
                 mockStatic(NotificationGroupManager.class)) {

            mockedStatic.when(NotificationGroupManager::getInstance)
                .thenReturn(mockGroupManager);
            when(mockGroupManager.getNotificationGroup(anyString()))
                .thenReturn(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            NotificationUtil.showError(mockProject, "错误内容");

            verify(mockNotificationGroup).createNotification(
                eq("错误内容"),
                eq(NotificationType.ERROR)
                                                            );
            verify(mockNotification).notify(mockProject);
        }
    }

    /**
     * 测试完成通知功能, 包含失败情况
     * <p>
     * 测试场景: 当生成 Javadoc 时存在成功, 失败和跳过的测试用例
     * 预期结果: 通知内容应包含成功, 失败和跳过的数量, 并且通知类型应为 WARNING
     * <p>
     * 该测试模拟了 NotificationGroupManager 和相关对象的行为, 验证通知内容是否正确
     * 通过断言内容中包含 "完成: 5","失败: 2","跳过: 1" 以及通知类型为 WARNING 来确保功能正确
     */
    @Test
    @DisplayName("测试完成通知 - 有失败")
    void testNotifyCompletion_withFailures() {
        try (MockedStatic<NotificationGroupManager> mockedStatic =
                 mockStatic(NotificationGroupManager.class)) {

            mockedStatic.when(NotificationGroupManager::getInstance)
                .thenReturn(mockGroupManager);
            when(mockGroupManager.getNotificationGroup(anyString()))
                .thenReturn(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);

            verify(mockNotificationGroup).createNotification(
                eq("Javadoc 生成完成"),
                contentCaptor.capture(),
                typeCaptor.capture()
                                                            );

            String content = contentCaptor.getValue();
            assertThat(content).contains("完成: 5");
            assertThat(content).contains("失败: 2");
            assertThat(content).contains("跳过: 1");

            // 有失败时应该是 WARNING
            assertThat(typeCaptor.getValue()).isEqualTo(NotificationType.WARNING);
        }
    }

    /**
     * 测试完成通知功能, 验证在所有操作成功时通知类型是否正确
     * <p>
     * 测试场景: 模拟 NotificationGroupManager 和其相关对象, 确保在调用 notifyCompletion 方法时,
     * 创建的通知类型为 INFORMATION.
     * <p>
     * 预期结果: 捕获到的 NotificationType 应等于 NotificationType.INFORMATION.
     * <p>
     * 依赖对象:mockGroupManager,mockNotificationGroup,mockNotification
     * <p>
     * 注意: 使用了 MockedStatic 来模拟静态方法 getInstance, 确保测试环境隔离.
     */
    @Test
    @DisplayName("测试完成通知 - 全部成功")
    void testNotifyCompletion_allSuccess() {
        try (MockedStatic<NotificationGroupManager> mockedStatic =
                 mockStatic(NotificationGroupManager.class)) {

            mockedStatic.when(NotificationGroupManager::getInstance)
                .thenReturn(mockGroupManager);
            when(mockGroupManager.getNotificationGroup(anyString()))
                .thenReturn(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);

            verify(mockNotificationGroup).createNotification(
                eq("Javadoc 生成完成"),
                anyString(),
                typeCaptor.capture()
                                                            );

            // 没有失败时应该是 INFORMATION
            assertThat(typeCaptor.getValue()).isEqualTo(NotificationType.INFORMATION);
        }
    }

    /**
     * 测试完成通知功能
     * <p>
     * 测试场景: 当没有完成任何任务时
     * 预期结果: 应创建标题为「Javadoc 生成完成」且类型为 {@link NotificationType#WARNING} 的通知
     */
    @Test
    @DisplayName("测试完成通知 - 没有完成任何任务")
    void testNotifyCompletion_noCompleted() {
        try (MockedStatic<NotificationGroupManager> mockedStatic =
                 mockStatic(NotificationGroupManager.class)) {

            mockedStatic.when(NotificationGroupManager::getInstance)
                .thenReturn(mockGroupManager);
            when(mockGroupManager.getNotificationGroup(anyString()))
                .thenReturn(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);

            verify(mockNotificationGroup).createNotification(
                eq("Javadoc 生成完成"),
                anyString(),
                typeCaptor.capture()
                                                            );

            // 没有完成任何任务时应该是 WARNING
            assertThat(typeCaptor.getValue()).isEqualTo(NotificationType.WARNING);
        }
    }

    /**
     * 测试通知目标完成功能
     * <p>
     * 测试场景: 模拟通知组管理器和通知组对象, 验证通知内容是否正确生成
     * 预期结果: 通知内容应包含指定的文件名, 完成数, 失败数和跳过数
     * <p>
     * 该测试需要使用 Mockito 的 mockStatic 方法模拟静态方法调用, 并验证通知内容是否符合预期
     */
    @Test
    @DisplayName("测试目标完成通知")
    void testNotifyTargetCompletion() {
        try (MockedStatic<NotificationGroupManager> mockedStatic =
                 mockStatic(NotificationGroupManager.class)) {

            mockedStatic.when(NotificationGroupManager::getInstance)
                .thenReturn(mockGroupManager);
            when(mockGroupManager.getNotificationGroup(anyString()))
                .thenReturn(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            NotificationUtil.notifyTargetCompletion(mockProject, "UserService.java", 3, 1, 0);

            ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);

            verify(mockNotificationGroup).createNotification(
                eq("Javadoc 生成完成"),
                contentCaptor.capture(),
                any(NotificationType.class)
                                                            );

            String content = contentCaptor.getValue();
            assertThat(content).contains("UserService.java");
            assertThat(content).contains("完成: 3");
            assertThat(content).contains("失败: 1");
            assertThat(content).contains("跳过: 0");
        }
    }

    /**
     * 测试无任务通知功能
     * <p>
     * 测试场景: 当没有需要生成文档的元素时, 调用 notifyNoTask 方法
     * 预期结果: 应创建一条类型为 INFORMATION 的通知
     * <p>
     * 该测试通过 Mock 模拟 NotificationGroupManager 和相关对象, 验证 notifyNoTask 方法是否正确调用
     * createNotification 方法, 并传递正确的参数
     */
    @Test
    @DisplayName("测试无任务通知")
    void testNotifyNoTask() {
        try (MockedStatic<NotificationGroupManager> mockedStatic =
                 mockStatic(NotificationGroupManager.class)) {

            mockedStatic.when(NotificationGroupManager::getInstance)
                .thenReturn(mockGroupManager);
            when(mockGroupManager.getNotificationGroup(anyString()))
                .thenReturn(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            NotificationUtil.showWarning(mockProject, "没有需要生成文档的元素");

            verify(mockNotificationGroup).createNotification(
                eq(PluginContents.PLUGIN_NAME),
                eq("没有需要生成文档的元素"),
                eq(NotificationType.WARNING));
        }
    }

    /**
     * 测试错误消息通知功能
     * <p>
     * 测试场景: 模拟 NotificationGroupManager 的静态方法并验证错误消息是否正确通知
     * 预期结果: 调用 {@link NotificationUtil#showError} 方法后, 应通过 {@link NotificationGroup#createNotification} 方法创建包含指定标题, 内容和类型的通知
     * <p>
     * 特殊说明: 测试使用了 {@link MockedStatic} 来模拟静态方法 {@link NotificationGroupManager#getInstance}, 并验证通知参数是否符合预期
     */
    @Test
    @DisplayName("测试错误消息通知")
    void testNotifyErrorMessage() {
        try (MockedStatic<NotificationGroupManager> mockedStatic =
                 mockStatic(NotificationGroupManager.class)) {

            mockedStatic.when(NotificationGroupManager::getInstance)
                .thenReturn(mockGroupManager);
            when(mockGroupManager.getNotificationGroup(anyString()))
                .thenReturn(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            NotificationUtil.showError(mockProject, "API 调用失败");

            verify(mockNotificationGroup).createNotification(
                eq("IntelliAI Javadoc - 错误"),
                eq("API 调用失败"),
                eq(NotificationType.ERROR)
                                                            );
        }
    }

    /**
     * 测试通知索引中功能
     * <p>
     * 测试场景: 模拟通知组管理器和通知组对象, 验证在项目索引过程中是否生成正确的通知内容
     * 预期结果: 通知内容应包含“不可用”和“索引中”关键词
     * <p>
     * 说明: 该测试使用 Mockito 框架进行模拟, 验证通知内容是否符合预期
     */
    @Test
    @DisplayName("测试索引中通知")
    void testNotifyIndexing() {
        try (MockedStatic<NotificationGroupManager> mockedStatic =
                 mockStatic(NotificationGroupManager.class)) {

            mockedStatic.when(NotificationGroupManager::getInstance)
                .thenReturn(mockGroupManager);
            when(mockGroupManager.getNotificationGroup(anyString()))
                .thenReturn(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            NotificationUtil.notifyIndexing(mockProject);

            ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);

            verify(mockNotificationGroup).createNotification(
                eq(PluginContents.PLUGIN_NAME),
                contentCaptor.capture(),
                eq(NotificationType.WARNING)
                                                            );

            String content = contentCaptor.getValue();
            assertThat(content).contains("不可用");
            assertThat(content).contains("索引中");
        }
    }

    /**
     * 测试通知方法可以传递 null 的 project 参数
     * <p>
     * 测试场景: 当 project 参数为 null 时, 调用 notifyInfo 方法
     * 预期结果: 通知方法应正常执行, 并验证 mockNotification 的 notify 方法被正确调用
     * <p>
     * 说明: 此测试需要使用 mockStatic 模拟 NotificationGroupManager 的静态方法 getInstance,
     * 并设置其返回 mockGroupManager, 同时模拟 getNotificationGroup 和 createNotification 方法的行为.
     */
    @Test
    @DisplayName("测试通知可以传递 null project")
    void testNotify_withNullProject() {
        try (MockedStatic<NotificationGroupManager> mockedStatic =
                 mockStatic(NotificationGroupManager.class)) {

            mockedStatic.when(NotificationGroupManager::getInstance)
                .thenReturn(mockGroupManager);
            when(mockGroupManager.getNotificationGroup(anyString()))
                .thenReturn(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            // 传递 null project 应该不会抛出异常
            NotificationUtil.showInfo(null, "测试内容");

            verify(mockNotification).notify(null);
        }
    }

    /**
     * 测试 {@link NotificationUtil#notifyTargetCompletion(Project, String, int, int, int)} 方法在不同状态组合下的通知类型判断.
     * <p>
     * 测试场景:
     * <ul>
     *   <li> 目标完成度 5, 已完成任务 2, 未完成任务 1, 期望生成 {@link NotificationType#WARNING} 通知.</li>
     *   <li> 目标完成度 5, 已完成任务 0, 未完成任务 1, 期望生成 {@link NotificationType#INFORMATION} 通知.</li>
     *   <li> 目标完成度 0, 已完成任务 0, 未完成任务 5, 期望生成 {@link NotificationType#WARNING} 通知.</li>
     * </ul>
     * <p>
     * 预期结果:
     * 对每种参数组合, 调用 {@link NotificationGroupManager#createNotification(String, String, NotificationType)} 时传入的 {@link NotificationType}
     * 与上述期望一致.
     */
    @Test
    @DisplayName("测试目标完成通知 - 不同的状态组合")
    void testNotifyTargetCompletion_differentStatusCombinations() {
        try (MockedStatic<NotificationGroupManager> mockedStatic =
                 mockStatic(NotificationGroupManager.class)) {

            mockedStatic.when(NotificationGroupManager::getInstance)
                .thenReturn(mockGroupManager);
            when(mockGroupManager.getNotificationGroup(anyString()))
                .thenReturn(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            // 测试有失败的情况
            NotificationUtil.notifyTargetCompletion(mockProject, "Test1", 5, 2, 1);
            ArgumentCaptor<NotificationType> typeCaptor1 = ArgumentCaptor.forClass(NotificationType.class);
            verify(mockNotificationGroup).createNotification(anyString(), anyString(), typeCaptor1.capture());
            assertThat(typeCaptor1.getValue()).isEqualTo(NotificationType.WARNING);

            reset(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            // 测试全部成功的情况
            NotificationUtil.notifyTargetCompletion(mockProject, "Test2", 5, 0, 1);
            ArgumentCaptor<NotificationType> typeCaptor2 = ArgumentCaptor.forClass(NotificationType.class);
            verify(mockNotificationGroup).createNotification(anyString(), anyString(), typeCaptor2.capture());
            assertThat(typeCaptor2.getValue()).isEqualTo(NotificationType.INFORMATION);

            reset(mockNotificationGroup);
            when(mockNotificationGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mockNotification);

            // 测试没有完成任何任务的情况
            NotificationUtil.notifyTargetCompletion(mockProject, "Test3", 0, 0, 5);
            ArgumentCaptor<NotificationType> typeCaptor3 = ArgumentCaptor.forClass(NotificationType.class);
            verify(mockNotificationGroup).createNotification(anyString(), anyString(), typeCaptor3.capture());
            assertThat(typeCaptor3.getValue()).isEqualTo(NotificationType.WARNING);
        }
    }
}

