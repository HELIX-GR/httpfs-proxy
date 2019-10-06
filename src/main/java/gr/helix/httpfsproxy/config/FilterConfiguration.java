package gr.helix.httpfsproxy.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import gr.helix.httpfsproxy.servlet.DecompressionFilter;

@Configuration
public class FilterConfiguration
{
    @Bean // Fixme not working under Tomcat 8.x
    public FilterRegistrationBean<DecompressionFilter> registerDecompressionFilter()
    {
        final DecompressionFilter filter = new DecompressionFilter();
        
        FilterRegistrationBean<DecompressionFilter> reg = new FilterRegistrationBean<>(filter);
        reg.addUrlPatterns("/f/file/content"); 
        reg.setOrder(Ordered.LOWEST_PRECEDENCE);
        return reg;
    }
}
