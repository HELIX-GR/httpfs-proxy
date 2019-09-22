package gr.helix.httpfsproxy.service.ops;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import gr.helix.httpfsproxy.model.ops.BooleanResponse;
import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.model.ops.TruncateFileRequestParameters;
import gr.helix.httpfsproxy.service.AbstractPostOperationTemplate;

@Service("truncateFileTemplate")
@Validated
public class TruncateFileTemplate extends AbstractPostOperationTemplate<TruncateFileRequestParameters, BooleanResponse>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.TRUNCATE;
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
}
