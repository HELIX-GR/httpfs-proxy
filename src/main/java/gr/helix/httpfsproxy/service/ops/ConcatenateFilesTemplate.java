package gr.helix.httpfsproxy.service.ops;

import java.io.IOException;

import javax.validation.constraints.NotNull;

import org.apache.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.JsonProcessingException;

import gr.helix.httpfsproxy.model.ops.ConcatenateFilesRequestParameters;
import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.service.AbstractPostOperationTemplate;

@Service("concatenateFilesTemplate")
@Validated
public class ConcatenateFilesTemplate extends AbstractPostOperationTemplate<ConcatenateFilesRequestParameters, Void>
{

    @Override
    public EnumOperation operation()
    {
        return EnumOperation.CONCAT;
    }

    @Override
    protected Class<Void> responseType()
    {
        return Void.class;
    }

    @Override
    protected boolean requireParameters()
    {
        return true;
    }
    
    @Override
    public Void responseFromHttpEntity(@NotNull HttpEntity e)
        throws JsonProcessingException, IOException
    {
        return null; // this operation returns no response body
    }
}
