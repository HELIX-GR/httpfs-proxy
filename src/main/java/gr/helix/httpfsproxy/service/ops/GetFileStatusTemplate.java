package gr.helix.httpfsproxy.service.ops;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.model.ops.GetFileStatusResponse;
import gr.helix.httpfsproxy.model.ops.VoidRequestParameters;
import gr.helix.httpfsproxy.service.AbstractGetOperationTemplate;

@Service("getFileStatusTemplate")
@Validated
public class GetFileStatusTemplate extends AbstractGetOperationTemplate<VoidRequestParameters, GetFileStatusResponse>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.GETFILESTATUS;
    }

    @Override
    protected Class<GetFileStatusResponse> responseType()
    {
        return GetFileStatusResponse.class;
    }

    @Override
    protected boolean requireParameters()
    {
        return false;
    }
}
