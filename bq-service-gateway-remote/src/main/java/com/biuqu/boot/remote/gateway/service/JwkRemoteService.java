package com.biuqu.boot.remote.gateway.service;

import com.nimbusds.jose.jwk.JWK;

import java.util.List;

/**
 * Jwk远程调用服务
 *
 * @author BiuQu
 * @date 2023/3/18 19:40
 */
public interface JwkRemoteService
{
    /**
     * 获取批量JWK
     *
     * @param jwkUrl JWK 地址
     * @return 批量的JWK
     */
    List<JWK> getBatchJwk(String jwkUrl);
}
