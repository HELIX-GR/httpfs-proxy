package gr.helix.httpfsproxy.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
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
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

import gr.helix.httpfsproxy.config.HttpFsServiceConfiguration;
import gr.helix.httpfsproxy.model.ops.BaseRequestParameters;
import gr.helix.httpfsproxy.validation.FilePath;

public abstract class AbstractOperationTemplate <P extends BaseRequestParameters, R> implements OperationTemplate<P, R>
{
    private final static String USERNAME_PARAMETER_KEY = "user.name";
    
    private final static String OPERATION_PARAMETER_KEY = "op";
    
    private final static String PARAMETERS_REQUIRED_MESSAGE_FORMAT = 
        "This operation (%s) requires parameters";
    
    private final static String BODY_REQUIRED_MESSAGE_FORMAT = 
        "This operation (%s) requires a request body";
    
    private final static String CONTENT_TYPE_NOT_ALLOWED_MESSAGE_FORMAT = 
        "This operation (%s) does not allow a content-type of [%s]";
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private JavaPropsMapper propertiesMapper;
    
    private final Random random = new Random();
    
    @Autowired
    protected HttpFsServiceConfiguration backend;
    
    protected abstract Class<R> responseType();
    
    /**
     * Are parameters required for this type of request? 
     */
    protected abstract boolean requireParameters();
    
    /**
     * Is a body (a request payload) required for this type of request?
     */
    protected boolean requireBody() { return false; }
    
    /**
     * Provide an object of default parameters to be used when parameters are not given 
     * (in {@link #requestForPath}).
     * 
     * <p>Note that this default object is meaningful only if parameters are not required
     * (see {@link #requireParameters()}) 
     */
    protected P defaultParameters() { return null; };
    
    /**
     * Specify a default content-type for this type of request (to be used if not supplied to
     *   {@link OperationTemplate#requestForPath}.
     * @return a content-type, or <tt>null</tt> if no default exists
     */
    protected ContentType defaultContentType() { return null; };
    
    /**
     * Narrow-down what is considered to be a valid content-type for this request
     * @return a collection of allowed content types, or <tt>null</tt> to not allow everything 
     */
    protected Collection<ContentType> allowedContentTypes() { return null; };
        
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
    
    private void checkParameters(P parameters)
    {
        if (parameters == null && this.requireParameters()) {
            throw new IllegalStateException(
                String.format(PARAMETERS_REQUIRED_MESSAGE_FORMAT, this.operation()));
        }
    }
    
    private <D> void checkBody(D content, ContentType contentType)
    {
        if (content == null && this.requireBody()) {
            throw new IllegalStateException(
                String.format(BODY_REQUIRED_MESSAGE_FORMAT, this.operation()));
        }
        
        if (content != null && contentType != null) {
            Collection<ContentType> allowedContentTypes = this.allowedContentTypes();
            if (allowedContentTypes != null && !allowedContentTypes.contains(contentType)) {
                throw new IllegalStateException(
                    String.format(CONTENT_TYPE_NOT_ALLOWED_MESSAGE_FORMAT, this.operation(), contentType));
            }
        }
    }
    
    /**
     * Return an absolute path possibly resolving a home-relative path
     * 
     * @param userName The HDFS user
     * @param filePath The path on the HDFS filesystem
     */
    private String resolvePath(String userName, String filePath)
    {
        // Turn to an absolute path
        if (!filePath.startsWith("/")) {
            // Resolve against user's home directory
            final String homeDir = String.format("/user/%s/", userName);
            filePath = StringUtils.applyRelativePath(homeDir, filePath);
        }
        
        return filePath;
    }
    
    private URI uriForPath(URI baseUri, String userName, String filePath, P parameters)
    {
        final String contextPath = baseUri.getPath();
        final String path = StringUtils.applyRelativePath("/webhdfs/v1/", filePath);
        
        final URIBuilder uriBuilder = new URIBuilder(baseUri)
            .setPath(StringUtils.applyRelativePath(contextPath, path))
            .addParameter(OPERATION_PARAMETER_KEY, this.operation().name())
            .addParameter(USERNAME_PARAMETER_KEY, userName);
        
        if (parameters == null)
            parameters = this.defaultParameters();
        
        if (parameters != null) {
            Properties parametersAsProperties = null; 
            try {
                parametersAsProperties = propertiesMapper.writeValueAsProperties(parameters);
            } catch (IOException ex) {
                throw new IllegalArgumentException("The given parameters cannot be written as properties", ex);
            }
            for (Entry<Object, Object> p: parametersAsProperties.entrySet()) {
                uriBuilder.addParameter((String) p.getKey(), (String) p.getValue());
            }
        }
        
        // Build target URI
        
        URI uri = null;
        try {
            uri = uriBuilder.build();
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
        
        return uri;
    }
    
    private HttpUriRequest requestForPath1(
        String userName, String filePath, P parameters, InputStream inputStream, ContentType contentType)
    {
        Assert.state(!StringUtils.isEmpty(userName), "Expected a non-empty userName!");
        Assert.state(filePath != null, "Expected a non-null path!");
        
        checkParameters(parameters);
        checkBody(inputStream, contentType);
        
        filePath = this.resolvePath(userName, filePath);
        
        final URI uri = this.uriForPath(this.baseUri(), userName, filePath, parameters);
        final RequestBuilder reqBuilder = RequestBuilder.create(this.methodName()).setUri(uri);
        
        if (inputStream != null) {
            reqBuilder.setEntity(new InputStreamEntity(inputStream, contentType));
        }
        
        return reqBuilder.build();
    }

    private HttpUriRequest requestForPath1(
        String userName, String filePath, P parameters, byte[] data, ContentType contentType)
    {
        Assert.state(!StringUtils.isEmpty(userName), "Expected a non-empty userName!");
        Assert.state(filePath != null, "Expected a non-null path!");
        
        checkParameters(parameters);
        checkBody(data, contentType);
        
        filePath = this.resolvePath(userName, filePath);
        
        final URI uri = this.uriForPath(this.baseUri(), userName, filePath, parameters);
        final RequestBuilder reqBuilder = RequestBuilder.create(this.methodName()).setUri(uri);
        
        if (data != null) {
            reqBuilder.setEntity(new ByteArrayEntity(data, contentType));
        }
        
        return reqBuilder.build();
    }
    
    //
    // Public interface
    //
    
    @Override
    public R responseFromHttpEntity(@NotNull HttpEntity e) 
        throws JsonProcessingException, IOException
    {
        Assert.state(e.getContentType() != null, "Expected to find a content-type header");
        Assert.state("application/json".equals(e.getContentType().getValue()), 
            "Expected content encoded as JSON (application/json)");
        return objectMapper.readValue(e.getContent(), this.responseType());
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath)
    {
        return requestForPath1(userName, filePath, (P) null, (byte[]) null, null);
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath, @Valid P parameters)
    {
        return requestForPath1(userName, filePath, parameters, (byte[]) null, null);
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath,
        @NotNull InputStream inputStream, @NotNull ContentType contentType)
    {
        return requestForPath1(userName, filePath, (P) null, inputStream, contentType);
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath,
        @NotNull InputStream inputStream)
    {
        return requestForPath1(userName, filePath, (P) null, inputStream, this.defaultContentType());
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath, @Valid P parameters, 
        @NotNull InputStream inputStream, @NotNull ContentType contentType)
    {
        return requestForPath1(userName, filePath, parameters, inputStream, contentType);
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath, @Valid P parameters, 
        @NotNull InputStream inputStream)
    {
        return requestForPath1(userName, filePath, parameters, inputStream, this.defaultContentType());
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath, 
        @NotNull byte[] data, @NotNull ContentType contentType)
    {
        return requestForPath1(userName, filePath, (P) null, data, contentType);
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath, 
        @NotNull byte[] data)
    {
        return requestForPath1(userName, filePath, (P) null, data, this.defaultContentType());
    }
    
    @Override
    public HttpUriRequest requestForPath(@NotEmpty String userName,
        @NotNull @FilePath String filePath, @Valid P parameters, 
        @NotNull byte[] data, @NotNull ContentType contentType)
    {
        return requestForPath1(userName, filePath, parameters, data, contentType);
    }
    
    @Override
    public HttpUriRequest requestForPath(@NotEmpty String userName,
        @NotNull @FilePath String filePath, @Valid P parameters, 
        @NotNull byte[] data)
    {
        return requestForPath1(userName, filePath, parameters, data, this.defaultContentType());
    }
    
}
