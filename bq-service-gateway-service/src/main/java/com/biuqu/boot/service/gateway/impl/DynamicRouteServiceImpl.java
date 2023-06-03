package com.biuqu.boot.service.gateway.impl;

import com.biuqu.boot.service.gateway.AtomicRouteService;
import com.biuqu.boot.service.gateway.DynamicRouteService;
import com.biuqu.utils.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 动态路由服务
 *
 * @author BiuQu
 * @date 2023/3/28 10:29
 */
@Slf4j
@Service
public class DynamicRouteServiceImpl implements DynamicRouteService
{
    @Override
    public boolean refresh(String routes)
    {
        if (StringUtils.isEmpty(routes))
        {
            log.warn("no routes to refresh.");
            return false;
        }

        List<RouteDefinition> definitions = JsonUtil.toComplex(routes, new TypeReference<List<RouteDefinition>>()
        {
        });
        return refresh(definitions);
    }

    @Override
    public boolean refresh(List<RouteDefinition> routes)
    {
        if (CollectionUtils.isEmpty(routes))
        {
            log.warn("no routes to parse.");
            return false;
        }

        for (RouteDefinition route : routes)
        {
            atomicService.delete(route.getId());
        }

        boolean addResult = atomicService.add(routes);
        log.info("add routes result:{}", addResult);

        boolean publishResult = atomicService.publish();
        log.info("refresh routes result:{}", publishResult);

        return addResult && publishResult;
    }

    @Override
    public boolean clear(String id)
    {
        boolean result = atomicService.delete(id);
        log.info("delete route result:{}", result);

        atomicService.publish();

        return result;
    }

    @Override
    public boolean clear(Set<String> ids)
    {
        boolean result = atomicService.delete(ids);
        log.info("delete routes result:{}", result);

        atomicService.publish();

        return result;
    }

    @Override
    public boolean clearAll()
    {
        boolean result = atomicService.deleteAll();
        log.info("delete all routes result:{}", result);

        atomicService.publish();

        return result;
    }

    /**
     * 原子服务
     */
    @Autowired
    private AtomicRouteService atomicService;
}
