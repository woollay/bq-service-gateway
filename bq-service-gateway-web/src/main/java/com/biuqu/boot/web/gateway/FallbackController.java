package com.biuqu.boot.web.gateway;

import com.biuqu.errcode.ErrCodeEnum;
import com.biuqu.model.ResultCode;
import com.biuqu.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import reactor.core.publisher.Mono;

/**
 * 服务降级Rest
 *
 * @author BiuQu
 * @date 2023/2/12 10:46
 */
@Slf4j
@RestController
public class FallbackController
{
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @PostMapping("/fallback")
    public Mono<ResultCode<?>> fallback(ServerWebExchange exchange)
    {
        Exception e = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        ServerWebExchange delegate = exchange;
        if (exchange instanceof ServerWebExchangeDecorator)
        {
            delegate = ((ServerWebExchangeDecorator)exchange).getDelegate();
        }
        String url = delegate.getRequest().getURI().getPath();
        ResultCode<?> resultCode = ResultCode.error(ErrCodeEnum.SERVER_ERROR.getCode());
        log.error("[{}]circuit breaker result:{},with exception:{}", url, JsonUtil.toJson(resultCode), e);
        return Mono.just(resultCode);
    }
}
