package gr.helix.httpfsproxy.service;

import gr.helix.httpfsproxy.model.ops.BaseRequestParameters;

public abstract class AbstractPostOperationTemplate <P extends BaseRequestParameters, R> extends AbstractOperationTemplate<P, R>
{
    @Override
    public String methodName() 
    { 
        return "POST";
    }
}
