package com.biuqu.boot.startup.gateway.server;

import com.biuqu.boot.common.gateway.utils.FluxUtil;
import com.biuqu.utils.UrlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;

/**
 * ServerHttpRequest请求构建器
 *
 * @author BiuQu
 * @date 2023/3/12 10:40
 */
@Slf4j
public final class FluxRequestWrapper
{
    /**
     * 复制一个新Data的ServerHttpRequest
     *
     * @param request 请求对象
     * @param data    新数据
     * @return 新的请求对象(仅更新了数据)
     */
    public static ServerHttpRequest wrap(ServerHttpRequest request, byte[] data)
    {
        return wrap(request, null, data);
    }

    /**
     * 复制一个新Data的ServerHttpRequest
     *
     * @param request 请求对象
     * @param url     新的请求url
     * @param data    新数据
     * @return 新的请求对象(仅更新了数据)
     */
    public static ServerHttpRequest wrap(ServerHttpRequest request, String url, byte[] data)
    {
        return wrap(request, url, null, data);
    }

    /**
     * 复制一个新Data的ServerHttpRequest
     *
     * @param request 请求对象
     * @param url     新的请求url
     * @param headers 新的header
     * @param data    新的报文
     * @return 新的请求对象(仅更新了数据)
     */
    public static ServerHttpRequest wrap(ServerHttpRequest request, String url, HttpHeaders headers, byte[] data)
    {
        ServerHttpRequest wrapper = request;
        if (data != null)
        {
            //1.构造body数据
            wrapper = new ServerHttpRequestDecorator(request)
            {
                @Override
                public HttpHeaders getHeaders()
                {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    //仅把新的header更新到request中
                    httpHeaders.putAll(request.getHeaders());
                    if (null != headers && !headers.isEmpty())
                    {
                        httpHeaders.putAll(headers);
                    }
                    if (data.length > 0)
                    {
                        httpHeaders.setContentLength(data.length);
                    }
                    return httpHeaders;
                }

                @Override
                public Flux<DataBuffer> getBody()
                {
                    return FluxUtil.toFlux(data);
                }
            };
        }

        //2.构造url
        if (!StringUtils.isEmpty(url))
        {
            String reqUrl = request.getURI().getPath();
            String shortUrl = UrlUtil.shortUrl(reqUrl);
            if (!shortUrl.equalsIgnoreCase(url))
            {
                //补充参数后缀
                String realUrl = url + reqUrl.replace(shortUrl, StringUtils.EMPTY);
                wrapper = wrapper.mutate().path(realUrl.trim()).build();
            }
        }
        return wrapper;
    }

    private FluxRequestWrapper()
    {
    }
}
