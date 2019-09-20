package gr.helix.httpfsproxy.service.ops;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import gr.helix.httpfsproxy.model.ops.BooleanResponse;
import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.model.ops.MakeDirectoryRequestParameters;
import gr.helix.httpfsproxy.service.AbstractPutOperationTemplate;

@Service("makeDirectoryTemplate")
@Validated
public class MakeDirectoryTemplate extends AbstractPutOperationTemplate<MakeDirectoryRequestParameters, BooleanResponse>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.MKDIRS;
    }

    @Override
    protected Class<BooleanResponse> responseType()
    {
        return BooleanResponse.class;
    }

    @Override
    protected boolean requireParameters()
    {
        return false;
    }
    
    @Override
    protected boolean requireBody()
    {
        return false;
    }
    
    @Override
    protected MakeDirectoryRequestParameters defaultParameters()
    {
        return new MakeDirectoryRequestParameters();
    }
}
