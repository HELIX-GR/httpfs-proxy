package gr.helix.httpfsproxy.service.ops;

import java.io.IOException;

import javax.validation.constraints.NotNull;

import org.apache.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.JsonProcessingException;

import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.model.ops.SetOwnerRequestParameters;
import gr.helix.httpfsproxy.service.AbstractPutOperationTemplate;

@Service("setOwnerTemplate")
@Validated
public class SetOwnerTemplate extends AbstractPutOperationTemplate<SetOwnerRequestParameters, Void>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.SETOWNER;
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
    public Void responseFromEntity(@NotNull HttpEntity e)
        throws JsonProcessingException, IOException
    {
        return null; // this operation returns no response body
    }
}
