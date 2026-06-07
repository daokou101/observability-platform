package com.obs.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("alert_rule")
public class AlertRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleName;
    private String service;
    private String metric;
    private Double threshold;
    private Integer windowMinutes;
    private Boolean enabled;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
