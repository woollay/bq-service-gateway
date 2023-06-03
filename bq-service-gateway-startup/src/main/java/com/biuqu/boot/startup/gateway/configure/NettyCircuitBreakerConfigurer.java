package com.biuqu.boot.startup.gateway.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 熔断降级配置器
 *
 * @author BiuQu
 * @date 2023/3/19 06:58
 */
@Slf4j
@Configuration
public class NettyCircuitBreakerConfigurer
{
    /**
     * 集成了Resilience4j过滤器
     *
     * @param circuitRegistry 服务降级熔断配置
     * @param timeRegistry    服务慢请求配置
     * @return 熔断降级的filter工厂
     */
    @Bean
    public ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory(CircuitBreakerRegistry circuitRegistry,
        TimeLimiterRegistry timeRegistry)
    {
        ReactiveResilience4JCircuitBreakerFactory factory =
            new ReactiveResilience4JCircuitBreakerFactory(circuitRegistry, timeRegistry);
        factory.configureDefault(id ->
        {
            CircuitBreakerConfig circuitConf = circuitRegistry.getDefaultConfig();
            TimeLimiterConfig timeConf = timeRegistry.getDefaultConfig();
            log.info("CircuitBreaker[{}]config:{}/{}", id, circuitConf, timeConf.getTimeoutDuration().getSeconds());
            Resilience4JConfigBuilder builder = new Resilience4JConfigBuilder(id);
            //此处仅构建了一个默认的熔断策略(还可以继续添加)
            return builder.circuitBreakerConfig(circuitConf).timeLimiterConfig(timeConf).build();
        });
        return factory;
    }
}
