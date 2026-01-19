package dev.dong4j.zeka.stack.api.auth.service;

import dev.dong4j.zeka.stack.api.auth.entity.dto.UserSessionDTO;
import dev.dong4j.zeka.stack.api.auth.entity.form.UserSessionForm;
import dev.dong4j.zeka.stack.api.auth.entity.po.UserSession;
import dev.dong4j.zeka.starter.mybatis.service.BaseService;

/**
 * <p> 登录会话表 服务接口 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 18:54
 * @since 1.0.0
 */
public interface UserSessionService extends

                                    BaseService<UserSession> {

    /**
     * 根据 ID 获取详细信息
     *
     * @param id 主键
     * @return 实体对象
     * @since 1.0.0
     */
    UserSessionDTO detail(Long id);

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    void create(UserSessionForm form);

    /**
     * 更新数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    void edit(UserSessionForm form);

}

