package gr.helix.httpfsproxy.controller;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;

import gr.helix.httpfsproxy.config.HttpFsServiceProperties;
import gr.helix.httpfsproxy.model.RestResponse;

@RestController
@RequestMapping(path = "/admin", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class AdminController
{
    private final static Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private HttpFsServiceProperties backend;
    
    private static class ServiceInfo
    {
        URI backendUri;
        
        public ServiceInfo(URI backendUri)
        {
            this.backendUri = backendUri;
        }
        
        @JsonProperty
        public URI getBackendUri()
        {
            return backendUri;
        }
    }
    
    @GetMapping(path = {"/", "/index"})
    public RestResponse<ServiceInfo> index()
    {
        ServiceInfo info = new ServiceInfo(backend.getBaseUri());
        return RestResponse.result(info);
    }
}
