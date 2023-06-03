package com.biuqu.boot.service.gateway;

import org.springframework.cloud.gateway.route.RouteDefinition;

import java.util.List;
import java.util.Set;

/**
 * 原子的路由服务
 *
 * @author BiuQu
 * @date 2023/3/28 09:16
 */
public interface AtomicRouteService
{
    /**
     * 原子的路由发布(所有发布的路由此时开始生效)
     * <p>
     *
     * @return true表示发布成功
     */
    boolean publish();

    /**
     * 添加路由(仅执行添加到路由列表操作，未生效)
     *
     * @param route 待添加的路由
     * @return true表示添加成功
     */
    boolean add(RouteDefinition route);

    /**
     * 添加路由(仅执行添加到路由列表操作，未生效)
     *
     * @param routes 待添加的路由集合
     * @return true表示添加成功
     */
    boolean add(List<RouteDefinition> routes);

    /**
     * 删除路由(仅执行删除操作，未生效)
     *
     * @param id 路由id
     * @return true表示删除成功
     */
    boolean delete(String id);

    /**
     * 删除路由集合(仅执行删除操作，未生效)
     *
     * @param ids 路由id集合
     * @return true表示删除成功
     */
    boolean delete(Set<String> ids);

    /**
     * 删除所有路由(仅执行删除操作，未生效)
     *
     * @return true表示删除成功
     */
    boolean deleteAll();
}
