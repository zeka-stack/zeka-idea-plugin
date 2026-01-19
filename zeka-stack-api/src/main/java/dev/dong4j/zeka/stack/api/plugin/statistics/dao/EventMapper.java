package dev.dong4j.zeka.stack.api.plugin.statistics.dao;

import org.apache.ibatis.annotations.Mapper;

import dev.dong4j.zeka.stack.api.plugin.statistics.entity.po.Event;
import dev.dong4j.zeka.starter.mybatis.base.BaseDao;

/**
 * <p> 统计事件表 Dao 接口  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 11:55
 * @since 1.0.0
 */
@Mapper
public interface EventMapper extends

                             BaseDao<Event> {

}
