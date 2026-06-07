package com.obs.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obs.platform.common.api.Result;
import com.obs.platform.common.constant.TraceConstant;
import com.obs.platform.entity.SamplingRule;
import com.obs.platform.mapper.SamplingRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SamplingService {

    private final SamplingRuleMapper samplingRuleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public List<SamplingRule> listRules() {
        return samplingRuleMapper.selectList(
                new LambdaQueryWrapper<SamplingRule>().orderByAsc(SamplingRule::getService));
    }

    public Result<Void> saveOrUpdate(SamplingRule rule) {
        if (rule.getId() != null) {
            rule.setUpdateTime(LocalDateTime.now());
            samplingRuleMapper.updateById(rule);
        } else {
            rule.setCreateTime(LocalDateTime.now());
            rule.setUpdateTime(LocalDateTime.now());
            samplingRuleMapper.insert(rule);
        }
        syncToRedis();
        return Result.success();
    }

    public Result<Void> delete(Long id) {
        samplingRuleMapper.deleteById(id);
        syncToRedis();
        return Result.success();
    }

    public void syncToRedis() {
        List<SamplingRule> rules = samplingRuleMapper.selectList(null);
        Map<String, String> map = rules.stream()
                .collect(Collectors.toMap(SamplingRule::getService,
                        r -> String.valueOf(r.getRate())));
        stringRedisTemplate.opsForHash().putAll(TraceConstant.SAMPLING_KEY, map);
        log.info("[Sampling] 采样规则已同步到Redis: {}", map);
    }
}
