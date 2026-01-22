package dev.dong4j.zeka.stack.api.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import dev.dong4j.zeka.kernel.common.api.BaseCodes;
import dev.dong4j.zeka.kernel.validation.group.UpdateGroup;
import dev.dong4j.zeka.stack.api.project.entity.dto.FeedbackDTO;
import dev.dong4j.zeka.stack.api.project.entity.form.FeedbackForm;
import dev.dong4j.zeka.stack.api.project.entity.form.FeedbackQuery;
import dev.dong4j.zeka.starter.rest.ServletController;
import dev.dong4j.zeka.starter.rest.annotation.RestControllerWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p> 反馈表 控制器 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22 20:02
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "反馈表接口")
@AllArgsConstructor
@RestControllerWrapper("/projects/feedbacks")
public class FeatureFeedbackController extends ServletController {

    /** Feedback service */
    private final dev.dong4j.zeka.stack.api.project.service.FeedbackService feedbackService;

    /**
     * 根据条件查询全部数据
     *
     * @param query 查询参数
     * @return 对象集合
     * @since 1.0.0
     */
    @GetMapping("/list")
    @Operation(summary = "列表查询")
    @ApiOperationSupport(order = 1)
    public List<FeedbackDTO> list(@ParameterObject FeedbackQuery query) {
        return this.feedbackService.list(query);
    }

    /**
     * 分页查询
     *
     * @param query 查询参数
     * @return 分页数据
     * @since 1.0.0
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询")
    @ApiOperationSupport(order = 2)
    public IPage<FeedbackDTO> pages(@ParameterObject FeedbackQuery query) {
        return this.feedbackService.page(query);
    }

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @PostMapping
    @Operation(summary = "新增数据")
    @ApiOperationSupport(order = 3)
    public void create(@Validated @RequestBody FeedbackForm form) {
        this.feedbackService.create(form);
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return dto 单条数据
     * @since 1.0.0
     */
    @GetMapping("/{id}")
    @Operation(summary = "详情")
    @ApiOperationSupport(order = 4)
    public FeedbackDTO detail(@PathVariable Long id) {
        return this.feedbackService.detail(id);
    }

    /**
     * 修改数据
     *
     * @param id   主键
     * @param form 参数实体
     * @since 1.0.0
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改数据")
    @ApiOperationSupport(order = 5)
    public void edit(@PathVariable Long id, @Validated( {UpdateGroup.class, Default.class}) @RequestBody FeedbackForm form) {
        BaseCodes.PARAM_VERIFY_ERROR.isTrue(id.equals(form.getId()), "id 不一致");
        BaseCodes.DATA_ERROR.notNull(feedbackService.getById(form.getId()), "指定的数据不存在: " + id);
        this.feedbackService.edit(form);
    }

    /**
     * 删除数据
     *
     * @param ids 主键集合
     * @since 1.0.0
     */
    @DeleteMapping
    @Operation(summary = "删除数据")
    @ApiOperationSupport(order = 6)
    public void remove(@RequestBody List<Long> ids) {
        BaseCodes.DATA_ERROR.notEmpty(ids, "带删除的数据标识不能为空");
        this.feedbackService.removeByIds(ids);
    }

    /**
     * 点赞
     *
     * @param id 主键
     * @since 1.0.0
     */
    @PostMapping("/{id}/vote")
    @Operation(summary = "点赞")
    @ApiOperationSupport(order = 7)
    public void vote(@PathVariable Long id) {
        this.feedbackService.vote(id);
    }

}
