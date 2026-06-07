package com.obs.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.obs.platform.entity.GatewayLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface GatewayLogMapper extends BaseMapper<GatewayLog> {

    @Select("SELECT DISTINCT service FROM gateway_log WHERE create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)")
    List<String> listActiveServices();

    @Select("SELECT COUNT(*) FROM gateway_log WHERE create_time >= #{since}")
    long countSince(@Param("since") String since);

    @Select("SELECT COALESCE(AVG(duration), 0) FROM gateway_log WHERE create_time >= #{since}")
    double avgDurationSince(@Param("since") String since);

    @Select("SELECT COUNT(*) FROM gateway_log WHERE status_code >= 500 AND create_time >= #{since}")
    long countErrorsSince(@Param("since") String since);
}
