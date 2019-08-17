package gr.helix.httpfsproxy.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gr.helix.httpfsproxy.httpfs")
public class HttpFsServiceProperties
{
    private URI baseUri;
    
    private String defaultUser;
    
    public URI getBaseUri()
    {
        return baseUri;
    }
    
    public String getDefaultUser()
    {
        return defaultUser;
    }
    
    public void setBaseUri(URI uri)
    {
        this.baseUri = uri;
    }
    
    public void setDefaultUser(String defaultUser)
    {
        this.defaultUser = defaultUser;
    }

    @Override
    public String toString()
    {
        return String.format("HttpFsServiceProperties [baseUri=%s, defaultUser=%s]", baseUri,
            defaultUser);
    }
    
    
}
