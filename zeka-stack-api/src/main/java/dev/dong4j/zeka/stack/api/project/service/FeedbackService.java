package dev.dong4j.zeka.stack.api.project.service;

import java.util.Collection;

import dev.dong4j.zeka.stack.api.project.entity.dto.FeedbackDTO;
import dev.dong4j.zeka.stack.api.project.entity.form.FeedbackForm;
import dev.dong4j.zeka.stack.api.project.entity.po.Feedback;
import dev.dong4j.zeka.starter.mybatis.service.BaseService;

/**
 * <p> 反馈表 服务接口 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22 20:02
 * @since 1.0.0
 */
public interface FeedbackService extends BaseService<Feedback> {

    /**
     * 根据 ID 获取详细信息
     *
     * @param id 主键
     * @return 实体对象
     * @since 1.0.0
     */
    FeedbackDTO detail(Long id);

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    void create(FeedbackForm form);

    /**
     * 更新数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    void edit(FeedbackForm form);

    /**
     * 点赞
     *
     * @param id 主键
     * @since 1.0.0
     */
    void vote(Long id);

    /**
     * 根据主键集合批量删除数据
     * <p> 从数据库中根据传入的主键集合删除对应记录, 返回是否删除成功的布尔值 </p>
     *
     * @param list 主键集合, 类型为 Collection<?>
     * @return 如果删除成功返回 true, 否则返回 false
     */
    boolean removeByIds(Collection<?> list);

}

