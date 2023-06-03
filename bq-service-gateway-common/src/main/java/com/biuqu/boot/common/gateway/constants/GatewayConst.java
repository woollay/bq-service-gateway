package com.biuqu.boot.common.gateway.constants;

/**
 * 网关常量
 *
 * @author BiuQu
 * @date 2023/3/4 17:59
 */
public final class GatewayConst
{
    /**
     * 缓存Body的key
     */
    public static final String BODY_CACHE_KEY = "bqBodyCache";

    /**
     * 缓存返回结果是否需要加密的key
     */
    public static final String ENC_RESPONSE_ALG_KEY = "bqEncRespAlg";

    /**
     * 链路日志key
     */
    public static final String TRACE_LOG_KEY = "bqTraceLog";

    /**
     * 缓存起始时间的key
     */
    public static final String START_CACHE_KEY = "bqStartCache";

    /**
     * 免鉴权和完整性校验的白名单
     */
    public static final String WHITELIST = "whitelistSvc";

    /**
     * 加密规则
     */
    public static final String ENCRYPT_RULE = "encryptRuleSvc";

    /**
     * 加密header对应的加密器名称
     */
    public static final String HEADER_ENC_ID = "bq-enc";
    
    /**
     * 完整性
     */
    public static final String HEADER_INTEGRITY = "bq-integrity";

    private GatewayConst()
    {
    }
}
