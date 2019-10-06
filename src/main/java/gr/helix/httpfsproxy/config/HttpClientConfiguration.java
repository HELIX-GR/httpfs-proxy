package gr.helix.httpfsproxy.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfiguration
{
    @Bean(name = "httpClient", destroyMethod = "close")
    CloseableHttpClient httpClient()
    {
        final int timeoutMillis = 5 * 1000;  
        
        // see https://www.baeldung.com/httpclient-timeout for the explanation on different timeouts
        final RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setSocketTimeout(timeoutMillis)
            .setConnectionRequestTimeout(timeoutMillis)
            .setConnectTimeout(timeoutMillis)
            .build();
        
        return HttpClients.custom()
            // proxying requests from multiple HDFS users: always perform state-less requests
            .disableCookieManagement() 
            .setDefaultRequestConfig(defaultRequestConfig)
            .setMaxConnTotal(100)
            .setMaxConnPerRoute(40)
            .build();
    }
}
