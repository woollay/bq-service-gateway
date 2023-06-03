package com.biuqu.boot.startup.gateway.server;

import com.biuqu.boot.common.gateway.utils.FluxUtil;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 基础的ServerHttpResponse对象
 *
 * @author BiuQu
 * @date 2023/3/10 11:28
 */
@Slf4j
public class FluxResponseWrapper extends ServerHttpResponseDecorator
{
    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body)
    {
        log.info("start to response data.");
        if (body instanceof Flux)
        {
            Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>)body;
            //[TCP粘包拆包]这个body是多次写入的，需要转换成fluxBody并利用其中的buffer来接受完整的body
            return super.writeWith(fluxBody.buffer().map(dataBuffers ->
            {
                DataBuffer dataBuffer = bufferFactory().join(dataBuffers);
                byte[] data = FluxUtil.fromFlux(dataBuffer);
                String result = new String(data, StandardCharsets.UTF_8);
                log.info("response body:{}", result);
                byte[] newData = doService(data);
                return bufferFactory().wrap(newData);
            }));
        }
        return super.writeWith(body);
    }

    public FluxResponseWrapper(ServerHttpResponse delegate)
    {
        super(delegate);
    }

    /**
     * 做业务处理
     *
     * @param data 响应body数据
     * @return 业务处理后的新数据
     */
    protected byte[] doService(byte[] data)
    {
        return data;
    }
}
