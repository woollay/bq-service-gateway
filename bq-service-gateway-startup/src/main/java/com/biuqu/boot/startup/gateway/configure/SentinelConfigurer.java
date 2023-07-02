package com.biuqu.boot.startup.gateway.configure;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.biuqu.errcode.ErrCodeEnum;
import com.biuqu.model.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Alibaba Sentinel熔断降级配置
 *
 * @author BiuQu
 * @date 2023/6/29 19:20
 */
@Slf4j
@Configuration
public class SentinelConfigurer
{
    /**
     * 定义网关异常时的处理器(使用自定义的错误码)
     *
     * @return 熔断降级异常时的处理器
     */
    @Bean
    public BlockRequestHandler blockRequestHandler()
    {
        return (exchange, e) ->
        {
            log.error("happened block exception.", e);
            ResultCode<?> resultCode = ResultCode.error(ErrCodeEnum.SERVER_ERROR.getCode());
            int httpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
            ServerResponse.BodyBuilder bodyBuilder = ServerResponse.status(httpCode);
            bodyBuilder.contentType(MediaType.APPLICATION_JSON);
            return bodyBuilder.body(BodyInserters.fromValue(resultCode));
        };
    }
}
