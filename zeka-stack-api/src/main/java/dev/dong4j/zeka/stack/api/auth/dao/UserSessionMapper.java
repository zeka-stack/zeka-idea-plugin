package dev.dong4j.zeka.stack.api.auth.dao;

import org.apache.ibatis.annotations.Mapper;

import dev.dong4j.zeka.stack.api.auth.entity.po.UserSession;
import dev.dong4j.zeka.starter.mybatis.base.BaseDao;

/**
 * <p> 登录会话表 Dao 接口  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 18:54
 * @since 1.0.0
 */
@Mapper
public interface UserSessionMapper extends BaseDao<UserSession> {

}
