package com.equestrian.club.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.equestrian.club.common.ApiResponse;
import com.equestrian.club.dto.HealthClearRequest;
import com.equestrian.club.dto.HealthEventCreateRequest;
import com.equestrian.club.dto.HealthTransitionRequest;
import com.equestrian.club.dto.view.ClearanceResultView;
import com.equestrian.club.dto.view.HealthEventView;
import com.equestrian.club.dto.view.HorseHealthOverviewView;
import com.equestrian.club.service.HealthEventService;

/**
 * 马匹健康事件处置台：登记 / 流转 / 复查 / 复训放行 + 列表筛选 + 马匹健康概览。
 * 权限与并发全部由后端把关，前端隐藏按钮只是体验优化。
 */
@RestController
@RequestMapping("/api/health")
public class HealthEventController {

    private final HealthEventService healthEventService;

    public HealthEventController(HealthEventService healthEventService) {
        this.healthEventService = healthEventService;
    }

    /** 事件列表：horseId 按马、status 按状态、overdue=true 只看逾期 */
    @GetMapping("/events")
    public ApiResponse<List<HealthEventView>> list(
            @RequestParam(required = false) Long horseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean overdue) {
        return ApiResponse.success(healthEventService.list(horseId, status, overdue));
    }

    @GetMapping("/events/{id}")
    public ApiResponse<HealthEventView> detail(@PathVariable Long id) {
        return ApiResponse.success(healthEventService.detail(id));
    }

    /** 马匹详情：未闭环事件 + 最近一次复查结论 + 放行条件（值班人员一屏看风险） */
    @GetMapping("/horses/{horseId}/overview")
    public ApiResponse<HorseHealthOverviewView> overview(
            @PathVariable Long horseId,
            @RequestParam(required = false) String role) {
        return ApiResponse.success(healthEventService.overview(horseId, role));
    }

    @PostMapping("/events")
    public ApiResponse<HealthEventView> register(@RequestBody HealthEventCreateRequest request) {
        return ApiResponse.success("健康事件已登记", healthEventService.register(request));
    }

    @PostMapping("/events/{id}/transitions")
    public ApiResponse<HealthEventView> transition(
            @PathVariable Long id, @RequestBody HealthTransitionRequest request) {
        return ApiResponse.success("处置已记录", healthEventService.transition(id, request));
    }

    /** 复训放行：负责人专属，通过后马匹恢复在役、所有未关闭事件关闭 */
    @PostMapping("/clearance")
    public ApiResponse<ClearanceResultView> clear(@RequestBody HealthClearRequest request) {
        ClearanceResultView result = healthEventService.clear(request);
        return ApiResponse.success(result.message(), result);
    }
}
