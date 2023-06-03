package com.biuqu.boot.startup.gateway.filter;

import com.biuqu.boot.common.gateway.constants.GatewayConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 清理网关
 * <p>
 * 优先级最高的网关，保证无论任何返回都会清理缓存
 *
 * @author BiuQu
 * @date 2023/3/1 08:53
 */
@Slf4j
@Component
public class RemovingGatewayFilter implements GlobalFilter, Ordered
{
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        //从缓存中提取并设置给过滤器
        Map<String, String> cachedMap = exchange.getAttribute(GatewayConst.TRACE_LOG_KEY);
        return chain.filter(exchange).doFinally(s ->
        {
            if (!MapUtils.isEmpty(cachedMap))
            {
                MDC.getMDCAdapter().setContextMap(cachedMap);
            }

            long start = System.currentTimeMillis();
            Map<String, Object> attributes = exchange.getAttributes();
            if (attributes.containsKey(GatewayConst.BODY_CACHE_KEY))
            {
                String body = attributes.remove(GatewayConst.BODY_CACHE_KEY).toString();
                log.info("clear cached body now:{}", body);
            }
            if (attributes.containsKey(GatewayConst.START_CACHE_KEY))
            {
                start = (long)attributes.remove(GatewayConst.START_CACHE_KEY);
            }
            if (attributes.containsKey(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR))
            {
                attributes.remove(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
            }
            if (attributes.containsKey(GatewayConst.TRACE_LOG_KEY))
            {
                attributes.remove(GatewayConst.TRACE_LOG_KEY);
            }
            log.info("finally cost:{}ms", System.currentTimeMillis() - start);
            MDC.getMDCAdapter().clear();
        });
    }

    @Override
    public int getOrder()
    {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
