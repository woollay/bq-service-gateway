package com.biuqu.boot.remote.gateway.service.impl;

import com.biuqu.boot.remote.gateway.service.JwkRemoteService;
import com.biuqu.http.CommonRestTemplate;
import com.google.common.collect.Lists;
import com.nimbusds.jose.jwk.JWK;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

/**
 * jwk远程获取服务实现
 *
 * @author BiuQu
 * @date 2023/3/18 19:43
 */
@Slf4j
@Service
public class JwkRemoteServiceImpl implements JwkRemoteService
{
    @Override
    public List<JWK> getBatchJwk(String jwkUrl)
    {
        ResponseEntity<String> response = restTemplate.getForEntity(jwkUrl, String.class);
        List<JWK> batchJwk = Lists.newArrayList();
        if (null != response)
        {
            String jwkJson = response.getBody();
            log.info("source url:{},response:{}", jwkUrl, jwkJson);
            JSONObject jsonObject = new JSONObject(jwkJson);
            if (!jsonObject.has(KEYS))
            {
                return batchJwk;
            }
            JSONArray jsonArray = jsonObject.getJSONArray(KEYS);
            Iterator<Object> iterator = jsonArray.iterator();
            while (iterator.hasNext())
            {
                try
                {
                    JWK jwk = JWK.parse(iterator.next().toString());
                    if (null != jwk)
                    {
                        batchJwk.add(jwk);
                    }
                }
                catch (ParseException e)
                {
                    log.error("failed to parse jwk", e);
                }
            }
        }
        return batchJwk;
    }

    /**
     * 公钥json中的key
     */
    private static final String KEYS = "keys";

    /**
     * 带负载均衡的http客户端
     */
    @Autowired
    private CommonRestTemplate restTemplate;
}
