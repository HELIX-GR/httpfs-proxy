package gr.helix.httpfsproxy.service;

import gr.helix.httpfsproxy.model.backend.BaseRequestParameters;

public abstract class AbstractPostOperationTemplate <P extends BaseRequestParameters, R> extends AbstractOperationTemplate<P, R>
{
    @Override
    public String methodName() 
    { 
        return "POST";
    }
}
