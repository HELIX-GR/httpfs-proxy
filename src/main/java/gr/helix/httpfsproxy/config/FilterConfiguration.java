package gr.helix.httpfsproxy.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import gr.helix.httpfsproxy.servlet.DecompressionFilter;

@Configuration
public class FilterConfiguration
{
    @Bean
    public FilterRegistrationBean<DecompressionFilter> registerDecompressionFilter()
    {
        final DecompressionFilter filter = new DecompressionFilter();
        
        FilterRegistrationBean<DecompressionFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.LOWEST_PRECEDENCE);
        return reg;
    }
}
