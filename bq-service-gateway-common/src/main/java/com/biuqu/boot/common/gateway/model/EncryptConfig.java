package com.biuqu.boot.common.gateway.model;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 待加解密处理的请求配置类
 *
 * @author BiuQu
 * @date 2023/4/25 09:18
 */
@Data
public class EncryptConfig
{
    /**
     * 请求参数是否需要解密
     *
     * @return true表示需要解密
     */
    public boolean needDec()
    {
        return !StringUtils.isEmpty(dec);
    }

    /**
     * 响应参数是否需要加密
     *
     * @return true表示需要
     */
    public boolean needEnc()
    {
        return !StringUtils.isEmpty(enc);
    }

    /**
     * 请求的url(对外暴露的虚拟接口)
     */
    private String url;

    /**
     * 对请求body的解密算法
     */
    private String dec;

    /**
     * 返回结果的加密算法
     */
    private String enc;

    /**
     * 真实的服务接口(但是不对客户直接暴露)
     */
    private String redirect;
}
