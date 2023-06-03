package com.biuqu.boot.startup.gateway.aop;

import com.biuqu.aop.BaseAop;
import com.biuqu.boot.common.gateway.constants.GatewayConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Netty Trace log切面
 *
 * @author BiuQu
 * @date 2023/3/12 22:19
 */
@Slf4j
@Component
@Aspect
public class NettyTraceLogAop extends BaseAop
{
    @Before(BEFORE_PATTERN)
    @Override
    public void before(JoinPoint joinPoint)
    {
        super.before(joinPoint);
    }

    @Override
    protected void doBefore(Method method, Object[] args)
    {
        Object webServerObj = args[0];
        if (webServerObj instanceof ServerWebExchange)
        {
            ServerWebExchange exchange = (ServerWebExchange)webServerObj;
            MDCAdapter mdc = MDC.getMDCAdapter();
            Map<String, String> map = mdc.getCopyOfContextMap();
            if (!MapUtils.isEmpty(map))
            {
                //获取并缓存链路信息
                exchange.getAttributes().put(GatewayConst.TRACE_LOG_KEY, map);
                HttpHeaders headers = exchange.getResponse().getHeaders();
                //把链路信息缓存至exchange的response对象header
                for (String traceKey : map.keySet())
                {
                    String value = map.get(traceKey);
                    if (!headers.containsKey(traceKey))
                    {
                        headers.add(traceKey, value);
                    }
                }
            }
            else
            {
                //从缓存中提取并设置给过滤器
                Map<String, String> cachedMap = exchange.getAttribute(GatewayConst.TRACE_LOG_KEY);
                if (!MapUtils.isEmpty(cachedMap))
                {
                    mdc.setContextMap(cachedMap);
                }
            }
        }
    }

    /**
     * 拦截所有过滤器匹配表达式
     */
    private static final String BEFORE_PATTERN = "(execution (* com.biuqu.boot.*.*.filter.*GatewayFilter.filter(..)))";
}
