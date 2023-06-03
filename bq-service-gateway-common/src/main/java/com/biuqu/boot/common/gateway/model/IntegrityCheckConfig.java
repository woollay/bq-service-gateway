package com.biuqu.boot.common.gateway.model;

import com.biuqu.utils.UrlUtil;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;

import java.util.Set;

/**
 * 待签名的配置类
 *
 * @author BiuQu
 * @date 2023/4/25 09:17
 */
@Data
public class IntegrityCheckConfig
{
    /**
     * 是否需要对接口摘要校验
     * 1.只要spring.cloud.gateway.security.verify.urls中配置了该Url就需要校验签名
     * 2.spring.cloud.gateway.security.verify.enabled配置了true,同时urls为空时也需要校验签名
     *
     * @param url 请求的url
     * @return true表示需要校验请求参数的摘要
     */
    public boolean needSign(String url)
    {
        String shortUrl = UrlUtil.shortUrl(url);
        if (this.getUrls().contains(shortUrl))
        {
            return true;
        }
        else if (this.isEnabled() && CollectionUtils.isEmpty(this.getUrls()))
        {
            return true;
        }

        return false;
    }

    /**
     * 是否启用参数摘要校验
     */
    private boolean enabled = true;

    /**
     * 需要做参数校验的url(在enable为true时，不设置表示对所有url都做参数校验)
     */
    private Set<String> urls;
}
