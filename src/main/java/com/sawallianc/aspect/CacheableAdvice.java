package com.sawallianc.aspect;

import com.sawallianc.annotation.Cacheable;
import com.sawallianc.redis.operations.RedisValueOperations;
import com.sawallianc.util.CacheKeyUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

@Aspect
@Component
public class CacheableAdvice {
    private final static Logger LOGGER = LoggerFactory.getLogger(CacheableAdvice.class);
    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private RedisValueOperations operations;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    public CacheableAdvice() {
    }
    @Pointcut("@annotation(cacheable)")
    public void cache(Cacheable cacheable){

    }

    @Around("cache(cacheable)")
    public Object proceed(ProceedingJoinPoint joinPoint,Cacheable cacheable) throws Throwable{
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        String key = this.generateKey(cacheable,joinPoint,method);
        Object obj = null;
        try {
            obj = this.getFromCache(method,key);
        } catch (Exception e) {
            LOGGER.error("get from redis error: {}",e.getMessage());
        }
        if(null == obj){
            obj = this.setCache(cacheable,joinPoint,key);
        }
        return obj;
    }

    private String generateKey(Cacheable cacheable,ProceedingJoinPoint joinPoint,Method method){
        return CacheKeyUtil.generateKey(applicationName,joinPoint.getTarget().getClass(),method,joinPoint.getArgs());
    }

    private Object setCache(Cacheable cacheable,ProceedingJoinPoint joinPoint,String key) throws Throwable {
        Object obj = joinPoint.proceed(joinPoint.getArgs());
        if(null!=obj){
            this.operations.set(key,obj,cacheable.expireTime());
        }
        return obj;
    }

    private Object getFromCache(Method method,String key) throws ClassNotFoundException {
        if(method.getReturnType().getName().equals(List.class.getName())){
            String typeName = method.getGenericReturnType().getTypeName();
            String genericReturnType = typeName.substring(typeName.indexOf("<")+1,typeName.indexOf(">"));
            return this.operations.getArray(key,Class.forName(genericReturnType));
        } else {
            return this.operations.get(key,method.getReturnType());
        }
    }
}
