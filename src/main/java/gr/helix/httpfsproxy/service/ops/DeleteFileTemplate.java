package gr.helix.httpfsproxy.service.ops;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import gr.helix.httpfsproxy.model.ops.BooleanResponse;
import gr.helix.httpfsproxy.model.ops.DeleteFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.service.AbstractDeleteOperationTemplate;

/**
 * Delete a file/directory
 */
@Service("deleteFileTemplate")
@Validated
public class DeleteFileTemplate extends AbstractDeleteOperationTemplate<DeleteFileRequestParameters, BooleanResponse>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.DELETE;
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
