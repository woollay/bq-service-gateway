package com.biuqu.boot.startup.gateway.configure;

import com.biuqu.json.JsonMappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Webflux缓存配置
 *
 * @author BiuQu
 * @date 2023/3/1 09:18
 */
@Slf4j
@Configuration
public class FluxConfigurer implements WebFluxConfigurer
{
    @Bean
    public ServerCodecConfigurer.ServerDefaultCodecs defaultCodecs(ServerCodecConfigurer configurer)
    {
        ServerCodecConfigurer.ServerDefaultCodecs codecs = configurer.defaultCodecs();
        codecs.maxInMemorySize(maxSize);
        return codecs;
    }

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer)
    {
        configurer.defaultCodecs().maxInMemorySize(maxSize);
        configurer.defaultCodecs().jackson2JsonEncoder(new JsonEncoder(snakeCase));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/static/assets");
    }

    /**
     * WebFlux json转换
     */
    private static class JsonEncoder implements Encoder<Object>
    {
        public JsonEncoder(boolean snake)
        {
            this.snake = snake;
        }

        @Override
        public boolean canEncode(ResolvableType elementType, MimeType mimeType)
        {
            return true;
        }

        @Override
        public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory,
            ResolvableType elementType, MimeType mimeType, Map<String, Object> hints)
        {
            if (inputStream instanceof Mono)
            {
                return Mono.from(inputStream).map(value -> encodeValue(value, bufferFactory)).flux();
            }
            if (inputStream instanceof Flux)
            {
                return Flux.from(inputStream).map(value -> encodeValue(value, bufferFactory));
            }
            return null;
        }

        @Override
        public List<MimeType> getEncodableMimeTypes()
        {
            MimeType mt = MimeTypeUtils.APPLICATION_JSON;
            MimeType mimeType = new MimeType(mt.getType(), mt.getSubtype(), StandardCharsets.UTF_8);
            return Collections.singletonList(mimeType);
        }

        /**
         * 使用系统标准的json处理数据
         *
         * @param value         业务对象
         * @param bufferFactory netty对应的数据处理工厂
         * @return 经过转换后的netty数据类型
         */
        private DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory)
        {
            DataBuffer buffer = bufferFactory.allocateBuffer();
            byte[] bytes = new byte[0];
            try
            {
                bytes = JsonMappers.getMapper(snake).writeValueAsBytes(value);
            }
            catch (JsonProcessingException e)
            {
                log.error("failed to write back json.", e);
            }
            buffer.write(bytes);
            return buffer;
        }

        /**
         * 是否驼峰转换
         */
        private final boolean snake;
    }

    /**
     * 报文的大小限制
     */
    @Value("${spring.codec.max-in-memory-size}")
    private int maxSize;

    /**
     * 是否驼峰式json(默认支持)
     */
    @Value("${bq.json.snake-case:true}")
    private boolean snakeCase;
}
