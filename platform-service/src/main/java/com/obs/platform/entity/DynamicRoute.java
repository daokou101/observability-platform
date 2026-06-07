package com.obs.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("dynamic_route")
public class DynamicRoute {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String routeId;
    private String uri;
    private String predicates;
    private String filters;
    private Integer order;
    private Boolean enabled;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
