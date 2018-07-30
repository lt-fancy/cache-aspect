package com.sawallianc.util;

import java.lang.reflect.Method;

public final class CacheKeyUtil {
    private CacheKeyUtil(){

    }

    public static String generateKey(String prefix, Class<?> target, Method method,Object[] params){
        StringBuilder sb = new StringBuilder();
        if(null!=params&&params.length>0){
            Object[] objs = params;
            int length = objs.length;
            for(int i=0;i<length;i++){
                Object object = objs[i];
                sb.append(object).append(":");
            }
        }
        return new StringBuilder(prefix).append(":").append(target.getSimpleName()).append(":").append(method.getName()).append(":").append(sb).toString();
    }
}
