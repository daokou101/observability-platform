package com.obs.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("alert_log")
public class AlertLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ruleId;
    private String ruleName;
    private String service;
    private String metric;
    private Double currentValue;
    private Double threshold;
    private String message;
    private LocalDateTime createTime;
}
