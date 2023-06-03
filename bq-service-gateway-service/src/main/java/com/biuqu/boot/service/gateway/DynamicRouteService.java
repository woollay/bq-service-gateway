package com.biuqu.boot.service.gateway;

import org.springframework.cloud.gateway.route.RouteDefinition;

import java.util.List;
import java.util.Set;

/**
 * 动态路由服务
 *
 * @author BiuQu
 * @date 2023/3/28 09:00
 */
public interface DynamicRouteService
{
    /**
     * 刷新路由
     * <p>
     * 对应路由原子操作：先添加再发布
     *
     * @param routes 路由集合
     * @return true表示刷新成功
     */
    boolean refresh(String routes);

    /**
     * 刷新路由
     * <p>
     * 对应路由原子操作：先添加再发布
     *
     * @param routes 路由集合
     * @return true表示刷新成功
     */
    boolean refresh(List<RouteDefinition> routes);

    /**
     * 清理路由
     * <p>
     * 路由若存在就先删除再发布
     *
     * @param id 路由id
     * @return true表示清理成功
     */
    boolean clear(String id);

    /**
     * 清理路由
     * <p>
     * 路由若存在就先删除再发布
     *
     * @param ids 路由id集合
     * @return true表示清理成功
     */
    boolean clear(Set<String> ids);

    /**
     * 清理全部路由
     * <p>
     * 路由若存在就先删除再发布
     *
     * @return true表示清理成功
     */
    boolean clearAll();
}
