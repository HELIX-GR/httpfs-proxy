package gr.helix.httpfsproxy.service.ops;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.model.ops.GetHomeDirectoryResponse;
import gr.helix.httpfsproxy.model.ops.VoidRequestParameters;
import gr.helix.httpfsproxy.service.AbstractGetOperationTemplate;

@Service("getHomeDirectoryTemplate")
@Validated
public class GetHomeDirectoryTemplate extends AbstractGetOperationTemplate<VoidRequestParameters, GetHomeDirectoryResponse>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.GETHOMEDIRECTORY;
    }

    @Override
    protected boolean requireParameters()
    {
        return false;
    }
    
    @Override
    protected Class<GetHomeDirectoryResponse> responseType()
    {
        return GetHomeDirectoryResponse.class;
    }
}
