package com.biuqu.boot.service.gateway.handler;

import com.biuqu.errcode.ErrCodeEnum;
import com.biuqu.exception.CommonException;
import com.biuqu.model.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 设置WebFlux全局处理的异常对象
 *
 * @author BiuQu
 * @date 2023/3/6 08:04
 */
@Slf4j
public class GlobalExceptionHandler extends DefaultErrorWebExceptionHandler
{
    /**
     * Create a new {@code DefaultErrorWebExceptionHandler} instance.
     *
     * @param errorAttributes    the error attributes
     * @param resources          the resources configuration properties
     * @param errorProperties    the error configuration properties
     * @param applicationContext the current application context
     * @since 2.4.0
     */
    public GlobalExceptionHandler(ErrorAttributes errorAttributes, WebProperties.Resources resources,
        ErrorProperties errorProperties, ApplicationContext applicationContext)
    {
        super(errorAttributes, resources, errorProperties, applicationContext);
    }

    /**
     * 覆盖默认的异常处理类别(屏蔽掉默认的响应值)
     *
     * @param errorAttributes 异常属性
     * @return 路由异常的处理函数
     */
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes)
    {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    @Override
    protected Mono<ServerResponse> renderErrorResponse(ServerRequest request)
    {
        Map<String, Object> error = getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.ALL));
        Throwable e = getError(request);
        log.error("happened exception by global catching:{}.", e.getMessage());
        String code = ErrCodeEnum.SERVER_ERROR.getCode();
        if (e instanceof CommonException)
        {
            code = ((CommonException)e).getErrCode().getCode();
        }
        ResultCode<?> resultCode = ResultCode.error(code);
        int httpCode = getHttpStatus(error);
        if (httpCode == HttpStatus.OK.value())
        {
            httpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        else if (ErrCodeEnum.SIGNATURE_ERROR.getCode().equals(code) || ErrCodeEnum.AUTH_ERROR.getCode().equals(code))
        {
            httpCode = HttpStatus.UNAUTHORIZED.value();
        }
        ServerResponse.BodyBuilder bodyBuilder = ServerResponse.status(httpCode);
        bodyBuilder.contentType(MediaType.APPLICATION_JSON);
        return bodyBuilder.bodyValue(resultCode);
    }
}
