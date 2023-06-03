package com.biuqu.boot.startup.gateway.filter;

import com.biuqu.boot.common.gateway.constants.GatewayConst;
import com.biuqu.boot.common.gateway.utils.FluxUtil;
import com.biuqu.boot.startup.gateway.server.FluxRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 接入网关
 * <p>
 * 1.网关过滤启用顺序:
 * AccessGatewayFilter(-100)->SecurityGatewayFilter(-50)->IntegrityCheckGatewayFilter(-40)
 * ->SecureAuthGatewayFilter(-30)->TokenGatewayFilter(-10)->RemovingGatewayFilter(Integer.MIN_VALUE)
 * 2.网关过滤其实看的是Request过滤还是Response过滤,Request过滤同启用顺序,Response过滤顺序则同启用顺序相反;
 * 3.Request过滤顺序(对请求参数做处理):
 * AccessGatewayFilter->SecurityGatewayFilter->IntegrityCheckGatewayFilter->SecureAuthGatewayFilter->TokenGatewayFilter
 * 4.Response执行顺序(对响应报文做处理):
 * RemovingGatewayFilter<-SecurityGatewayFilter
 *
 * @author BiuQu
 * @date 2023/3/1 08:37
 */
@Slf4j
@Component
public class AccessGatewayFilter implements GlobalFilter, Ordered
{
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        long start = System.currentTimeMillis();
        exchange.getAttributes().put(GatewayConst.START_CACHE_KEY, start);

        //1.从缓存中获取缓存的body(参见yaml中配置的CacheRequestBody过滤器),注意此处的缓存对象仅为body的引用，还是只能读1次
        Object cacheObj = exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
        byte[] data = null;
        //如果ContentType是application/json,则缓存对象是NettyDataBuffer类型
        if (cacheObj instanceof NettyDataBuffer)
        {
            NettyDataBuffer cacheBody = (NettyDataBuffer)cacheObj;
            data = FluxUtil.fromFlux(cacheBody);
        }
        else if (cacheObj instanceof String)
        {
            data = cacheObj.toString().getBytes(StandardCharsets.UTF_8);
        }

        ServerWebExchange exchangeWrapper = exchange;
        if (null != data)
        {
            String cache = new String(data, StandardCharsets.UTF_8);
            log.info("current cache body:{}", cache);

            //2.读取完毕后，还需要再把数据回写至流对象中
            ServerHttpRequest request = FluxRequestWrapper.wrap(exchange.getRequest(), data);

            //3.构造新的ServerExchange对象接收新构造的request对象
            exchangeWrapper = exchange.mutate().request(request).build();

            //4.直接统一缓存新的body对象(后面就不用再重新读取并回填了)
            exchangeWrapper.getAttributes().put(GatewayConst.BODY_CACHE_KEY, cache);
        }

        return chain.filter(exchangeWrapper);
    }

    @Override
    public int getOrder()
    {
        //必须要在缓存body之后
        return -100;
    }
}
