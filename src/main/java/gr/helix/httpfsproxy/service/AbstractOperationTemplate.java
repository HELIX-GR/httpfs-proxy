package gr.helix.httpfsproxy.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Pattern;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

import gr.helix.httpfsproxy.config.HttpFsServiceConfiguration;
import gr.helix.httpfsproxy.model.backend.BaseRequestParameters;

public abstract class AbstractOperationTemplate <P extends BaseRequestParameters, R> implements OperationTemplate<P, R>
{
    protected final static String USERNAME_PARAMETER_KEY = "user.name";
    
    protected final static String OPERATION_PARAMETER_KEY = "op";
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected JavaPropsMapper propertiesMapper;
    
    @Autowired
    protected HttpFsServiceConfiguration backend;
    
    protected final Pattern filenamePattern = Pattern.compile("^[-.0-9\\w]+$"); 
    
    protected final Random random = new Random();
      
    protected abstract Class<R> responseType();
    
    /**
     * Choose a backend (as a base URI) to communicate with.
     * <p>This implementation simply returns a random pick from our available URIs
     * (so, in effect, it operates in a round-robin manner).  
     */
    protected URI baseUri()
    {
        final int b = backend.getBaseUris().size();
        return backend.getBaseUris().get(b < 2? 0 : random.nextInt(b));
    }
    
    /**
     * Check if path is valid and return a (normalized) absolute path
     * 
     * @param userName The HDFS user
     * @param path The path on the HDFS filesystem
     */
    protected String checkPath(String userName, String path)
    {
        Assert.state(!StringUtils.isEmpty(userName), "Expected a non-empty userName!");
        Assert.state(path != null, "Expected a non-null path!");
        
        // Turn to an absolute path
        if (!path.startsWith("/")) {
            // Resolve against user's home directory
            final String homeDir = String.format("/user/%s/", userName);
            path = StringUtils.applyRelativePath(homeDir, path);
        }
        
        if (path.equals("/")) {
            return path;
        }
        
        // Check name components of given path
        final String[] names = path.substring(1).split(File.separator);
        Assert.state(names.length > 0, "Expected at least 1 name component");
        if (!Arrays.stream(names).allMatch(filenamePattern.asPredicate())) {
            throw new IllegalArgumentException("The path has invalid name components: " + path);
        }
        
        return path;
    }
    
    protected URI uriForPath(URI baseUri, String path)
    {
        final String rootRelativePath = StringUtils.applyRelativePath("/webhdfs/v1/", path);
        return baseUri.resolve(rootRelativePath);
    }
    
    @Override
    public R responseFromHttpEntity(HttpEntity e) throws JsonProcessingException, IOException
    {
        Assert.notNull(e, "Expected an non-empty HTTP entity");
        Assert.state(e.getContentType() != null, "Expected to find a content-type header");
        Assert.state("application/json".equals(e.getContentType().getValue()), 
            "Expected content encoded as JSON (application/json)");
        return objectMapper.readValue(e.getContent(), this.responseType());
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, 
        @NotNull String path)
    {
        path = checkPath(userName, path);
        final URI uri = uriForPath(this.baseUri(), path);
        
        return RequestBuilder.create(this.methodName())
            .setUri(uri)
            .addParameter(OPERATION_PARAMETER_KEY, this.operation().name())
            .addParameter(USERNAME_PARAMETER_KEY, userName)
            .build();
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, 
        @NotNull String path, 
        @Valid @NotNull P parameters)
    {
        path = checkPath(userName, path);
        final URI uri = uriForPath(this.baseUri(), path);
        
        RequestBuilder reqBuilder = RequestBuilder.create(this.methodName())
            .setUri(uri)
            .addParameter(OPERATION_PARAMETER_KEY, this.operation().name())
            .addParameter(USERNAME_PARAMETER_KEY, userName);
        
        Properties parametersAsProperties = null; 
        try {
            parametersAsProperties = propertiesMapper.writeValueAsProperties(parameters);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Parameters cannot be written as properties", ex);
        }
        
        for (Entry<Object, Object> p: parametersAsProperties.entrySet()) {
            reqBuilder.addParameter((String) p.getKey(), (String) p.getValue());
        }
        
        return reqBuilder.build();
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, 
        @NotNull String path,
        InputStream in, ContentType contentType)
    {
        // Todo Auto-generated method stub
        return null;
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, 
        @NotNull String path, 
        @Valid @NotNull P parameters, 
        InputStream in, ContentType contentType)
    {
        // Todo Auto-generated method stub
        return null;
    }
}
