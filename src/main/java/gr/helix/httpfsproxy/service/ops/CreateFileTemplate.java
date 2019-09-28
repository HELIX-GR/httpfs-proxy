package gr.helix.httpfsproxy.service.ops;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.JsonProcessingException;

import gr.helix.httpfsproxy.model.ops.CreateFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.service.AbstractPutOperationTemplate;
import gr.helix.httpfsproxy.validation.FilePath;

@Service("createFileTemplate")
@Validated
public class CreateFileTemplate extends AbstractPutOperationTemplate<CreateFileRequestParameters, Void>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.CREATE;
    }

    @Override
    protected Class<Void> responseType()
    {
        return Void.class;
    }

    @Override
    protected boolean requireParameters()
    {
        return false;
    }
    
    @Override
    protected int expectedHttpStatusCode()
    {
        return HttpStatus.SC_CREATED;
    }
    
    @Override
    public Void responseFromEntity(@NotNull HttpEntity e)
        throws JsonProcessingException, IOException
    {
        return null; // this operation returns no response body
    }
    
    @Override
    protected boolean requireBody()
    {
        return true;
    }
    
    @Override
    protected Collection<ContentType> allowedContentTypes()
    {
        return Collections.singletonList(ContentType.APPLICATION_OCTET_STREAM);
    }
    
    @Override
    protected CreateFileRequestParameters defaultParameters()
    {
        return new CreateFileRequestParameters();
    }
}
