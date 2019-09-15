package gr.helix.httpfsproxy.config;

import java.net.URI;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gr.helix.httpfsproxy.httpfs")
@lombok.Getter
@lombok.Setter
@lombok.ToString
public class HttpFsServiceConfiguration
{
    private List<URI> baseUris;

    private String defaultUser;
}
