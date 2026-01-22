package dev.dong4j.zeka.stack.api.project.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.dong4j.zeka.kernel.common.api.BaseCodes;
import dev.dong4j.zeka.stack.api.project.dao.ProjectMapper;
import dev.dong4j.zeka.stack.api.project.entity.converter.ProjectConverter;
import dev.dong4j.zeka.stack.api.project.entity.dto.ProjectDTO;
import dev.dong4j.zeka.stack.api.project.entity.form.ProjectForm;
import dev.dong4j.zeka.stack.api.project.entity.po.Project;
import dev.dong4j.zeka.stack.api.project.service.ProjectService;
import dev.dong4j.zeka.starter.mybatis.service.impl.BaseServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p> 项目表 服务接口实现类 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22 20:02
 * @since 1.0.0
 */
@Slf4j
@Service
@AllArgsConstructor
public class ProjectServiceImpl extends BaseServiceImpl<ProjectMapper, Project> implements ProjectService {

    /**
     * 根据 ID 获取详细信息
     *
     * @param id 主键
     * @return 实体对象
     * @since 1.0.0
     */
    @Override
    public ProjectDTO detail(Long id) {
        final Project po = this.baseMapper.selectById(id);
        BaseCodes.DATA_ERROR.notNull(po);
        return ProjectConverter.INSTANCE.p2d(po);
    }

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(ProjectForm form) {
        final Project po = ProjectConverter.INSTANCE.f2p(form);
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
    public void edit(ProjectForm form) {
        final int updatedCount = this.baseMapper.updateById(ProjectConverter.INSTANCE.f2p(form));
        BaseCodes.OPTION_FAILURE.isTrue(updatedCount == 1);
    }
}


