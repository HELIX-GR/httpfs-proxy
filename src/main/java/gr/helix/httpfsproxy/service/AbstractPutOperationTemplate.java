package gr.helix.httpfsproxy.service;

import java.util.Collection;

import org.apache.http.entity.ContentType;

import gr.helix.httpfsproxy.model.ops.BaseRequestParameters;

public abstract class AbstractPutOperationTemplate <P extends BaseRequestParameters, R> extends AbstractOperationTemplate<P, R>
{
    @Override
    public String methodName() 
    { 
        return "PUT";
    }
    
    @Override
    protected ContentType defaultContentType()
    {
        return ContentType.APPLICATION_OCTET_STREAM;
    }
}
