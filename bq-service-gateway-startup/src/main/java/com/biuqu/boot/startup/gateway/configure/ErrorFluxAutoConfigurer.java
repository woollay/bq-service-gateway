package com.biuqu.boot.startup.gateway.configure;

import com.biuqu.boot.service.gateway.handler.GlobalExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.stream.Collectors;

/**
 * 自定义错误处理的自动注入({@link org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration})
 *
 * @author BiuQu
 * @date 2023/3/8 08:51
 */
@AutoConfiguration(before = WebFluxAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(WebFluxConfigurer.class)
@EnableConfigurationProperties({ServerProperties.class, WebProperties.class})
public class ErrorFluxAutoConfigurer
{
    private final ServerProperties serverProperties;

    public ErrorFluxAutoConfigurer(ServerProperties serverProperties)
    {
        this.serverProperties = serverProperties;
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorWebExceptionHandler.class, search = SearchStrategy.CURRENT)
    @Order(-1)
    public ErrorWebExceptionHandler errorWebExceptionHandler(ErrorAttributes errAttr, WebProperties webProperties,
        ObjectProvider<ViewResolver> viewResolvers, ServerCodecConfigurer serverCodecConfigurer,
        ApplicationContext context)
    {
        WebProperties.Resources resources = webProperties.getResources();
        ErrorProperties errProperties = this.serverProperties.getError();
        DefaultErrorWebExceptionHandler errHandler =
            new GlobalExceptionHandler(errAttr, resources, errProperties, context);
        errHandler.setViewResolvers(viewResolvers.orderedStream().collect(Collectors.toList()));
        errHandler.setMessageWriters(serverCodecConfigurer.getWriters());
        errHandler.setMessageReaders(serverCodecConfigurer.getReaders());
        return errHandler;
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
    public DefaultErrorAttributes errorAttributes()
    {
        return new DefaultErrorAttributes();
    }
}
