package gr.helix.httpfsproxy.service;

import org.apache.http.entity.ContentType;

import gr.helix.httpfsproxy.model.ops.BaseRequestParameters;

public abstract class AbstractPostOperationTemplate <P extends BaseRequestParameters, R> extends AbstractOperationTemplate<P, R>
{
    @Override
    public String methodName() 
    { 
        return "POST";
    }
    
    @Override
    protected ContentType defaultContentType()
    {
        return ContentType.APPLICATION_OCTET_STREAM;
    }
}
