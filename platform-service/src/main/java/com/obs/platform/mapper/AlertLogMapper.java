package com.obs.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.obs.platform.entity.AlertLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AlertLogMapper extends BaseMapper<AlertLog> {

    @Select("SELECT COUNT(*) FROM alert_log WHERE create_time >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE)")
    long countRecent(@Param("minutes") int minutes);
}
