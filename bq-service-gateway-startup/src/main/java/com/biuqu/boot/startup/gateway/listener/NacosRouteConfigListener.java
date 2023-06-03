package com.biuqu.boot.startup.gateway.listener;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.biuqu.boot.listener.AppInitListener;
import com.biuqu.boot.service.gateway.DynamicRouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Nacos配置路由服务监听
 * <p>
 * 服务启动时会加载路由，为了不影响启动，动态路由必须在应用服务就绪后才开始加载
 *
 * @author BiuQu
 * @date 2023/3/28 11:05
 */
@Slf4j
@Component("nacosConfListener")
public class NacosRouteConfigListener extends AppInitListener
{
    @Override
    protected void init()
    {
        super.init();
        try
        {
            Properties properties = new Properties();
            properties.setProperty(PropertyKeyConst.SERVER_ADDR, serverAddr);
            ConfigService configService = NacosFactory.createConfigService(properties);

            //给配置服务添加监听器
            configService.addListener(dataId, group, new Listener()
            {
                @Override
                public void receiveConfigInfo(String config)
                {
                    log.info("get changed route:{}", config);
                    routeService.refresh(config);
                }

                @Override
                public Executor getExecutor()
                {
                    return null;
                }
            });

            String config = configService.getConfig(dataId, group, timeout);
            log.info("get init route config:{}", config);
            routeService.refresh(config);
        }
        catch (NacosException e)
        {
            log.error("failed to config nacos.", e);
        }
    }

    /**
     * 动态路由服务
     */
    @Autowired
    private DynamicRouteService routeService;

    /**
     * 监听的data Id
     */
    @Value("${spring.cloud.nacos.data-id:bq-gateway-route-id}")
    private String dataId;

    /**
     * 服务发现地址
     */
    @Value("${spring.cloud.nacos.server-addr:localhost:8848}")
    private String serverAddr;

    /**
     * 配置的组
     */
    @Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}")
    private String group;

    /**
     * 配置的组
     */
    @Value("${spring.cloud.nacos.timeout:3000}")
    private long timeout;
}
