package dev.dong4j.zeka.stack.api.auth.service;

import dev.dong4j.zeka.stack.api.auth.entity.dto.UserAccountDTO;
import dev.dong4j.zeka.stack.api.auth.entity.form.UserAccountForm;
import dev.dong4j.zeka.stack.api.auth.entity.po.UserAccount;
import dev.dong4j.zeka.starter.mybatis.service.BaseService;

/**
 * <p> GitHub 账号绑定表 服务接口 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 18:54
 * @since 1.0.0
 */
public interface UserAccountService extends

                                    BaseService<UserAccount> {

    /**
     * 根据 ID 获取详细信息
     *
     * @param id 主键
     * @return 实体对象
     * @since 1.0.0
     */
    UserAccountDTO detail(Long id);

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    void create(UserAccountForm form);

    /**
     * 更新数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    void edit(UserAccountForm form);

}

