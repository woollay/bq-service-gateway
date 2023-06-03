package com.biuqu.boot.common.gateway.utils;

import com.biuqu.boot.common.gateway.constants.GatewayConst;
import com.biuqu.model.ResultCode;
import com.biuqu.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 服务端工具类
 *
 * @author BiuQu
 * @date 2023/3/5 13:31
 */
@Slf4j
public final class ServerUtil
{
    /**
     * 回写异常结果
     *
     * @param exchange server对象(包含request和response)
     * @param json     带错误码的resultCode json
     * @return 标准的异常结果对象
     */
    public static Mono<Void> writeErr(ServerWebExchange exchange, String json)
    {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        MediaType utf8Type = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, utf8Type.toString());
        DataBuffer dataBuffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Flux.just(dataBuffer));
    }

    /**
     * 回写异常结果
     *
     * @param exchange server对象(包含request和response)
     * @param code     错误码
     * @param snake    驼峰转换
     * @return 标准的异常结果对象
     */
    public static Mono<Void> writeErr(ServerWebExchange exchange, String code, boolean snake)
    {
        ResultCode<?> resultCode = ResultCode.error(code);
        long start = Long.parseLong(exchange.getAttribute(GatewayConst.START_CACHE_KEY).toString());
        resultCode.setCost(System.currentTimeMillis() - start);
        String json = JsonUtil.toJson(resultCode, snake);

        return writeErr(exchange, json);
    }

    private ServerUtil()
    {
    }
}
