package gr.helix.httpfsproxy.service.ops;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import gr.helix.httpfsproxy.model.backend.EnumOperation;
import gr.helix.httpfsproxy.model.backend.ops.BooleanResponse;
import gr.helix.httpfsproxy.model.backend.ops.RenameRequestParameters;
import gr.helix.httpfsproxy.service.AbstractPutOperationTemplate;

@Service("renameTemplate")
@Validated
public class RenameTemplate extends AbstractPutOperationTemplate<RenameRequestParameters, BooleanResponse>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.RENAME;
    }
    
    @Override
    protected boolean requireParameters()
    {
        return true;
    }
    
    @Override
    protected Class<BooleanResponse> getResponseType()
    {
        return BooleanResponse.class;
    }
}
