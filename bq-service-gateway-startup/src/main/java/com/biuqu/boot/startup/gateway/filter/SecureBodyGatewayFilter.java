package com.biuqu.boot.startup.gateway.filter;

import com.biuqu.boot.common.gateway.constants.GatewayConst;
import com.biuqu.boot.common.gateway.model.EncParam;
import com.biuqu.boot.common.gateway.model.EncResult;
import com.biuqu.boot.common.gateway.model.EncryptConfig;
import com.biuqu.boot.common.gateway.utils.ServerUtil;
import com.biuqu.boot.startup.gateway.server.FluxRequestWrapper;
import com.biuqu.boot.startup.gateway.server.FluxResponseWrapper;
import com.biuqu.errcode.ErrCodeEnum;
import com.biuqu.model.ResultCode;
import com.biuqu.security.ClientSecurity;
import com.biuqu.utils.JsonUtil;
import com.biuqu.utils.UrlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 安全过滤器(支持参数摘要校验和报文内容加解密)
 *
 * @author BiuQu
 * @date 2023/3/10 09:26
 */
@Slf4j
@Component
public class SecureBodyGatewayFilter implements GlobalFilter, Ordered
{
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        //1.解析出该请求的摘要配置和加密配置
        ServerHttpRequest request = exchange.getRequest();
        String url = request.getURI().getPath();
        EncryptConfig encryptConf = this.match(url);

        //2.没有加密配置则直接放过请求
        if (null == encryptConf)
        {
            return chain.filter(exchange);
        }

        //3.缓存加密参数配置至全局缓存中,并构造响应对象随时接收响应结果并加密
        String encId = request.getHeaders().getFirst(GatewayConst.HEADER_ENC_ID);
        ServerHttpResponse response = exchange.getResponse();
        if (encryptConf.needEnc())
        {
            exchange.getAttributes().put(GatewayConst.ENC_RESPONSE_ALG_KEY, encryptConf.getEnc());
            if (StringUtils.isEmpty(encId))
            {
                encId = encryptConf.getEnc();
            }
            final String encAlg = encId;
            response = new FluxResponseWrapper(exchange.getResponse())
            {
                @Override
                protected byte[] doService(byte[] data)
                {
                    //明文的业务结果
                    String result = new String(data, StandardCharsets.UTF_8);
                    log.info("before encrypt response body:{}", result);
                    EncResult encResult = new EncResult();
                    encResult.setResult(clientEncryptor.encrypt(encAlg, result));
                    String encJson = JsonUtil.toJson(encResult, snakeCase);
                    log.info("after encrypt response body:{}", encJson);
                    return encJson.getBytes(StandardCharsets.UTF_8);
                }
            };
        }

        String body = exchange.getAttribute(GatewayConst.BODY_CACHE_KEY);
        //4.对请求数据做解密(包括替换请求body数据,替换请求header的body长度)
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        if (encryptConf.needDec())
        {
            if (StringUtils.isEmpty(encId))
            {
                encId = encryptConf.getDec();
            }
            log.info("**[{}]body encrypted[{}]={}", url, body, clientEncryptor.encrypt(encId, body));
            EncParam param = JsonUtil.toObject(body, EncParam.class, snakeCase);
            if (null == param || StringUtils.isEmpty(param.getParam()))
            {
                log.error("[{}]decrypt data error.", url);
                return this.writeSecErr(exchange, encId, ErrCodeEnum.VALID_ERROR.getCode(), snakeCase);
            }
            String decBody = clientEncryptor.decrypt(encId, param.getParam());
            log.info("[{}]decrypt body:{}", url, decBody);
            data = decBody.getBytes(StandardCharsets.UTF_8);
        }
        ServerHttpRequest requestWrapper = FluxRequestWrapper.wrap(request, encryptConf.getRedirect(), data);
        return chain.filter(exchange.mutate().request(requestWrapper).response(response).build());
    }

    @Override
    public int getOrder()
    {
        return -50;
    }

    /**
     * 回写异常结果
     *
     * @param exchange server对象(包含request和response)
     * @param encAlg   加密算法
     * @param code     错误码
     * @param snake    驼峰转换
     * @return 标准的异常结果对象
     * @secMgr 本地秘钥的加密服务
     */
    private Mono<Void> writeSecErr(ServerWebExchange exchange, String encAlg, String code, boolean snake)
    {
        //1.构造常规的返回结果json
        ResultCode<?> resultCode = ResultCode.error(code);
        long start = Long.parseLong(exchange.getAttribute(GatewayConst.START_CACHE_KEY).toString());
        resultCode.setCost(System.currentTimeMillis() - start);
        String json = JsonUtil.toJson(resultCode, snake);

        //2.如果设置了返回结果加密时，则要先对返回结果json加密
        String enc = exchange.getAttribute(GatewayConst.ENC_RESPONSE_ALG_KEY);
        if (!StringUtils.isEmpty(enc))
        {
            EncResult encResult = new EncResult();
            encResult.setResult(clientEncryptor.encrypt(encAlg, json));
            json = JsonUtil.toJson(encResult, snake);
        }

        //3.构造最终的json返回结果
        return ServerUtil.writeErr(exchange, json);
    }

    /**
     * 获取请求的加解密配置参数
     *
     * @param url 请求的url
     * @return 该请求的加解密配置
     */
    private EncryptConfig match(String url)
    {
        for (EncryptConfig encConf : this.rules)
        {
            if (encConf.getUrl().equalsIgnoreCase(UrlUtil.shortUrl(url)))
            {
                return encConf;
            }
        }
        return null;
    }

    /**
     * 加密规则配置
     */
    @Resource(name = GatewayConst.ENCRYPT_RULE)
    private List<EncryptConfig> rules;

    /**
     * 是否驼峰式json(默认支持)
     */
    @Value("${bq.json.snake-case:true}")
    private boolean snakeCase;

    /**
     * 注入安全服务服务
     */
    @Autowired
    private ClientSecurity clientEncryptor;
}
