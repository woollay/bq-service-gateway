package com.biuqu.boot.demo.startup.configure;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * 限流配置
 *
 * @author BiuQu
 * @date 2023/3/2 07:55
 */
@Configuration
public class LimitConfigurer
{
    /**
     * 针对接口限流
     *
     * @return 限流对象
     */
    @Bean
    public KeyResolver urlLimit()
    {
        return exchange -> Mono.just(exchange.getRequest().getURI().getPath());
    }
}
