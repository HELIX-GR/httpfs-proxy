package gr.helix.httpfsproxy.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig
{
    @Bean(name = "httpClient", destroyMethod = "close")
    CloseableHttpClient httpClient()
    {
        return HttpClients.custom()
            .setMaxConnTotal(100)
            .setMaxConnPerRoute(40)
            .build();
    }
}
