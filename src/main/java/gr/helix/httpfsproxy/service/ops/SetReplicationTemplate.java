package gr.helix.httpfsproxy.service.ops;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import gr.helix.httpfsproxy.model.ops.BooleanResponse;
import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.model.ops.SetReplicationRequestParameters;
import gr.helix.httpfsproxy.service.AbstractPutOperationTemplate;

@Service("setReplicationTemplate")
@Validated
public class SetReplicationTemplate extends AbstractPutOperationTemplate<SetReplicationRequestParameters, BooleanResponse>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.SETREPLICATION;
    }

    @Override
    protected Class<BooleanResponse> responseType()
    {
        return BooleanResponse.class;
    }

    @Override
    protected boolean requireParameters()
    {
        return true;
    }
}
