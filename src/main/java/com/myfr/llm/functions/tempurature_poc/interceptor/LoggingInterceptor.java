package com.myfr.llm.functions.tempurature_poc.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {
    private static final String TIME_TAKEN = "timeTaken";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(TIME_TAKEN, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {

        long timeTaken = System.currentTimeMillis() - (Long) request.getAttribute(TIME_TAKEN);
        String requestId = request.getHeader("X-Request-ID");

        if (ex == null) {
            log.info("Request completed - Path: {}, Request ID: {}, Status: {}, Time: {}ms",
                    request.getRequestURI(), requestId, response.getStatus(), timeTaken);
        } else {
            log.error("Request failed - Path: {}, Request ID: {}, Status: {}, Time: {}ms, Error: {}",
                    request.getRequestURI(), requestId, response.getStatus(), timeTaken, ex.getMessage());
        }
    }
}