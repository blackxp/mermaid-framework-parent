package com.mermaid.framework.redis.impl;

import com.mermaid.framework.redis.RedisCacheService;
import com.mermaid.framework.redis.RedisDistributedLockService;
import com.mermaid.framework.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonObjectSerializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class DefaultRedisService implements RedisCacheService,RedisDistributedLockService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRedisService.class);
    private static Map<String,Object> operationsMap = new HashMap<>();

    private static final String STRING_TYPE="STRING_TYPE";
    private static final String SET_TYPE="SET_TYPE";
    private static final String HASH_TYPE="HASH_TYPE";
    private static final String ZSET_TYPE="ZSET_TYPE";
    private static final String LIST_TYPE="LIST_TYPE";
    private static final String HYPERLOG_TYPE="HYPERLOG_TYPE";
    /**
     * 服务名
     */
    private String name;

    private RedisTemplate redisTemplate;
    public DefaultRedisService(String name) {
        this(name,null);
    }

    public DefaultRedisService(String name, RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.setName(name);
        initOperationsMap();
    }

    private void initOperationsMap() {
        ValueOperations valueOperations = this.redisTemplate.opsForValue();
        SetOperations setOperations = this.redisTemplate.opsForSet();
        HashOperations hashOperations = this.redisTemplate.opsForHash();
        HyperLogLogOperations hyperLogLogOperations = this.redisTemplate.opsForHyperLogLog();
        ZSetOperations zSetOperations = this.redisTemplate.opsForZSet();
        ListOperations listOperations = this.redisTemplate.opsForList();

        operationsMap.put(STRING_TYPE,valueOperations);
        operationsMap.put(SET_TYPE,setOperations);
        operationsMap.put(HASH_TYPE,hashOperations);
        operationsMap.put(ZSET_TYPE,zSetOperations);
        operationsMap.put(HYPERLOG_TYPE,hyperLogLogOperations);
        operationsMap.put(LIST_TYPE,listOperations);
    }

    @Override
    public void set(String key, Object value) {
        set(key,value,0l);
    }

    @Override
    public void set(String key, Object value, long expire) {
        try {
            if(null == value) {
                delete(key);
            }else {
                ValueOperations operations = getOperation(STRING_TYPE);
                operations.set(key,value,expire);
            }
        } catch (Exception e) {
            logger.error("redis set error",e);
        }
    }

    private ValueOperations getOperation(String stringType) {
        return (ValueOperations) operationsMap.get(stringType);
    }

    @Override
    public Set<String> keys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys;
    }

    @Override
    public String get(String key) {
        return get(key,String.class);
    }

    @Override
    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    public <T> T get(String key, Class<T> tpl) {
        if(!StringUtils.hasText(key)) {
                return null;
        }
        ValueOperations operations = (ValueOperations) operationsMap.get(STRING_TYPE);
        Object ret = operations.get(key);
        return (T) ret;
    }

    @Override
    public void delete(String... keys) {
        redisTemplate.delete(keys);
    }

    @Override
    public void expire(String key, long expire) {
        redisTemplate.expire(key,expire, TimeUnit.SECONDS);
    }

    @Override
    public boolean lock(String lockName) {
        return false;
    }

    @Override
    public boolean lock(String lockName, long expire) {
        return false;
    }

    @Override
    public void unlock(String lockName) {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if(!StringUtils.hasText(name)) {
            name = DefaultRedisService.class.getSimpleName();
        }
        this.name = name;
    }
}
