package gr.helix.httpfsproxy.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer
{
    @Autowired
    private AsyncTaskExecutor taskExecutor;
    
    @Value("${gr.helix.httpfsproxy.async.task-timeout-seconds:120}")
    private Integer timeoutSeconds;
    
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer)
    {
        configurer.setDefaultTimeout(timeoutSeconds.longValue() * 1000L);
        configurer.setTaskExecutor(taskExecutor);
    }
}
