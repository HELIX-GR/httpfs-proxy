package gr.helix.httpfsproxy.service;

import gr.helix.httpfsproxy.model.ops.BaseRequestParameters;

public abstract class AbstractDeleteOperationTemplate <P extends BaseRequestParameters, R> extends AbstractOperationTemplate<P, R>
{
    @Override
    public String methodName() 
    { 
        return "DELETE";
    }
}
