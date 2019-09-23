package gr.helix.httpfsproxy.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer
{
    @Autowired
    AsyncTaskExecutor taskExecutor;
    
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer)
    {
        configurer.setDefaultTimeout(28 * 1000L);
        configurer.setTaskExecutor(taskExecutor);
    }
}
