package com.biuqu.boot.startup.gateway.filter;

import com.biuqu.boot.common.gateway.constants.GatewayConst;
import com.biuqu.boot.common.gateway.model.EncryptConfig;
import com.biuqu.boot.common.gateway.utils.ServerUtil;
import com.biuqu.boot.startup.gateway.server.FluxRequestWrapper;
import com.biuqu.errcode.ErrCodeEnum;
import com.biuqu.security.ClientSecurity;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 增强的认证过滤器
 *
 * @author BiuQu
 * @date 2023/5/11 08:15
 */
@Slf4j
@Component
public class SecureAuthGatewayFilter implements GlobalFilter, Ordered
{
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        //解析出该请求的摘要配置和加密配置
        ServerHttpRequest request = exchange.getRequest();
        String url = request.getURI().getPath();

        //配置转发后，对header中的认证头做校验和解密
        String encId = request.getHeaders().getFirst(GatewayConst.HEADER_ENC_ID);
        if (authConf.getUrl().equals(url) && this.authConf.needDec())
        {
            String encAlg = encId;
            if (StringUtils.isEmpty(encAlg))
            {
                encAlg = this.authConf.getDec();
            }

            String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            log.info("current auth encrypt[{}][{}]=[{}].", encAlg, authorization,
                clientEncryptor.encrypt(encAlg, authorization));
            String decAuth = clientEncryptor.decrypt(encAlg, authorization);
            if (StringUtils.isEmpty(decAuth))
            {
                log.error("[{}]decrypt auth header failed.", url);
                return ServerUtil.writeErr(exchange, ErrCodeEnum.SIGNATURE_ERROR.getCode(), snakeCase);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.put(HttpHeaders.AUTHORIZATION, Lists.newArrayList(decAuth));

            String body = exchange.getAttribute(GatewayConst.BODY_CACHE_KEY);
            if (StringUtils.isEmpty(body))
            {
                body = StringUtils.EMPTY;
            }
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            request = FluxRequestWrapper.wrap(request, authConf.getRedirect(), headers, data);
        }
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder()
    {
        return -30;
    }

    /**
     * 是否驼峰式json(默认支持)
     */
    @Value("${bq.json.snake-case:true}")
    private boolean snakeCase;

    /**
     * 认证配置
     */
    @Autowired
    private EncryptConfig authConf;

    /**
     * 注入安全服务服务
     */
    @Autowired
    private ClientSecurity clientEncryptor;
}
