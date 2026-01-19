package dev.dong4j.zeka.stack.api.auth.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.dong4j.zeka.kernel.common.api.BaseCodes;
import dev.dong4j.zeka.stack.api.auth.dao.UserSessionMapper;
import dev.dong4j.zeka.stack.api.auth.entity.converter.UserSessionConverter;
import dev.dong4j.zeka.stack.api.auth.entity.dto.UserSessionDTO;
import dev.dong4j.zeka.stack.api.auth.entity.form.UserSessionForm;
import dev.dong4j.zeka.stack.api.auth.entity.po.UserSession;
import dev.dong4j.zeka.stack.api.auth.service.UserSessionService;
import dev.dong4j.zeka.starter.mybatis.service.impl.BaseServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p> 登录会话表 服务接口实现类 </p>
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
public class UserSessionServiceImpl extends BaseServiceImpl<UserSessionMapper, UserSession> implements UserSessionService {

    /**
     * 根据 ID 获取详细信息
     *
     * @param id 主键
     * @return 实体对象
     * @since 1.0.0
     */
    @Override
    public UserSessionDTO detail(Long id) {
        final UserSession po = this.baseMapper.selectById(id);
        BaseCodes.DATA_ERROR.notNull(po);
        return UserSessionConverter.INSTANCE.p2d(po);
    }

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(UserSessionForm form) {
        final UserSession po = UserSessionConverter.INSTANCE.f2p(form);
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
    public void edit(UserSessionForm form) {
        final int updatedCount = this.baseMapper.updateById(UserSessionConverter.INSTANCE.f2p(form));
        BaseCodes.OPTION_FAILURE.isTrue(updatedCount == 1);
    }
}


