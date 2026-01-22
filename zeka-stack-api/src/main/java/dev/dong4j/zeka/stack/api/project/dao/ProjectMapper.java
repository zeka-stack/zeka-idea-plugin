package dev.dong4j.zeka.stack.api.project.dao;

import org.apache.ibatis.annotations.Mapper;

import dev.dong4j.zeka.stack.api.project.entity.po.Project;
import dev.dong4j.zeka.starter.mybatis.base.BaseDao;

/**
 * <p> 项目表 Dao 接口  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22 20:02
 * @since 1.0.0
 */
@Mapper
public interface ProjectMapper extends BaseDao<Project> {

}
