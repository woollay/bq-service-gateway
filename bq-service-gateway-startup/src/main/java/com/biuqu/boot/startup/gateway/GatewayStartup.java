package com.biuqu.boot.startup.gateway;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 容器启动类(默认的启动类，带Netty容器)
 *
 * @author BiuQu
 * @date 2023/1/27 12:06
 */
@EnableDiscoveryClient
@SpringBootApplication
@EnableAspectJAutoProxy
@EnableConfigurationProperties
@EnableEncryptableProperties
@ComponentScan(basePackages = {"com.biuqu"})
public class GatewayStartup
{
    public static void main(String[] args)
    {
        SpringApplication.run(GatewayStartup.class, args);
    }
}
