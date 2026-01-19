package dev.dong4j.zeka.stack.api.plugin.statistics.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.util.StringUtils;
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
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.dto.EventStat30mDTO;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.dto.EventStatOverviewDTO;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.form.EventStat30mForm;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.form.EventStat30mQuery;
import dev.dong4j.zeka.stack.api.plugin.statistics.service.EventStat30mService;
import dev.dong4j.zeka.starter.rest.ServletController;
import dev.dong4j.zeka.starter.rest.annotation.RestControllerWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.AllArgsConstructor;

/**
 * <p> 统计事件 30 分钟聚合表 控制器 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 17:34
 * @since 1.0.0
 */
@Tag(name = "统计事件 30 分钟聚合表接口")
@AllArgsConstructor
@RestControllerWrapper("/plugin/event-stat30")
public class EventStat30mController extends ServletController {

    /** EventStat30m service */
    private final EventStat30mService eventStat30mService;

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
    public List<EventStat30mDTO> list(@ParameterObject EventStat30mQuery query) {
        return this.eventStat30mService.list(query);
    }

    /**
     * 统计概览（WebUI）
     *
     * @param query 查询参数
     * @return 概览数据
     * @since 1.0.0
     */
    @GetMapping("/overview")
    @Operation(summary = "统计概览(WebUI)")
    @ApiOperationSupport(order = 2)
    public EventStatOverviewDTO overview(@ParameterObject EventStat30mQuery query) {
        BaseCodes.PARAM_VERIFY_ERROR.isTrue(StringUtils.hasText(query.getDeviceId()), "deviceId 不能为空");
        return this.eventStat30mService.overview(query);
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
    @ApiOperationSupport(order = 3)
    public IPage<EventStat30mDTO> pages(@ParameterObject EventStat30mQuery query) {
        return this.eventStat30mService.page(query);
    }

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @PostMapping
    @Operation(summary = "新增数据")
    @ApiOperationSupport(order = 4)
    public void create(@Validated @RequestBody EventStat30mForm form) {
        this.eventStat30mService.create(form);
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
    @ApiOperationSupport(order = 5)
    public EventStat30mDTO detail(@PathVariable Long id) {
        return this.eventStat30mService.detail(id);
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
    @ApiOperationSupport(order = 6)
    public void edit(@PathVariable Long id, @Validated( {UpdateGroup.class, Default.class}) @RequestBody EventStat30mForm form) {
        BaseCodes.PARAM_VERIFY_ERROR.isTrue(id.equals(form.getId()), "id 不一致");
        BaseCodes.DATA_ERROR.notNull(eventStat30mService.getById(form.getId()), "指定的数据不存在: " + id);
        this.eventStat30mService.edit(form);
    }

    /**
     * 删除数据
     *
     * @param ids 主键集合
     * @since 1.0.0
     */
    @DeleteMapping
    @Operation(summary = "删除数据")
    @ApiOperationSupport(order = 7)
    public void remove(@RequestBody List<Long> ids) {
        BaseCodes.DATA_ERROR.notEmpty(ids, "带删除的数据标识不能为空");
        this.eventStat30mService.removeByIds(ids);
    }

}
