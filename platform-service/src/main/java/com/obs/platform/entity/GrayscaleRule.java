package com.obs.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("grayscale_rule")
public class GrayscaleRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String service;
    private String strategy;
    private String ruleValue;
    private String targetVersion;
    private Boolean enabled;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
