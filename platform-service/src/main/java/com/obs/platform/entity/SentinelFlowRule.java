package com.obs.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sentinel_flow_rule")
public class SentinelFlowRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String resource;
    private String limitApp;
    private Integer grade;
    private Double count;
    private Integer strategy;
    private Integer controlBehavior;
    private Boolean enabled;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
