package com.biuqu.boot.startup.gateway.configure;

import com.biuqu.boot.constants.CommonBootConst;
import com.biuqu.jwt.JwkMgr;
import com.biuqu.jwt.PubJwkMgrFacade;
import com.biuqu.jwt.PubJwtMgr;
import com.biuqu.model.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JwtToken配置服务
 *
 * @author BiuQu
 * @date 2023/2/4 15:56
 */
@Slf4j
@Configuration
public class PubJwtConfigurer
{
    @Bean(CommonBootConst.JWT_CHANNEL_CONFIG)
    @ConfigurationProperties(prefix = "bq.channels.jwt")
    public Channel jwtChannel()
    {
        return new Channel();
    }

    @Bean
    public PubJwkMgrFacade facade(@Qualifier(CommonBootConst.JWT_CHANNEL_CONFIG) Channel channel)
    {
        JwkMgr localJwkMgr = new JwkMgr(channel);
        return new PubJwkMgrFacade(localJwkMgr);
    }

    @Bean
    public PubJwtMgr pubJwtMgr(PubJwkMgrFacade jwkMgr)
    {
        return new PubJwtMgr(jwkMgr);
    }
}
