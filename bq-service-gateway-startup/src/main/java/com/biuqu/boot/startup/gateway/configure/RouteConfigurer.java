package com.biuqu.boot.startup.gateway.configure;

import com.biuqu.boot.common.gateway.constants.GatewayConst;
import com.biuqu.boot.common.gateway.model.EncryptConfig;
import com.biuqu.boot.common.gateway.model.IntegrityCheckConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

/**
 * 路由映射规则
 *
 * @author BiuQu
 * @date 2023/3/5 11:27
 */
@Configuration
public class RouteConfigurer
{
    /**
     * 认证加密配置
     *
     * @return 认证服务的安全配置规则
     */
    @Bean
    @ConfigurationProperties(prefix = "bq.cloud.gateway.security.auth")
    public EncryptConfig authConfig()
    {
        return new EncryptConfig();
    }

    /**
     * 配置报文加密规则
     *
     * @return 报文加密规则列表
     */
    @Bean(GatewayConst.ENCRYPT_RULE)
    @ConfigurationProperties(prefix = "bq.cloud.gateway.security.encrypts")
    public List<EncryptConfig> encryptConfig()
    {
        return Lists.newArrayList();
    }

    /**
     * 完整性校验规则配置
     *
     * @return 校验规则配置
     */
    @Bean
    @ConfigurationProperties(prefix = "bq.cloud.gateway.security.verify")
    public IntegrityCheckConfig verifyConfig()
    {
        return new IntegrityCheckConfig();
    }

    /**
     * 跳过鉴权的白名单列表
     *
     * @return 不用鉴权的白名单列表
     */
    @Bean(GatewayConst.WHITELIST)
    @ConfigurationProperties(prefix = "bq.cloud.gateway.security.whitelist")
    public Set<String> whitelist()
    {
        return Sets.newHashSet();
    }
}
