package com.connectsphere.gateway.exception;

import java.util.Map;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

/**
 * Global Exception Handler for the Reactive API Gateway.
 * 
 * <p>Standard {@code @ControllerAdvice} does not work in Spring Cloud Gateway because it 
 * is built on **Spring WebFlux (Reactive Stack)** rather than Spring MVC.
 * 
 * <p>This implementation:
 * <ul>
 *     <li>Extends {@code AbstractErrorWebExceptionHandler} to hook into the reactive error stream.</li>
 *     <li>Captures authentication failures (401), missing services (503), and internal errors (500).</li>
 *     <li>Converts low-level reactive exceptions into standardized JSON responses for the frontend.</li>
 * </ul>
 */
@Component
@Order(-2) // Higher priority than default Spring error handler
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalExceptionHandler(ErrorAttributes errorAttributes,
                                  WebProperties webProperties,
                                  ApplicationContext applicationContext,
                                  ServerCodecConfigurer configurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.setMessageWriters(configurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorMap = getErrorAttributes(request, ErrorAttributeOptions.defaults());

        // Get the original exception message
        Throwable error = getError(request);
        String message = error.getMessage();

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        // Map specific error messages to proper HTTP status codes
        if (message != null) {
            if (message.contains("Missing Authorization") ||
                message.contains("Invalid Authorization") ||
                message.contains("Invalid Token")) {
                status = HttpStatus.UNAUTHORIZED;
            } else if (message.contains("503") ||
                       message.contains("Connection refused") ||
                       message.contains("No servers available for service") ||
                       message.contains("LoadBalancer does not contain an instance") ||
                       message.contains("Unable to find instance")) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
            }
        }

        Map<String, Object> response = Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message != null ? message : "Unexpected error",
                "path", request.path()
        );

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(response));
    }
}
