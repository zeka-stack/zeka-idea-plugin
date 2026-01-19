package dev.dong4j.zeka.stack.api.auth.entity.converter;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import dev.dong4j.zeka.kernel.common.mapstruct.ExtendConverter;
import dev.dong4j.zeka.stack.api.auth.entity.dto.UserSessionDTO;
import dev.dong4j.zeka.stack.api.auth.entity.form.UserSessionForm;
import dev.dong4j.zeka.stack.api.auth.entity.po.UserSession;

/**
 * <p> 登录会话表 实体转换器 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 18:54
 * @since 1.0.0
 */
@Mapper
public interface UserSessionConverter extends ExtendConverter<UserSessionForm, UserSessionDTO, UserSession> {
    /** INSTANCE */
    UserSessionConverter INSTANCE = Mappers.getMapper(UserSessionConverter.class);
}
