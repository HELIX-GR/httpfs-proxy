package gr.helix.httpfsproxy.service.ops;

import java.io.IOException;

import javax.validation.constraints.NotNull;

import org.apache.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.JsonProcessingException;

import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.model.ops.ReadFileRequestParameters;
import gr.helix.httpfsproxy.service.AbstractGetOperationTemplate;

@Service("readFileTemplate")
@Validated
public class ReadFileTemplate extends AbstractGetOperationTemplate<ReadFileRequestParameters, byte[]>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.OPEN;
    }

    @Override
    protected Class<byte[]> responseType()
    {
        throw new IllegalStateException("This operation does not return an object!");
    }

    @Override
    protected boolean requireParameters()
    {
        return false;
    }
    
    @Override
    public byte[] responseFromHttpEntity(@NotNull HttpEntity e)
        throws JsonProcessingException, IOException
    {
        throw new UnsupportedOperationException("This operation returns a binary stream");
    }
}
