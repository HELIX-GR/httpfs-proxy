package gr.helix.httpfsproxy.service;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.helix.httpfsproxy.config.HttpFsServiceConfiguration;
import gr.helix.httpfsproxy.model.backend.ServiceStatus;
import gr.helix.httpfsproxy.model.backend.ServiceStatusInfo;

@Service
public class PingService
{
    private final static Logger logger = LoggerFactory.getLogger(PingService.class);
    
    @Autowired
    HttpFsServiceConfiguration backend;
    
    @Autowired
    @Qualifier("httpClient")
    private CloseableHttpClient httpClient;
    
    @Autowired
    private ObjectMapper objectMapper;
  
    private Map<URI, ServiceStatusInfo> statusReport = new ConcurrentHashMap<>();
    
    void pingBackendService(URI baseUri) 
        throws HttpResponseException, IOException
    {
        final URI uri = baseUri.resolve("/webhdfs/v1");
        final HttpUriRequest request = RequestBuilder.get(uri)
            .addParameter("op", "gethomedirectory")
            .addParameter("user.name", backend.getDefaultUser())
            .build();
        
        final String expectedHomeDirectory = String.format("/user/%s", backend.getDefaultUser());
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            if (responseStatus.getStatusCode() != HttpStatus.SC_OK)
                throw new HttpResponseException(
                    responseStatus.getStatusCode(), responseStatus.getReasonPhrase());
            final HttpEntity e = response.getEntity();
            if (e == null)
                throw new IllegalStateException("Expected a non-empty response entity");
            final String contentType = e.getContentType().getValue();
            if (!MediaType.APPLICATION_JSON_VALUE.equals(contentType))
                throw new IllegalStateException(
                    "Got an unexpected Content-Type from response: " + contentType);
            final Map<?,?> r = objectMapper.readValue(e.getContent(), Map.class);
            if (!expectedHomeDirectory.equals(r.get("Path")))
                throw new IllegalStateException(
                    "Expected key [Path] to hold the user\'s home directory: " + r.get("Path"));
        }   
        return;
    }
    
    /**
     * Ping backend HttpFs services and report their status
     */
    @Scheduled(fixedRate = 5000L)
    public void pingBackend()
    {
        for (URI baseUri: backend.getBaseUris()) {
            final Instant now = Instant.now();
            ServiceStatusInfo statusInfo = null;
            try {
                pingBackendService(baseUri);
            } catch (HttpResponseException ex) {
                statusInfo = ServiceStatusInfo.of(baseUri, ServiceStatus.FAILED, now, 
                    String.format("The request has failed: %s", ex.getMessage()));
            } catch (IllegalStateException ex) {
                statusInfo = ServiceStatusInfo.of(baseUri, ServiceStatus.FAILED, now, ex.getMessage());
            } catch (IOException ex) {
                statusInfo = ServiceStatusInfo.of(baseUri, ServiceStatus.UNREACHABLE, now, 
                    String.format("Encountered an I/O exception: %s", ex.getMessage()));
            } 
            if (statusInfo == null) {
                statusInfo = ServiceStatusInfo.of(baseUri, ServiceStatus.OK, now);
            }
            if (!statusInfo.getStatus().isSuccessful()) 
                logger.error("Reporting status for {}: {} - {}", 
                    baseUri, statusInfo.getStatus(), statusInfo.getErrorMessage());
            // Store status to our internal report
            this.statusReport.put(baseUri, statusInfo);
        }
    }
    
    public Map<URI, ServiceStatusInfo> getReport()
    {
        return Collections.unmodifiableMap(this.statusReport);
    }
}
