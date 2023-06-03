# resilience4j使用指南

## 1.resilience4j maven依赖

- 当前选用的`springcloud`版本为`3.1.6`对应`spring-cloud-release`为`2021.0.6`

```xml
<!--熔断降级 for spring cloud 3.1.6 @formatter:off-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
    <version>2.1.6</version>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-reactor</artifactId>
    <version>1.7.0</version>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
    <version>1.7.0</version>
    <exclusions>
        <exclusion>
            <artifactId>resilience4j-bulkhead</artifactId>
            <groupId>io.github.resilience4j</groupId>
        </exclusion>
    </exclusions>
</dependency>
```

- 在启动的yaml中引入resilience4j配置
```yaml
#spring-cloud内置熔断组件,也支持从springboot中集成
resilience4j.circuitbreaker:
  configs:
    #熔断机制的默认设置
    default:
      #状态收集器类型
      #COUNT_BASED：根据数量计算，slidingWindowSize为次数
      #TIME_BASED：根据时间计算，slidingWindowSize为秒数
      slidingWindowType: TIME_BASED
      #时间窗口的大小为60秒
      slidingWindowSize: 600
      #在单位时间窗口内最少需要多少次调用才能开始进行统计计算
      minimumNumberOfCalls: 2
      #进入halfOpen状态时，可以被调用次数，就算这些请求的失败率，低于设置的失败率变为close状态，否则变为open。
      permittedNumberOfCallsInHalfOpenState: 2
      #允许断路器自动由打开状态转换为半开状态
      #是否自动进入halfOpen状态，默认false-一定时间后进入halfOpen，ture-需要通过接口执行。
      automaticTransitionFromOpenToHalfOpenEnabled: false
      #断路器打开状态转换为半开状态需要等待的时间
      waitDurationInOpenState: 30s
      #在单位时间窗口内调用失败率达到50%后会启动断路器
      failureRateThreshold: 50
      #消费数据处理的缓存数据量大小
      eventConsumerBufferSize: 20
      #哪些异常都当作失败来处理(会被fallback)
      recordExceptions:
        - org.springframework.web.client.HttpServerErrorException
        - java.io.IOException
        - java.util.concurrent.TimeoutException
        - java.lang.IllegalArgumentException
      #哪些异常直接忽略(不会被fallback)
      ignoreExceptions:
        - java.lang.IllegalStateException
  #熔断备用实例
  instances:
    backendA:
      #使用默认配置
      baseConfig: default
#配置熔断的超时设置
resilience4j.timelimiter:
  configs:
    #默认的慢请求的超时设置策略
    default:
      #请求的超时时间
      timeoutDuration: 3s
      #中断异步调用
      cancelRunningFuture: true
```

## 2.springcloud-gateway引入resilience4j
- 配置springcloud-gateway的过滤器factory:

```java

@Slf4j
@Configuration
public class NettyCircuitBreakerConfigurer
{
    /**
     * 集成了Resilience4j过滤器
     *
     * @param circuitRegistry 服务降级熔断配置
     * @param timeRegistry    服务慢请求配置
     * @return 熔断降级的filter工厂
     */
    @Bean
    public ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory(CircuitBreakerRegistry circuitRegistry,
        TimeLimiterRegistry timeRegistry)
    {
        ReactiveResilience4JCircuitBreakerFactory factory =
            new ReactiveResilience4JCircuitBreakerFactory(circuitRegistry, timeRegistry);
        factory.configureDefault(id ->
        {
            CircuitBreakerConfig circuitConf = circuitRegistry.getDefaultConfig();
            TimeLimiterConfig timeConf = timeRegistry.getDefaultConfig();
            log.info("CircuitBreaker[{}]config:{}/{}", id, circuitConf, timeConf.getTimeoutDuration().getSeconds());
            Resilience4JConfigBuilder builder = new Resilience4JConfigBuilder(id);
            //此处仅构建了一个默认的熔断策略(还可以继续添加)
            return builder.circuitBreakerConfig(circuitConf).timeLimiterConfig(timeConf).build();
        });
        return factory;
    }
}
```

- 编写对应的fallback rest服务:

```java

@Slf4j
@RestController
public class FallbackController
{
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @PostMapping("/fallback")
    public Mono<ResultCode<?>> fallback(ServerWebExchange exchange)
    {
        Exception e = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        ServerWebExchange delegate = exchange;
        if (exchange instanceof ServerWebExchangeDecorator)
        {
            delegate = ((ServerWebExchangeDecorator)exchange).getDelegate();
        }
        String url = delegate.getRequest().getURI().getPath();
        ResultCode<?> resultCode = ResultCode.error(ErrCodeEnum.SERVER_ERROR.getCode());
        log.error("[{}]circuit breaker result:{},with exception:{}", url, JsonUtil.toJson(resultCode), e);
        return Mono.just(resultCode);
    }
}
```

- 路由配置中添加该过滤器

```xml
    #@formatter:off
    - id: demo_auth
    uri: lb://bq-demo/
    predicates:
        - Path=/bq-demo/demo/**,/bq-demo/monitor/**,/demo/**
    #为接口设置熔断过滤器
    filters:
        - name: CircuitBreaker
        args:
            name: circuitBreaker
            fallbackUri: forward:/fallback
```

## 3.基于SpringMVC的微服务中添加resilience4j：
- 编写配置服务
```java
@Slf4j
@Configuration
public class CircuitBreakerConfigurer
{
    /**
     * 全局默认的策略断路器
     *
     * @param circuitRegistry 断路器注册器
     * @return 断路器
     */
    @Primary
    @Bean
    public CircuitBreaker globalCircuitBreaker(CircuitBreakerRegistry circuitRegistry)
    {
        CircuitBreakerConfig circuitBreakerConf = circuitRegistry.getDefaultConfig();
        log.info("circuit breaker config is:{}", circuitBreakerConf);
        CircuitBreaker circuitBreaker = circuitRegistry.circuitBreaker("default", circuitBreakerConf);

        circuitBreaker.getEventPublisher().onSuccess(event -> log.info("circuit breaker success：{}", event))
            .onError(event -> log.info("circuit breaker error：{}", event))
            .onIgnoredError(event -> log.info("circuit breaker ignore：{}", event))
            .onReset(event -> log.info("circuit breaker reset：{}", event))
            .onStateTransition(event -> log.info("circuit breaker transition：{}", event))
            .onCallNotPermitted(event -> log.info("circuit breaker not permitted：{}", event));

        return circuitBreaker;
    }

    /**
     * 全局默认的时间策略(慢请求)断路器
     * <p>
     *
     * @param timeRegistry 断路器注册器
     * @return 断路器
     */
    @Primary
    @Bean
    public TimeLimiter globalTimeLimiter(TimeLimiterRegistry timeRegistry)
    {
        return timeRegistry.timeLimiter("default", timeRegistry.getDefaultConfig());
    }
}
```
## 3.1 使用全局的默认异常处理机制来做服务熔断
- 编写rest服务
```java
    /**
     * 使用全局的服务降级实现{@link com.biuqu.boot.startup.test.handler.GlobalExceptionHandler#handle(String, Exception)}
     *
     * @param param 业务参数
     * @return 返回结果
     */
    @CircuitBreaker(name = "default")
    @ResponseBody
    @PostMapping("/test/circuit-breaker/global")
    public ResultCode<String> testCircuitBreaker(@RequestBody ResultCode<String> param)
    {
        log.error("happened illegal error:{}", param);
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
            log.error("sleep error.", e);
        }
        throw new IllegalStateException("circuit breaker illegal error.");
    }
```

- 定义全局的异常熔断handler
```java
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler
{
    @ExceptionHandler({Exception.class, RuntimeException.class, Throwable.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ResultCode<?> handleErr(HttpServletRequest req, Exception e)
    {
        log.error("global rest[{}] exception.", req.getRequestURI(), e);
        ResultCode<?> resultCode = ResultCode.error(e.getClass().getSimpleName());
        resultCode.setMsg(e.getMessage());
        return resultCode;
    }
}
```

## 3.2 使用自定义的熔断方法
- 通过自定义的降级方法去执行
```java
@CircuitBreaker(name = "default", fallbackMethod = "testCustomFallback")
@ResponseBody
@PostMapping("/test/circuit-breaker/custom")
public ResultCode<String> testCircuitBreakerWithCustom(@RequestBody ResultCode<String> param)
{
    log.error("happened illegal error:{}", param);
    try
    {
        Thread.sleep(1000);
    }
    catch (InterruptedException e)
    {
        log.error("sleep error.", e);
    }
    throw new IllegalStateException("circuit breaker illegal error.");
}

private ResultCode<String> testCustomFallback(@RequestBody ResultCode<String> param, Throwable e)
{
    log.error("happened illegal error:{}", param, e);
    ResultCode<String> resultCode = ResultCode.error(e.getClass().getSimpleName());
    resultCode.setMsg(e.getMessage());
    resultCode.setData("custom circuit breaker");
    return resultCode;
}
```
## 3.3 熔断可使用于各种场景，并不限于Rest请求方法：
- rest[RestController中]请求代码：
```java
@ResponseBody
@PostMapping("/test/circuit-breaker/service")
public ResultCode<String> testCircuitBreakerWithService(@RequestBody ResultCode<String> param)
{
    return testService.testServiceCircuitBreaker(param);
}
```
- service[Service服务中]方法，下例使用了全局默认的方法：
```java
@CircuitBreaker(name = "default")
public ResultCode<String> testServiceCircuitBreaker(@RequestBody ResultCode<String> param)
{
    log.error("happened illegal error:{}", param);
    try
    {
        Thread.sleep(1000);
    }
    catch (InterruptedException e)
    {
        log.error("sleep error.", e);
    }
    throw new IllegalStateException("circuit breaker illegal error.");
}
```

## 3.4 熔断针对慢请求的处理方法，使用了全局默认的异常处理逻辑：

- rest服务代码
```java
@ResponseBody
@PostMapping("/test/circuit-breaker/service/time")
public CompletableFuture<ResultCode<String>> testTimeCircuitBreakerWithService(@RequestBody ResultCode<String> param)
{
    return testService.testTimeCircuitBreaker(param);
}
```

- 服务代码
```java
@TimeLimiter(name = "default")
public CompletableFuture<ResultCode<String>> testTimeCircuitBreaker(@RequestBody ResultCode<String> param)
{
    log.error("current time limiter:{}", param);
    Supplier<ResultCode<String>> supplier = () ->
    {
        try
        {
            Thread.sleep(2005);
        }
        catch (InterruptedException e)
        {
            log.error("sleep error.", e);
        }
        return ResultCode.ok("success.");
    };
    return CompletableFuture.supplyAsync(supplier);
}
```

# 4.resilience4j使用总结：
- resilience4j服务降级熔断是一套较完整的方法，适用于SpringWebFlux场景(如：SpringCloud-Gateway)和SpringMvc场景；
- 在SpringCloud-Gateway的使用过程中异常简洁，如果再加上nacos的配置能力，则随时可以在路由中更改服务降级配置；
- 服务降级不限于Rest请求，基本上任意的方法上都可以做服务降级；
- resilience4j服务降级注解的fallbackMethod必须写在服务中，要和服务方法的参数相同，并额外加上1个异常类参数，同时返回值也必须相同，这其实不太友好，尤其不便于添加统一的服务降级逻辑；
- resilience4j服务降级注解的fallbackMethod不写时，默认使用全局的异常处理逻辑，这虽然比较优雅，但是必须在项目前期就要求大家必须按照统一的服务出参和入参才可以；
- resilience4j慢请求拦截采用了函数式写法；