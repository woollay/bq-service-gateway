package com.biuqu.boot.service.gateway.impl;

import com.biuqu.boot.service.gateway.AtomicRouteService;
import com.biuqu.utils.JsonUtil;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 原子的路由操作服务
 *
 * @author BiuQu
 * @date 2023/3/28 09:37
 */
@Slf4j
@Service
public class AtomicRouteServiceImpl implements AtomicRouteService
{
    @Override
    public boolean publish()
    {
        eventPublisher.publishEvent(new RefreshRoutesEvent(routeWriter));
        return true;
    }

    @Override
    public boolean add(RouteDefinition route)
    {
        if (null == route)
        {
            log.warn("no route to add");
            return false;
        }

        String id = route.getId();
        log.warn("add route[{}] now:{},old route:{}.", id, JsonUtil.toJson(route), JsonUtil.toJson(ROUTES.get(id)));
        routeWriter.save(Mono.just(route)).subscribe();
        ROUTES.put(route.getId(), route);
        return true;
    }

    @Override
    public boolean add(List<RouteDefinition> routes)
    {
        if (null == routes)
        {
            log.warn("no routes to add");
            return false;
        }

        for (RouteDefinition route : routes)
        {
            this.add(route);
        }
        return true;
    }

    @Override
    public boolean delete(String id)
    {
        if (null == id)
        {
            log.warn("no route to delete");
            return false;
        }
        RouteDefinition route = ROUTES.remove(id);
        if (null != route)
        {
            try
            {
                routeWriter.delete(Mono.just(id)).subscribe();
                log.warn("delete route[{}] now:{}.", route.getId(), JsonUtil.toJson(route));
            }
            catch (Exception e)
            {
                log.error("delete route error.", e);
            }
        }
        return true;
    }

    @Override
    public boolean delete(Set<String> ids)
    {
        if (null == ids)
        {
            log.warn("no routes to delete");
            return false;
        }

        for (String id : ids)
        {
            this.delete(id);
        }
        return true;
    }

    @Override
    public boolean deleteAll()
    {
        log.warn("delete all routes now:{}.", JsonUtil.toJson(ROUTES));

        boolean result = this.delete(ROUTES.keySet());

        ROUTES.clear();

        return result;
    }

    /**
     * 全局路由缓存
     */
    private static final Map<String, RouteDefinition> ROUTES = Maps.newHashMap();

    /**
     * 路由写服务
     */
    @Autowired
    private RouteDefinitionWriter routeWriter;

    /**
     * spring框架的事件发布器
     */
    @Autowired
    private ApplicationEventPublisher eventPublisher;
}
