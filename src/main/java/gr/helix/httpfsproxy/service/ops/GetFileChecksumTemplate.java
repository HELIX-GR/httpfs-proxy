package gr.helix.httpfsproxy.service.ops;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.model.ops.GetFileChecksumResponse;
import gr.helix.httpfsproxy.model.ops.VoidRequestParameters;
import gr.helix.httpfsproxy.service.AbstractGetOperationTemplate;

@Service("getFileChecksumTemplate")
@Validated
public class GetFileChecksumTemplate extends AbstractGetOperationTemplate<VoidRequestParameters, GetFileChecksumResponse>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.GETFILECHECKSUM;
    }

    @Override
    protected Class<GetFileChecksumResponse> responseType()
    {
        return GetFileChecksumResponse.class;
    }

    @Override
    protected boolean requireParameters()
    {
        return false;
    }

}
