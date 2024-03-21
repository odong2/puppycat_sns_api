/*
package com.architecture.admin.libraries.filter;

import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class AccessLogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        // 들어오는 요청이 HttpServletRequest의 인스턴스인지 확인합니다.
        if (request instanceof HttpServletRequest) {
            String uri = ((HttpServletRequest) request).getRequestURI();
            // "/robots.txt"로 시작하는 요청을 로그로 남기지 않도록 처리합니다.
            if (uri.startsWith("/robots.txt")) {
                request.setAttribute("NO_LOG", "true");
            }
        }
        // 다음 필터 또는 서블릿으로 요청을 전달합니다.
        chain.doFilter(request, response);
    }
}

 */