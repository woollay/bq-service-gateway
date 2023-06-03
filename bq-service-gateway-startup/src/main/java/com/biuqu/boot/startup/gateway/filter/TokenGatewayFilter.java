package com.biuqu.boot.startup.gateway.filter;

import com.biuqu.boot.common.gateway.constants.GatewayConst;
import com.biuqu.boot.common.gateway.utils.ServerUtil;
import com.biuqu.boot.constants.CommonBootConst;
import com.biuqu.boot.service.SecurityUrlService;
import com.biuqu.boot.startup.gateway.server.FluxRequestWrapper;
import com.biuqu.errcode.ErrCodeEnum;
import com.biuqu.jwt.PubJwtMgr;
import com.biuqu.model.Channel;
import com.biuqu.model.JwtToken;
import com.biuqu.utils.JsonUtil;
import com.biuqu.utils.JwtUtil;
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
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Set;

/**
 * Token鉴权过滤器
 * <p>
 * 支持内部请求免JwtToken请求
 *
 * @author BiuQu
 * @date 2023/3/14 11:58
 */
@Slf4j
@Component
public class TokenGatewayFilter implements GlobalFilter, Ordered
{
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        String url = request.getURI().getPath();
        PathMatcher pathMatcher = new AntPathMatcher();
        boolean ignore = this.whitelist.stream().anyMatch(pattern -> pathMatcher.match(pattern, url));
        log.info("url:{},whitelist:{},result:{}", url, JsonUtil.toJson(this.whitelist), ignore);
        if (!ignore)
        {
            String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            boolean refreshType = false;
            //判断是否为token接口
            if (pathMatcher.match(jwtChannel.getUrl(), url))
            {
                String grantType = request.getQueryParams().getFirst("grant_type");
                log.info("url[{}]'s  grant type:{}", url, grantType);
                refreshType = "jwt_refresh".equalsIgnoreCase(grantType);
                //不是刷新token接口调用时，就认定是申请token接口，直接放过
                if (!refreshType)
                {
                    return chain.filter(exchange);
                }
            }

            if (urlService.isEncUrl(request.getQueryParams()))
            {
                String decUrl = urlService.decryptUrl(url);
                log.info("enc url:{},and dec url:{}", url, decUrl);
                if (!StringUtils.isEmpty(decUrl))
                {
                    log.info("start request url:{} without checking token.", decUrl);
                    ServerHttpRequest requestWrapper = FluxRequestWrapper.wrap(request, decUrl, null);
                    return chain.filter(exchange.mutate().request(requestWrapper).build());
                }
            }

            boolean result = jwtMgr.valid(authorization);
            log.info("token[{}] valid result:{}", authorization, result);
            if (!result)
            {
                log.error("token auth failed.");
                return ServerUtil.writeErr(exchange, ErrCodeEnum.SIGNATURE_ERROR.getCode(), snakeCase);
            }

            JwtToken jwtToken = JwtUtil.getJwtToken(authorization);
            if (null == jwtToken)
            {
                log.error("parse token failed.");
                return ServerUtil.writeErr(exchange, ErrCodeEnum.SIGNATURE_ERROR.getCode(), snakeCase);
            }

            //token才能请求业务接口
            boolean validBizType = !refreshType && !jwtToken.isRefresh();
            //刷新token只能请求刷新token
            boolean validRefreshType = refreshType && jwtToken.isRefresh();
            if (!validBizType && !validRefreshType)
            {
                log.error("[{}]token type not matched.", url);
                return ServerUtil.writeErr(exchange, ErrCodeEnum.SIGNATURE_ERROR.getCode(), snakeCase);
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder()
    {
        return -10;
    }

    /**
     * 是否驼峰式json(默认支持)
     */
    @Value("${bq.json.snake-case:true}")
    private boolean snakeCase;

    /**
     * 安全url服务
     */
    @Autowired
    private SecurityUrlService urlService;

    /**
     * 不用做鉴权的白名单
     */
    @Resource(name = GatewayConst.WHITELIST)
    private Set<String> whitelist;

    /**
     * 注入jwt管理器
     */
    @Autowired
    private PubJwtMgr jwtMgr;

    /**
     * jwt配置
     */
    @Resource(name = CommonBootConst.JWT_CHANNEL_CONFIG)
    private Channel jwtChannel;
}
