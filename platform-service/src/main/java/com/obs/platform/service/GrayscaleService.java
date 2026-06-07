package com.obs.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obs.platform.common.api.Result;
import com.obs.platform.common.exception.BusinessException;
import com.obs.platform.entity.GrayscaleRule;
import com.obs.platform.mapper.GrayscaleRuleMapper;
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
public class GrayscaleService {

    private static final String REDIS_KEY = "obs:grayscale:rules";

    private final GrayscaleRuleMapper grayscaleRuleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public List<GrayscaleRule> listRules() {
        return grayscaleRuleMapper.selectList(
                new LambdaQueryWrapper<GrayscaleRule>().orderByAsc(GrayscaleRule::getService));
    }

    public Result<Void> saveRule(GrayscaleRule rule) {
        if (rule.getEnabled() == null) rule.setEnabled(true);
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        grayscaleRuleMapper.insert(rule);
        syncToRedis();
        log.info("[Grayscale] 新增灰度规则: service={}, strategy={}", rule.getService(), rule.getStrategy());
        return Result.success();
    }

    public Result<Void> updateRule(GrayscaleRule rule) {
        rule.setUpdateTime(LocalDateTime.now());
        grayscaleRuleMapper.updateById(rule);
        syncToRedis();
        return Result.success();
    }

    public Result<Void> deleteRule(Long id) {
        grayscaleRuleMapper.deleteById(id);
        syncToRedis();
        return Result.success();
    }

    public void syncToRedis() {
        List<GrayscaleRule> enabled = grayscaleRuleMapper.selectList(
                new LambdaQueryWrapper<GrayscaleRule>().eq(GrayscaleRule::getEnabled, true));
        Map<String, String> map = enabled.stream()
                .collect(Collectors.toMap(
                        r -> r.getService() + ":" + r.getId(),
                        r -> r.getStrategy() + "|" + r.getRuleValue() + "|" + r.getTargetVersion()));
        stringRedisTemplate.opsForHash().putAll(REDIS_KEY, map);
        log.info("[Grayscale] 灰度规则已同步到Redis: {} 条", map.size());
    }
}
