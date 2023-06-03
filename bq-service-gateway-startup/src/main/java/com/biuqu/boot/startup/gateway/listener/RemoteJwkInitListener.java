package com.biuqu.boot.startup.gateway.listener;

import com.biuqu.boot.constants.CommonBootConst;
import com.biuqu.boot.listener.AppInitListener;
import com.biuqu.boot.remote.gateway.service.JwkRemoteService;
import com.biuqu.jwt.PubJwkMgr;
import com.biuqu.jwt.PubJwkMgrFacade;
import com.biuqu.model.Channel;
import com.nimbusds.jose.jwk.JWK;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * remote JWK初始化监听
 * <p>
 * 启动成功后，获取一次远程的jwk配置，本监听必须在nacos路由监听之后
 *
 * @author BiuQu
 * @date 2023/3/15 08:58
 */
@Slf4j
@DependsOn("nacosConfListener")
@Component
public class RemoteJwkInitListener extends AppInitListener
{
    @Override
    protected void init()
    {
        super.init();
        PubJwkMgr remoteJwkMgr = null;
        try
        {
            String jwkUrl = jwtChannel.getAuthUrl();
            log.info("source url is :{}", jwkUrl);
            List<JWK> batchJwk = jwkRemoteService.getBatchJwk(jwkUrl);
            if (!CollectionUtils.isEmpty(batchJwk))
            {
                remoteJwkMgr = new PubJwkMgr(batchJwk);
            }
        }
        catch (Exception e)
        {
            log.error("failed to get jwk by rest.", e);
        }

        if (null != remoteJwkMgr)
        {
            pubJwkMgr.append(remoteJwkMgr);
        }
    }

    @Autowired
    private JwkRemoteService jwkRemoteService;

    /**
     * 公钥管理门面
     */
    @Autowired
    private PubJwkMgrFacade pubJwkMgr;

    /**
     * jwt token
     */
    @Resource(name = CommonBootConst.JWT_CHANNEL_CONFIG)
    private Channel jwtChannel;
}
