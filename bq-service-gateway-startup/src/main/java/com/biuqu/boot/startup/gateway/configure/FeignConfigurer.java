package com.biuqu.boot.startup.gateway.configure;

import com.biuqu.boot.constants.CommonBootConst;
import com.biuqu.constants.Const;
import com.biuqu.context.ApplicationContextHolder;
import com.biuqu.http.HttpClientMgr;
import com.biuqu.http.HttpParam;
import com.biuqu.utils.JsonUtil;
import feign.Feign;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Feign配置服务
 *
 * @author BiuQu
 * @date 2023/3/2 08:00
 */
@Slf4j
@AutoConfigureBefore(FeignAutoConfiguration.class)
@Configuration
@ConditionalOnClass(Feign.class)
public class FeignConfigurer
{
    @Bean
    public HttpClient httpClient()
    {
        List<HttpParam> httpParams = ApplicationContextHolder.getBean(CommonBootConst.HTTP_PARAMS);
        HttpParam httpParam = httpParams.get(Const.TWO);
        log.info("current gateway http param:{}", JsonUtil.toJson(httpParam));
        HttpClientMgr clientMgr = new HttpClientMgr(httpParam);
        return clientMgr.clientBuilder().build();
    }
}
