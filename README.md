# bq-service-gateway服务使用说明
- bq微服务解决方案参见
	```xml
    <dependency>
        <groupId>com.biuqu</groupId>
        <artifactId>bq-parent</artifactId>
        <version>1.0.3</version>
    </dependency>
	```
 中的[`README`文档](https://github.com/woollay/bq-parent/raw/main/README.MD) 
- 本服务基于SpringCloud-Gateway二次封装，主要是为了简化业务安全场景，当然也支持灵活的配置扩展：
- 本服务具备完整的独立服务能力，核心依赖：
    ```xml
    <dependency>
        <groupId>com.biuqu</groupId>
        <artifactId>bq-boot-root</artifactId>
        <version>1.0.3</version>
    </dependency>
    ```

## 1. 为什么要写bq-service-gateway服务

- `SpringCloud-Gateway`本身带有20+默认的过滤器，又是基于WebFlux编程，上手难度较大；
- 业务安全通常具有共性（如：接口参数完整性校验、业务敏感参数加解密），放在公共的`bq-service-gateway`可以较好地做好安全和业务的隔离，使业务服务尽可能只关注业务特性；
- `bq-service-gateway`用作JwtToken鉴权分离方案中访问最频繁的重要部分，完全剥离了鉴权时的数据库访问，提升了微服务系统的性能；
- 整合了sleuth/zipkin，并同时整合了logback，`bq-service-gateway`这种基于Netty的服务也做到了Access Log和运行日志均具有链路追踪ID，各位要是看下代码就知道这其实并不简单；
- 整合了`CircuitBreaker`/Sentinel服务降级、`Nacos`/`Eureka`服务注册中心，使之具有完备的服务能力；

## 2. 使用bq-service-gateway服务有什么好处
- 按照组件化的思维去添加了多个安全过滤器，读者可以根据业务的实际需要去单独扩展其中的某些过滤器；
- 对报文做了缓存和耗时记录，并贯穿了链路追踪ID，基本上可以达到开箱即用；
- 整合了docker脚本和docker-compose两种方式，可以非常方便地构建docker集群(目前只验证了MacOS)；

## 3. bq-service-gateway最佳实践
- `bq-service-gateway`最佳实践是配合[bq-service-auth](https://github.com/woollay/bq-service-auth) 中一起使用，以达到JwtToken会话生成和鉴权的分离；


