package com.biuqu.boot.common.gateway.utils;

import com.biuqu.utils.FileUtil;
import io.netty.buffer.ByteBufAllocator;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import reactor.core.publisher.Flux;

/**
 * Flux工具类
 *
 * @author BiuQu
 * @date 2023/3/4 11:21
 */
public final class FluxUtil
{
    /**
     * 把数据转换成Flux对象
     *
     * @param data 数据二进制
     * @return flux对象
     */
    public static Flux<DataBuffer> toFlux(byte[] data)
    {
        NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
        DataBuffer dataBuffer = nettyDataBufferFactory.allocateBuffer(data.length);
        dataBuffer.write(data);
        Flux<DataBuffer> dataFlux = Flux.just(dataBuffer);
        return dataFlux;
    }

    /**
     * 从WebFlux流对象转成二进制对象(效果同{@link FluxUtil#fromFlux(DataBuffer)})
     *
     * @param dataBuffer 流对象(读取完毕后，会自动关闭)
     * @return 二进制对象
     */
    public static byte[] fromStream(DataBuffer dataBuffer)
    {
        return FileUtil.read(dataBuffer.asInputStream());
    }

    /**
     * 从WebFlux流对象转成二进制对象
     *
     * @param dataBuffer 流对象(读取完毕后，会自动关闭)
     * @return 二进制对象
     */
    public static byte[] fromFlux(DataBuffer dataBuffer)
    {
        byte[] data = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(data);
        DataBufferUtils.release(dataBuffer);
        return data;
    }

    private FluxUtil()
    {
    }
}
