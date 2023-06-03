package com.biuqu.boot.startup.gateway.configure;

import com.biuqu.boot.constants.CommonBootConst;
import com.biuqu.constants.Const;
import com.biuqu.utils.TimeUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.server.logging.AccessLog;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Netty配置器
 * <p>
 *
 * @author BiuQu
 * @date 2023/3/12 12:11
 */
@Slf4j
@Configuration
public class NettyConfigurer
{
    /**
     * 配置自定义的AccessLog
     *
     * @return Netty定制工厂
     */
    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> nettyServerFactory()
    {
        return factory ->
        {
            //配置access log
            factory.addServerCustomizers(httpServer -> httpServer.accessLog(true, x ->
            {
                List<String> params = Lists.newArrayList();
                params.add(x.accessDateTime().format(DateTimeFormatter.ofPattern(TimeUtil.SIMPLE_TIME_FORMAT)));
                String traceId = Const.MID_LINK;
                if (null != x.responseHeader(CommonBootConst.TRACE_ID))
                {
                    traceId = x.responseHeader(CommonBootConst.TRACE_ID).toString();
                }
                params.add(traceId);

                String spanId = Const.MID_LINK;
                if (null != x.responseHeader(CommonBootConst.SPAN_ID))
                {
                    spanId = x.responseHeader(CommonBootConst.SPAN_ID).toString();
                }
                params.add(spanId);

                params.add(x.method().toString());
                params.add(x.protocol());
                params.add(x.connectionInformation().remoteAddress().toString());
                params.add(x.connectionInformation().hostAddress().toString());
                params.add(x.status() + StringUtils.EMPTY);
                params.add(x.uri().toString());
                params.add(x.contentLength() + "B");
                params.add(x.duration() + "ms");
                String format = StringUtils.repeat("{}|", params.size());
                return AccessLog.create(format, params.toArray());
            }));
        };
    }
}
