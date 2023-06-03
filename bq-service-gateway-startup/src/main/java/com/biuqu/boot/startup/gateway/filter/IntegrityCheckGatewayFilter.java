package com.biuqu.boot.startup.gateway.filter;

import com.biuqu.boot.common.gateway.constants.GatewayConst;
import com.biuqu.boot.common.gateway.model.IntegrityCheckConfig;
import com.biuqu.boot.common.gateway.utils.ServerUtil;
import com.biuqu.constants.Const;
import com.biuqu.errcode.ErrCodeEnum;
import com.biuqu.security.facade.SecurityFacade;
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
 * 完整性校验网关
 *
 * @author BiuQu
 * @date 2023/5/11 07:57
 */
@Slf4j
@Component
public class IntegrityCheckGatewayFilter implements GlobalFilter, Ordered
{
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        //1.解析出该请求的摘要配置和加密配置
        ServerHttpRequest request = exchange.getRequest();
        String url = request.getURI().getPath();
        boolean signed = checkConf.needSign(url);
        PathMatcher pathMatcher = new AntPathMatcher();
        boolean ignore = this.whitelist.stream().anyMatch(pattern -> pathMatcher.match(pattern, url));

        //2.没有摘要或者在白名单里面的请求则直接放过请求
        if (!signed || ignore)
        {
            return chain.filter(exchange);
        }

        //3.做完整性校验(使用加密器门面的默认摘要算法)
        String body = exchange.getAttribute(GatewayConst.BODY_CACHE_KEY);
        boolean result = checkIntegrity(request, body);
        if (!result)
        {
            log.error("[{}]check integrity failed.", url);
            return ServerUtil.writeErr(exchange, ErrCodeEnum.SIGNATURE_ERROR.getCode(), snakeCase);
        }
        log.info("[{}]check integrity successfully.", url);
        return chain.filter(exchange);
    }

    /**
     * 使用本地秘钥的默认加密器做摘要验证
     * <p>
     * 拼接header认证头和body: `${Authorization}|${body}`,字段不存在或者为空时，使用空串代替
     *
     * @param request 请求对象
     * @param body    缓存的body
     * @return true表示检验通过
     */
    private boolean checkIntegrity(ServerHttpRequest request, String body)
    {
        String sign = request.getHeaders().getFirst(GatewayConst.HEADER_INTEGRITY);
        if (StringUtils.isEmpty(sign))
        {
            return false;
        }

        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isEmpty(auth))
        {
            auth = StringUtils.EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(auth);

        String encId = request.getHeaders().getFirst(GatewayConst.HEADER_ENC_ID);
        if (StringUtils.isEmpty(encId))
        {
            encId = StringUtils.EMPTY;
        }
        builder.append(Const.JOIN).append(encId);

        if (StringUtils.isEmpty(body))
        {
            body = StringUtils.EMPTY;
        }
        builder.append(Const.JOIN).append(body);
        String integrity = this.securityFacade.hash(builder.toString());
        log.info("current signature:{},src:{}", integrity, sign);
        return sign.equals(integrity);
    }

    @Override
    public int getOrder()
    {
        return -40;
    }

    /**
     * 不用做鉴权的白名单
     */
    @Resource(name = GatewayConst.WHITELIST)
    private Set<String> whitelist;

    /**
     * 注入安全服务服务
     */
    @Autowired
    private SecurityFacade securityFacade;

    /**
     * 注入校验规则
     */
    @Autowired
    private IntegrityCheckConfig checkConf;

    /**
     * 是否驼峰式json(默认支持)
     */
    @Value("${bq.json.snake-case:true}")
    private boolean snakeCase;
}
