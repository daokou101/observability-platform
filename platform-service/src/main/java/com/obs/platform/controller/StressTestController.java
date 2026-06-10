package com.obs.platform.controller;

import com.obs.platform.common.api.Result;
import com.obs.platform.service.StressTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/stress")
@RequiredArgsConstructor
public class StressTestController {

    private final StressTestService stressTestService;

    @PostMapping("/start")
    public Result<Map<String, String>> start(@RequestBody Map<String, Object> config) {
        String taskId = stressTestService.start(config);
        return Result.success(Map.of("taskId", taskId));
    }

    @PostMapping("/stop/{taskId}")
    public Result<Void> stop(@PathVariable String taskId) {
        stressTestService.stop(taskId);
        return Result.success();
    }

    @GetMapping(value = "/subscribe/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String taskId) {
        return stressTestService.subscribe(taskId);
    }

    @GetMapping("/history")
    public Result<Object> history() {
        return Result.success(Map.of("records", stressTestService.getHistory()));
    }
}
