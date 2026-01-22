package dev.dong4j.zeka.stack.api;

import org.junit.jupiter.api.Test;

import dev.dong4j.zeka.kernel.common.util.SystemUtils;
import dev.dong4j.zeka.kernel.devtools.AutoGeneratorCodeBuilder;
import dev.dong4j.zeka.kernel.devtools.ModuleConfig;
import dev.dong4j.zeka.kernel.devtools.TemplatesConfig;

/**
 * 代码自动生成器测试类
 *
 * <p>该类用于测试ZEKA框架的代码自动生成功能。
 * 主要用于根据数据库表结构自动生成相关的Java代码文件,
 * 包括实体类、DAO、Service、Controller等。</p>
 *
 * <p>主要功能:
 * <ul>
 *   <li>根据数据库表自动生成实体类</li>
 *   <li>生成DAO层数据访问代码</li>
 *   <li>生成Service层业务逻辑代码</li>
 *   <li>生成Controller层控制器代码</li>
 *   <li>生成DTO、Query、Converter等辅助类</li>
 *   <li>生成MyBatis XML映射文件</li>
 * </ul></p>
 *
 * <p>支持的表:
 * <ul>
 *   <li>feeds - 公众号信息表</li>
 *   <li>articles - 文章信息表</li>
 * </ul></p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.20 16:19
 * @since 1.0.0
 */
class AutoGeneratorCodeTest {

    /**
     * 简单的代码自动生成测试
     *
     * <p>该方法演示了如何使用ZEKA框架的代码生成器,
     * 根据指定的数据库表自动生成完整的CRUD代码结构。</p>
     *
     * <p>生成的文件类型:
     * <ul>
     *   <li>DAO - 数据访问对象</li>
     *   <li>SERVICE - 业务服务接口</li>
     *   <li>ENUM - 枚举类</li>
     *   <li>IMPL - 服务实现类</li>
     *   <li>ENTITY - 实体类</li>
     *   <li>DTO - 数据传输对象</li>
     *   <li>QUERY - 查询对象</li>
     *   <li>CONVERTER - 转换器</li>
     *   <li>XML - MyBatis映射文件</li>
     *   <li>START - 启动类</li>
     *   <li>CONTROLLER - 控制器</li>
     * </ul></p>
     *
     * <p>配置说明:
     * <ul>
     *   <li>包名:minex.module.wechat</li>
     *   <li>作者:系统用户名</li>
     *   <li>公司:minex</li>
     *   <li>版本:1.0.0</li>
     * </ul></p>
     *
     * @since 1.0.0
     */
    @Test
    void simpleAutoGeneratorCode() {
        // 使用代码生成器构建器创建自动生成配置
        AutoGeneratorCodeBuilder.onAutoGeneratorCode()
            // 设置存放自动生成的代码路径, 不填则默认当前项目下
            .withModelPath("")
            .withVersion("1.0.0")
            .withCompany("zeka.stack")
            .withEmail("gmail.com")
            // 设置作者名, 默认读取 ZEKA_NAME_SPACE 变量
            .withAuthor(SystemUtils.USER_NAME)
            // 设置包名 (前缀默认为 公司项目顶层包路径, 因此最终的包名为: ${公司项目顶层包路径}.${packageName})
            .withPackageName("stack.api.project")
            // 忽略前缀
            .withPrefix(new String[] {""})
            // 设置根据哪张表生成代码, 可写多张表
            .withTables(new String[] {"project", "feedback", "feedback_comment"})

            // 设置需要生成的模板 不设置则全部生成
            .withTemplate(
                TemplatesConfig.DAO,           // 数据访问对象
                TemplatesConfig.SERVICE,       // 业务服务接口
                TemplatesConfig.ENUM,          // 枚举类
                TemplatesConfig.IMPL,          // 服务实现类
                TemplatesConfig.ENTITY,        // 实体类
                TemplatesConfig.DTO,           // 数据传输对象
                TemplatesConfig.QUERY,         // 查询对象
                TemplatesConfig.CONVERTER,     // 转换器
                TemplatesConfig.XML,           // MyBatis映射文件
                // TemplatesConfig.START,         // 启动类
                TemplatesConfig.CONTROLLER,    // 控制器

                // 占位符而已, 避免手动删除逗号的烦恼
                TemplatesConfig.PLACEHOLDER
                         )
            // 设置需要生成的配置
            .withComponets(
                // PropertiesConfig.BOOT_CONFIG
                          )
            // 设置模块类型为单模块
            .withModuleType(ModuleConfig.ModuleType.SINGLE_MODULE)
            // 执行代码生成
            .build();
    }

}
