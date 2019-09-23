package gr.helix.httpfsproxy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TaskExecutorConfiguration
{
    @Bean({"taskExecutor", "threadpoolTaskExecutor"})
    AsyncTaskExecutor taskExecutor() 
    {        
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(20);
        taskExecutor.setThreadNamePrefix("async-");
        return taskExecutor;
    };
}
