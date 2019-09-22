package gr.helix.httpfsproxy.service.ops;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import gr.helix.httpfsproxy.model.ops.ContentSummaryResponse;
import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.model.ops.VoidRequestParameters;
import gr.helix.httpfsproxy.service.AbstractGetOperationTemplate;

@Service("getContentSummaryTemplate")
@Validated
public class GetContentSummaryTemplate extends AbstractGetOperationTemplate<VoidRequestParameters, ContentSummaryResponse>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.GETCONTENTSUMMARY;
    }

    @Override
    protected Class<ContentSummaryResponse> responseType()
    {
        return ContentSummaryResponse.class;
    }

    @Override
    protected boolean requireParameters()
    {
        return false;
    }
}
