package com.obs.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sampling_rule")
public class SamplingRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String service;
    private Integer rate;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
