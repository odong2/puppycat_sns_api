package com.architecture.admin.config;

import com.architecture.admin.libraries.filter.Filter;
import com.architecture.admin.libraries.filter.LogFilterLibrary;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*****************************************************
 * 필터 등록
 ****************************************************/
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<Filter> logFilter() {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new LogFilterLibrary()); // 등록 할 필터를 지정
        filterRegistrationBean.setOrder(1);  // 순서가 낮을수록 먼저 동작한다.
        filterRegistrationBean.addUrlPatterns("/*"); // 필터를 적용할 URL 패턴을 지정

        return filterRegistrationBean;
    }

}
