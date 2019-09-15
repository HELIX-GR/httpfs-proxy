package gr.helix.httpfsproxy.service.ops;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import gr.helix.httpfsproxy.model.backend.EnumOperation;
import gr.helix.httpfsproxy.model.backend.VoidRequestParameters;
import gr.helix.httpfsproxy.model.backend.ops.ListStatusResponse;
import gr.helix.httpfsproxy.service.AbstractGetOperationTemplate;

@Service("listStatusTemplate")
@Validated
public class ListStatusTemplate extends AbstractGetOperationTemplate<VoidRequestParameters, ListStatusResponse>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.LISTSTATUS;
    }

    @Override
    protected boolean requireParameters()
    {
        return false;
    }
    
    @Override
    protected Class<ListStatusResponse> getResponseType()
    {
        return ListStatusResponse.class;
    }
}
