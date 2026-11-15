package app.salescopilot.business;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class BusinessWebConfiguration implements WebMvcConfigurer {

    private final BusinessAccessResolver access;

    BusinessWebConfiguration(BusinessAccessResolver access) {
        this.access = access;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(access);
    }
}
