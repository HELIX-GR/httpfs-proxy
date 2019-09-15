package gr.helix.httpfsproxy.service;

import java.io.InputStream;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;

import gr.helix.httpfsproxy.model.backend.BaseRequestParameters;

public abstract class AbstractGetOperationTemplate <P extends BaseRequestParameters, R> extends AbstractOperationTemplate<P, R>
{
    @Override
    public String methodName() 
    { 
        return "GET";
    }
    
    @Override
    public HttpUriRequest requestForPath(@NotEmpty String userName, @NotNull String path,
        InputStream in, ContentType contentType)
    {
        throw new UnsupportedOperationException("GET requests do not support a request payload");
    }
    
    @Override
    public HttpUriRequest requestForPath(@NotEmpty String userName, @NotNull String path,
        @Valid @NotNull P parameters, InputStream in, ContentType contentType)
    {
        throw new UnsupportedOperationException("GET requests do not support a request payload");
    }
}
