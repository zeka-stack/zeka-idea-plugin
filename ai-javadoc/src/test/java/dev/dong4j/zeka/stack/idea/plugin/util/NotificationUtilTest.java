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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationUtilTest 类
 * <p>
 * 该类用于对 NotificationUtil 工具类进行单元测试，验证其通知功能的正确性。
 * 包括对不同通知类型（Info、Warning、Error）以及任务完成状态（成功、失败、无任务）的测试。
 * 测试内容涵盖通知信息的构建、发送以及对不同情况下的状态处理逻辑。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@DisplayName("NotificationUtil 单元测试")
public class NotificationUtilTest {

    /** 模拟的 Project 对象，用于单元测试中替代真实实例 */
    @Mock
    private Project mockProject;

    /** 用于模拟的 NotificationGroupManager 实例 */
    @Mock
    private NotificationGroupManager mockGroupManager;

    /** 模拟的 NotificationGroup 对象，用于单元测试 */
    @Mock
    private NotificationGroup mockNotificationGroup;

    /** 模拟的 Notification 对象，用于单元测试 */
    @Mock
    private Notification mockNotification;

    /**
     * 初始化测试环境，设置Mockito注解
     * <p>
     * 用于在每个测试方法执行前初始化Mock对象，确保测试环境的稳定性
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试通知信息 - Info 类型的通知发送功能
     * <p>
     * 测试场景：模拟 NotificationGroupManager 和其相关依赖，验证 notifyInfo 方法是否正确调用
     * 预期结果：应确保通知标题、内容和类型正确传递，并验证通知组和通知对象的相应方法被调用
     * <p>
     * 特殊说明：使用 MockedStatic 模拟静态方法 getInstance，确保测试环境隔离
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

            NotificationUtil.notifyInfo(mockProject, "测试标题", "测试内容");

            verify(mockNotificationGroup).createNotification(
                eq("测试标题"),
                eq("测试内容"),
                eq(NotificationType.INFORMATION)
                                                            );
            verify(mockNotification).notify(mockProject);
        }
    }

    /**
     * 测试通知信息 - Warning 功能
     * <p>
     * 测试场景：模拟 NotificationGroupManager 和相关对象，验证 notifyWarning 方法是否正确调用
     * 预期结果：应调用 createNotification 方法并传递正确的参数，同时验证 notify 方法被调用
     * <p>
     * 说明：测试中使用了 Mockito 的 mockStatic 方法模拟静态方法 getInstance，并验证相关对象的方法调用
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

            NotificationUtil.notifyWarning(mockProject, "警告标题", "警告内容");

            verify(mockNotificationGroup).createNotification(
                eq("警告标题"),
                eq("警告内容"),
                eq(NotificationType.WARNING)
                                                            );
            verify(mockNotification).notify(mockProject);
        }
    }

    /**
     * 测试通知错误信息功能
     * <p>
     * 测试场景：调用 notifyError 方法发送错误通知
     * 预期结果：应正确创建错误类型的通知并触发通知发送
     * <p>
     * 测试过程中使用 Mockito 模拟 NotificationGroupManager 和相关对象，验证 createNotification 和 notify 方法是否被正确调用
     * <p>
     * 关联方法：{@link NotificationUtil#notifyError(Project, String, String)}
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

            NotificationUtil.notifyError(mockProject, "错误标题", "错误内容");

            verify(mockNotificationGroup).createNotification(
                eq("错误标题"),
                eq("错误内容"),
                eq(NotificationType.ERROR)
                                                            );
            verify(mockNotification).notify(mockProject);
        }
    }

    /**
     * 测试通知完成功能，当存在失败情况时
     * <p>
     * 测试场景：项目中有 5 个成功、2 个失败和 1 个跳过的任务
     * 预期结果：通知内容应包含成功、失败和跳过的数量，并且通知类型应为 WARNING
     * <p>
     * 该测试验证在有任务失败的情况下，通知内容是否正确生成并设置为 WARNING 类型
     * <p>
     * 注意：测试中使用了 Mockito 的 mockStatic 方法模拟 NotificationGroupManager 的静态方法
     * 以及 ArgumentCaptor 来捕获 createNotification 方法的参数，确保内容和类型符合预期
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

            NotificationUtil.notifyCompletion(mockProject, 5, 2, 1);

            ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);

            verify(mockNotificationGroup).createNotification(
                eq("JavaDoc 生成完成"),
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
     * 测试通知完成功能 - 全部成功场景
     * <p>
     * 测试场景：当所有任务执行成功，没有失败时
     * 预期结果：应发送类型为 INFORMATION 的通知
     * <p>
     * 测试过程中使用 Mockito 模拟 NotificationGroupManager 和相关对象，
     * 验证 notifyCompletion 方法调用 createNotification 方法时传入的 NotificationType 是否为 INFORMATION。
     * <p>
     * 注意：测试需要 mock NotificationGroupManager 和其相关依赖对象。
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

            NotificationUtil.notifyCompletion(mockProject, 10, 0, 2);

            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);

            verify(mockNotificationGroup).createNotification(
                eq("JavaDoc 生成完成"),
                anyString(),
                typeCaptor.capture()
                                                            );

            // 没有失败时应该是 INFORMATION
            assertThat(typeCaptor.getValue()).isEqualTo(NotificationType.INFORMATION);
        }
    }

    /**
     * 测试通知完成功能 - 没有完成任何任务的场景
     * <p>
     * 测试场景：当没有完成任何任务时，调用 notifyCompletion 方法
     * 预期结果：应生成一个类型为 WARNING 的通知
     * <p>
     * 该测试通过模拟 NotificationGroupManager 和相关对象，验证在无任务完成情况下，
     * 通知类型是否正确设置为 WARNING。使用 ArgumentCaptor 捕获通知类型并进行断言。
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

            NotificationUtil.notifyCompletion(mockProject, 0, 0, 5);

            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);

            verify(mockNotificationGroup).createNotification(
                eq("JavaDoc 生成完成"),
                anyString(),
                typeCaptor.capture()
                                                            );

            // 没有完成任何任务时应该是 WARNING
            assertThat(typeCaptor.getValue()).isEqualTo(NotificationType.WARNING);
        }
    }

    /**
     * 测试通知目标完成的功能
     * <p>
     * 测试场景：模拟 NotificationGroupManager 和其相关依赖，验证 notifyTargetCompletion 方法是否正确生成通知内容
     * 预期结果：通知内容应包含文件名 "UserService.java" 以及统计信息：完成 3 项，失败 1 项，跳过 0 项
     * <p>
     * 该测试需要使用 Mockito 的 mockStatic 功能来模拟静态方法 getInstance，并验证 createNotification 方法的调用参数
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
                eq("JavaDoc 生成完成"),
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
     * 测试场景：当没有需要生成文档的元素时，调用 notifyNoTask 方法
     * 预期结果：应创建一条类型为 INFORMATION 的通知，内容为 "没有需要生成文档的元素"
     * <p>
     * 该测试使用 Mockito 模拟 NotificationGroupManager 和相关对象，验证 createNotification 方法是否被正确调用
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

            NotificationUtil.notifyNoTask(mockProject, "没有需要生成文档的元素");

            verify(mockNotificationGroup).createNotification(
                eq("AI Javadoc"),
                eq("没有需要生成文档的元素"),
                eq(NotificationType.INFORMATION)
                                                            );
        }
    }

    /**
     * 测试错误消息通知功能
     * <p>
     * 测试场景：调用 notifyErrorMessage 方法并传递错误信息
     * 预期结果：应正确创建并发送错误类型的通知
     * <p>
     * 测试过程中使用了 Mockito 的 mockStatic 方法模拟 NotificationGroupManager 的静态方法，
     * 并验证 createNotification 方法是否被正确调用，参数应为指定的错误标题、消息和错误类型。
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

            NotificationUtil.notifyErrorMessage(mockProject, "API 调用失败");

            verify(mockNotificationGroup).createNotification(
                eq("AI Javadoc - 错误"),
                eq("API 调用失败"),
                eq(NotificationType.ERROR)
                                                            );
        }
    }

    /**
     * 测试通知索引中状态的功能
     * <p>
     * 测试场景：当项目处于索引过程中时，调用 notifyIndexing 方法应生成相应的通知
     * 预期结果：通知内容应包含 "不可用" 和 "索引中" 的关键词
     * <p>
     * 该测试使用 Mockito 模拟 NotificationGroupManager 和相关对象，验证通知内容是否符合预期
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
                eq("AI Javadoc"),
                contentCaptor.capture(),
                eq(NotificationType.WARNING)
                                                            );

            String content = contentCaptor.getValue();
            assertThat(content).contains("不可用");
            assertThat(content).contains("索引中");
        }
    }

    /**
     * 测试通知工具类可以传递 null project 参数
     * <p>
     * 测试场景：当 project 参数为 null 时
     * 预期结果：调用 notifyInfo 方法不会抛出异常，并正确调用通知链
     * <p>
     * 特殊说明：需要使用 Mockito 的 mockStatic 方法模拟 NotificationGroupManager 的静态方法
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
            NotificationUtil.notifyInfo(null, "测试标题", "测试内容");

            verify(mockNotification).notify(null);
        }
    }

    /**
     * 测试通知目标完成功能，验证不同状态组合下通知类型的正确性
     * <p>
     * 测试场景：
     * 1. 存在失败任务的情况：测试名称为 "Test1"，总任务数为5，完成数为2，失败数为1，预期通知类型为WARNING
     * 2. 所有任务均成功完成：测试名称为 "Test2"，总任务数为5，完成数为5，失败数为0，预期通知类型为INFORMATION
     * 3. 没有完成任何任务：测试名称为 "Test3"，总任务数为5，完成数为0，失败数为0，预期通知类型为WARNING
     * <p>
     * 预期结果：根据不同的任务完成状态，应返回对应的通知类型
     * <p>
     * 注意：测试中使用了Mockito框架进行模拟，涉及NotificationGroupManager和NotificationGroup的模拟对象
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

