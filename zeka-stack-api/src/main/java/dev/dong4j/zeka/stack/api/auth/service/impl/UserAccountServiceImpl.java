package dev.dong4j.zeka.stack.api.auth.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.dong4j.zeka.kernel.common.api.BaseCodes;
import dev.dong4j.zeka.stack.api.auth.dao.UserAccountMapper;
import dev.dong4j.zeka.stack.api.auth.entity.converter.UserAccountConverter;
import dev.dong4j.zeka.stack.api.auth.entity.dto.UserAccountDTO;
import dev.dong4j.zeka.stack.api.auth.entity.form.UserAccountForm;
import dev.dong4j.zeka.stack.api.auth.entity.po.UserAccount;
import dev.dong4j.zeka.stack.api.auth.service.UserAccountService;
import dev.dong4j.zeka.starter.mybatis.service.impl.BaseServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p> GitHub 账号绑定表 服务接口实现类 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 18:54
 * @since 1.0.0
 */
@Slf4j
@Service
@AllArgsConstructor
public class UserAccountServiceImpl extends BaseServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    /**
     * 根据 ID 获取详细信息
     *
     * @param id 主键
     * @return 实体对象
     * @since 1.0.0
     */
    @Override
    public UserAccountDTO detail(Long id) {
        final UserAccount po = this.baseMapper.selectById(id);
        BaseCodes.DATA_ERROR.notNull(po);
        return UserAccountConverter.INSTANCE.p2d(po);
    }

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(UserAccountForm form) {
        final UserAccount po = UserAccountConverter.INSTANCE.f2p(form);
        final int savedCount = this.baseMapper.insertIgnore(po);
        BaseCodes.OPTION_FAILURE.isTrue(savedCount == 1);
    }

    /**
     * 更新数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(UserAccountForm form) {
        final int updatedCount = this.baseMapper.updateById(UserAccountConverter.INSTANCE.f2p(form));
        BaseCodes.OPTION_FAILURE.isTrue(updatedCount == 1);
    }
}


