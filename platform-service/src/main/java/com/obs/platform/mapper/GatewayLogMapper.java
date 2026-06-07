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

    @Select("SELECT DATE(create_time) label, COUNT(*) value FROM gateway_log WHERE create_time >= #{since} GROUP BY DATE(create_time) ORDER BY label")
    List<java.util.Map<String, Object>> trendSince(@Param("since") String since);

    @Select("SELECT CASE WHEN status_code LIKE '2%' THEN '2xx' WHEN status_code LIKE '4%' THEN '4xx' WHEN status_code LIKE '5%' THEN '5xx' ELSE 'other' END category, COUNT(*) value FROM gateway_log WHERE create_time >= #{since} GROUP BY category")
    List<java.util.Map<String, Object>> statusDistribution(@Param("since") String since);

    @Select("SELECT service label, COUNT(*) value FROM gateway_log WHERE create_time >= #{since} GROUP BY service ORDER BY value DESC LIMIT 10")
    List<java.util.Map<String, Object>> topServices(@Param("since") String since);
}
